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
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Strings;
import arc.util.Threads;
import arc.util.serialization.Base64Coder;
import arc.util.serialization.Jval;
import arc.util.serialization.Jval.Jformat;
import mindustry.Vars;

public class Logs {

	private static final int MAX_LOGS = 10; 
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
			for (int i = 0; i < instances.size; i++) {
				instances.get(i).globalIdShift = shift;
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

					if (current.totalRows >= MAX_LOGS) {
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

	private static final RuntimeException amoutOfRequestedLimit = new RuntimeException("Amount of requested logs can not be >" + MAX_LOGS);

	/**
	 * @param tags - list of selected tags, empty for all
	 * @param gid - first id (largest)
	 * @param limit - logs read before id
	 * @param t1 - minimum time (inclusive)
	 * @param t2 - maximum time (inclusive)
	 * @return unsorted limit-length interval with nullable items [id, id+limit) from database that filter by (tags + timerange [t1,t2])<br>example: [e, e, e, e, null]
	 */
	public static LogEntity[] logsBy(long gid, int limit, long t1, long t2, int[] tags) {
		if(limit > MAX_LOGS) throw amoutOfRequestedLimit; // No more 2 databases per call

		LogEntity[] result = new LogEntity[limit];

		if(gid < 0) return result;

		final LogInstance first, second;
		synchronized (lock) {
			int firstIndex = -1;
			firstIndex = Seqs.binarySearch(instances, i -> {
				if(gid < i.globalIdShift) return -1;
				if(gid < i.globalIdLimit()) return 0;
				return 1;
			});
			if(firstIndex < 0) return result;

			first = instances.get(firstIndex);
			second = firstIndex - 1 >= 0 ? instances.get(firstIndex - 1) : null;
		}

		if(first == null) return result; // No logs in id range found
		if(!first.hasAnyTimestamp(t1, t2)) return result;


		// First database search
		int[] size = new int[1];

		// Select between A (inclusive) and B (inclusive) + filters
		final String sql = "WHERE id BETWEEN ? AND ? AND timestamp >= ? AND timestamp <= ?";
		// TODO: tags filter to SQL

		// Case 1
		// [second][first]
		// ---------FFFF-
		//             ^
		//            GID

		// Case 2
		// [second][first] 
		// ------SSFF----
		//          ^
		//         GID

		// Taking "F" part
		final int firstB = (int) (gid - first.globalIdShift);
		final int firstA = Math.max(0, firstB - limit + 1);

		try {
			if(!first.isOpen()) first.open();
			first.logs.select(sql, e -> {
				e.globalId = first.globalIdShift + e.id;
				result[size[0]++] = e;
			}, firstA, firstB, t1, t2);
		} catch (Exception e) {
			Log.err("Error reading page backwards from instance " + first.id, e);
		} finally {
			first.close();
		}

		int itemsCovered = firstB - firstA + 1;
		if(itemsCovered >= limit || second == null) return result; // limit completed or not more logs
		if(!second.hasAnyTimestamp(t1, t2)) return result;

		// Taking "S" part
		int secondB = (int) (second.globalIdLimit() - 1 - second.globalIdShift);
		int remainingIds = limit - itemsCovered;
		int secondA = Math.max(0, secondB - remainingIds + 1);

		try {
			if(!second.isOpen()) second.open();
			second.logs.select(sql, e -> {
				e.globalId = second.globalIdShift + e.id;
				result[size[0]++] = e;
			}, secondA, secondB, t1, t2);
		} catch (Exception e) {
			Log.err("Error reading page backwards from instance " + second.id, e);
		} finally {
			second.close();
		}

		return result;
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
		public int totalRows = 0;
		public long minTimestamp = 0;
		public long maxTimestamp = 0;
		public long globalIdShift = 0;

		public LogInstance(int fileIndex) {
			this.id = fileIndex;
		}

		public boolean hasTimestamp(long t) {
			return minTimestamp <= t && t <= maxTimestamp;
		}

		public boolean hasAnyTimestamp(long t1, long t2) {
			return hasTimestamp(t1) || hasTimestamp(t2);
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
