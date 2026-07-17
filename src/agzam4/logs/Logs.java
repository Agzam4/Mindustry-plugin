package agzam4.logs;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import agzam4.api.endpoints.ApiLogs;
import agzam4.database.Database;
import agzam4.database.Table;
import agzam4.logs.LogEvents.*;
import agzam4.utils.Log;
import agzam4.utils.Seqs;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Threads;
import arc.util.serialization.Base64Coder;
import arc.util.serialization.Jval;
import arc.util.serialization.Jval.Jformat;
import mindustry.Vars;

public class Logs {
	
	/**
	 * MTU = 1500 bytes (TCP MSS payload = 1448 bytes)
	 * HTTP Headers ~500 bytes
	 * Free space in 1st packet: 1448 - 500 = 948 bytes
	 * Free space in 2 packets: 1448 + 948 = 2396 bytes
	 * 
	 * Raw JSON entity ~126 bytes
	 * GZIP compressed (~5.5x) ~23 bytes
	 * 1st packet capacity: 948 / 23 ≈ 41 logs
	 * 2nd packet capacity: 2396 / 23 ≈ 104 logs
	 * 
	 * 100 fits into 2 MTU packets
	 * 
	 * TODO: test on real data
	 */
	private static final int maxPageSize = 100;
	private static final RuntimeException amoutOfRequestedLimit = new RuntimeException("Amount of requested logs can not be >" + maxPageSize);
	
	/**
	 * Average daily logs: ~11_219 logs/day
	 * 
	 * Target database log file size: <= 10 MB
	 * 
	 * Database row size: ~162 bytes
	 * 10 MB / 162 bytes = ~61_728 bytes = ~50_000
	 * 50_000 / 11_219 ≈ 4.45 days
	 */
	private static final int maxRows = 50_000; 
	
	private static final String prefix = "log-";
	private static Fi root = Vars.dataDirectory.child("logs/");

	private static LogInstance current = null;

	private static final ConcurrentLinkedQueue<LogEvent> queue = new ConcurrentLinkedQueue<>();
	private static Seq<LogInstance> instances = new Seq<>(); // CopyOnWriteArrayList?

	private static ObjectMap<Class<? extends LogEvent>, LogBuilder<? extends LogEvent>> builderByClass = ObjectMap.of();
	private static LogBuilder<? extends LogEvent>[] builders;

	private static final Object lock = new Object();

	@SuppressWarnings("unchecked")
	public static void init() throws ClassNotFoundException {
		builders = new LogBuilder[LogEvents.events.length];
		for (int i = 0; i < LogEvents.events.length; i++) {
			var event = LogEvents.events[i];
			LogBuilder<? extends LogEvent> builder = new LogBuilder<>(event, i);
			builderByClass.put(event, builder);
			builders[i] = builder;
		}

		root.mkdirs();

		synchronized(lock) {
			instances = Seq.with(root.list())
					.select(f -> f.name().startsWith(prefix))
					.map(f -> new LogInstance(Strings.parseInt(f.nameWithoutExtension().substring(prefix.length()), 10, 0)));

			instances.forEach(LogInstance::initMeta);
			instances.sort((i1,i2) -> Long.compare(i1.minTimestamp, i2.minTimestamp));

			long shift = 0;
			LogInstance prev = null;
			for (int i = 0; i < instances.size; i++) {
				instances.get(i).globalIdShift = shift;
				instances.get(i).prev = prev;
				prev = instances.get(i);
				shift += instances.get(i).totalRows;
			}

			current = instances.peek();
			if (current == null) {
				current = new LogInstance(0);
				instances.add(current);
			}
		}

		try {
			current.open();     
			// TODO: PRAGMA setting
			// PRAGMA synchronous = FULL;
			// PRAGMA journal_mode = WAL;
			// PRAGMA cache_size = -10000;
			// PRAGMA mmap_size = 0;
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}

		Threads.daemon("logs-writer-thread", () -> {
			while (true) {
				try {
					LogEvent event = queue.poll();
					if (event == null) {
						Thread.sleep(100);
						continue;
					}
					LogEntity entity = buildEntity(event);

					if (current.totalRows >= maxRows) {
						synchronized(lock) {
							current.close();

							LogInstance nextInstance = new LogInstance(current.id + 1);
							nextInstance.globalIdShift = current.globalIdLimit();
							nextInstance.open();

							Log.info("Logs rotated to @.db", nextInstance.id);
							instances.add(nextInstance);
							current = nextInstance;
						}
					}
					current.logs.put(entity);

					if (current.totalRows == 0) {
						current.minTimestamp = entity.timestamp;
					}
					current.maxTimestamp = entity.timestamp;
					current.totalRows++;

					ApiLogs.logsStream.broadcast(entity);
				} catch (Exception e) {
					Log.err(e);
				}
			}
		});
		event(new ServerStartLogEvent());
	}

	@SuppressWarnings("unchecked")
	private static <T extends LogEvent> LogEntity buildEntity(T event) {
		Class<T> eventClass = (Class<T>) event.getClass();
		LogBuilder<T> builder = (LogBuilder<T>) builderByClass.get(eventClass);
		if (builder == null) {
			throw new ArcRuntimeException("No builder registrated for class " + eventClass);
		}
		LogEntity entity = new LogEntity();
		entity.timestamp = event.timestamp;
		entity.tag = builder.id;
		entity.message = builder.build(event); 
		return entity;
	}

	/** @return last global entity id */
	public static long lastId() {
		synchronized (lock) {
			if (current == null) return 0;
			return current.globalIdLimit();
		}
	}

	@Deprecated
	public static String entityJson(LogEntity entity, boolean protect) {
		if(entity.tag >= builders.length) throw new RuntimeException(Strings.format("Unknow event type id: @ (maximum @) for @-@  @ ", entity.tag, builders.length, entity.globalId, entity.id, entity.message));

		var val = Jval.read(entity.message);
		val.put("timestamp", entity.timestamp);
		if(entity.globalId >= 0) val.put("logid", entity.globalId);
		if(entity.id != null) val.put("id", entity.id);

		return val.toString(Jformat.plain);
	}

	/**
	 * @param tags - list of selected tags, empty for all
	 * @param gid - first id (largest)
	 * @param limit - logs read before id
	 * @param t1 - minimum time (inclusive)
	 * @param t2 - maximum time (inclusive)
	 * @return unsorted limit-length interval with nullable items [id, id+limit) from database that filter by (tags + timerange [t1,t2])<br>example: [e, e, e, e, null]
	 */
	public static LogEntity[] filtredPage(long gid, int limit, long t1, long t2, int[] tags) {
		if(limit > maxPageSize) throw amoutOfRequestedLimit;

		LogEntity[] result = new LogEntity[limit];

		if(gid < 0) return result;

		// global interval [a,b], where a - oldest log (a.id < b.id)
		long a = gid - limit + 1; // inclusive
		long b = gid; // inclusive
		
		LogInstance log;
		synchronized (lock) {
			int firstIndex = -1;
			firstIndex = Seqs.binarySearch(instances, i -> {
				if(gid < i.globalIdShift) return -1;
				if(gid < i.globalIdLimit()) return 0;
				return 1;
			});
			if(firstIndex < 0) {
				if(a <= Logs.current.globalIdLimit()-1) {
					log = Logs.current;
					limit -= b - log.globalIdLimit() + 1; // remove non-existent
				} else {
					return result; // No logs found
				}
			} else {
				log = instances.get(firstIndex);
			}
		}
		
		if(!log.collideTimestamp(t1, t2)) return result;


		// First database search
		int[] size = new int[1];
		Object[] args = new Object[2 + (t1 == 0 ? 0 : 1) + (t2 == 0 ? 0 : 1) + tags.length];
		int argIndex = 2; // skip BETWEEN ? AND ?

		// Select between A (inclusive) and B (inclusive) + filters
		StringBuilder sqlBuilder = new StringBuilder("WHERE id BETWEEN ? AND ?");
		if(t1 != 0) {
			sqlBuilder.append(" AND timestamp >= ?");
			args[argIndex++] = t1;
		}
		if(t2 != 0) {
			sqlBuilder.append(" AND timestamp <= ?");
			args[argIndex++] = t2;
		}
		if(tags.length != 0) {
			sqlBuilder.append(" AND tag IN (");
			for (int i = 0; i < tags.length; i++) {
				if(i != 0) sqlBuilder.append(",");
				sqlBuilder.append("?");
				args[argIndex++] = tags[i];
			}
			sqlBuilder.append(")");
		}
		
		final String sql = sqlBuilder.toString();

		while (true) {
			final int currentA = Mathf.clamp((int) (a - log.globalIdShift), 0, log.totalRows - 1);
			final int currentB = Mathf.clamp((int) (b - log.globalIdShift), 0, log.totalRows - 1);
//			Log.info("Get [cyan][@,@][gray] [@,@][][] from [blue]@ log[gray] -@", currentA+1, currentB+1, a, b, log.id + (log == Logs.current ? " (latest)" : ""), log.globalIdShift);

			synchronized (lock) {
				try {
					if(!log.isOpen()) log.open();
					final long shift = log.globalIdShift - 1; // in database first is 1
					args[0] = currentA + 1;
					args[1] = currentB + 1;
					log.logs.select(sql, e -> {
						e.globalId = shift + e.id;
						result[size[0]++] = e;
					}, args);
				} catch (Exception e) {
					Log.err("Error reading page backwards from instance " + log.id, e);
				} finally {
					if(log != current) log.close();
				}
			}
			int cover =  currentB - currentA + 1;
			limit -= cover;
			b -= cover; // mover right border to left
			
			if(limit <= 0) return result; // all covered
			log = log.prev;
			if(log == null) return result;
		}
	}


	public static long packUuid(String uuid) {
		byte[] bytes = Base64Coder.decode(uuid);
		long uuidBuiled = 0;
		for (int i = 0; i < 8; i++) uuidBuiled = (uuidBuiled << 8) | (bytes[i] & 0xFF);
		return uuidBuiled;
	}


	private static class LogInstance {

		// Database
		public final int id;
		public Database database;
		public Table<LogEntity> logs;

		// Meta
		public @Nullable LogInstance prev;
		public int totalRows = 0;
		public long minTimestamp = 0;
		public long maxTimestamp = 0;
		public long globalIdShift = 0;

		public LogInstance(int fileIndex) {
			this.id = fileIndex;
		}

		public boolean collideTimestamp(long t1, long t2) {
			if(t2 < minTimestamp) return false;
			if(t1 > maxTimestamp) return false;
			return true;
		}

		public boolean isOpen() {
			return database != null;
		}

		public synchronized void open() throws ClassNotFoundException, SQLException {
			if (isOpen()) return; 
			Fi file = root.child(prefix + id + ".db");
			this.database = new Database(file);
			this.logs = database.createTable("logs", LogEntity.class);
		}

		public synchronized void close() {
			if (!isOpen()) return; 
			try {
				database.close();
			} catch (Exception e) {
				Log.err(e);
			}
			this.database = null;
			this.logs = null;
		}

		public void initMeta() { 
			try {
				this.open();
				this.totalRows = this.logs.count();
				if (this.totalRows > 0) {
					LogEntity first = this.logs.first(); 
					LogEntity last = this.logs.last();
					this.minTimestamp = first.timestamp;
					this.maxTimestamp = last.timestamp;
				}
			} catch (Exception e) {
				this.totalRows = 0;
			} finally {
				this.close();
			}
		}

		private long globalIdLimit() {
			return this.globalIdShift + totalRows;
		}

		@Override
		public String toString() {
			return Strings.format("Logs[@,@]", globalIdShift, globalIdLimit());
		}
	}


	public static void event(LogEvent event) {
		queue.add(event);
	}

}
