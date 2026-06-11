package agzam4.logs;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import agzam4.database.Database;
import agzam4.database.Table;
import agzam4.logs.LogEvents.*;
import agzam4.utils.Log;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Strings;
import arc.util.Threads;
import arc.util.serialization.Base64Coder;
import mindustry.Vars;

public class Logs {
	
	/**
	 * Next: new log schemes
	 */

    private static final int MAX_LOGS = 10; 
	private static final String prefix = "log-";
	private static Fi root = Vars.dataDirectory.child("logs/");
	
	private static LogInstance current = null;
	
	private static final ConcurrentLinkedQueue<LogEvent> queue = new ConcurrentLinkedQueue<>();
	private static Seq<LogInstance> instances = new Seq<>(); // CopyOnWriteArrayList?
	
	private static ObjectMap<Class<? extends LogEvent>, LogBuilder<? extends LogEvent>> builderByClass = ObjectMap.of();
	private static LogBuilder<? extends LogEvent>[] builders;
	
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
		
		instances = Seq.with(root.list())
				.select(f -> f.name().startsWith(prefix))
				.map(f -> new LogInstance(Strings.parseInt(f.nameWithoutExtension().substring(prefix.length()), 10, 0)));
		
		instances.forEach(LogInstance::initMeta);
		instances.sort((i1,i2) -> Long.compare(i1.minTimestamp, i2.minTimestamp));
		
		current = instances.peek();
		if(current == null) {
			current = new LogInstance(0);
			instances.add(current);
		}
		
		try {
            current.open();
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
                        current.close();
                        LogInstance nextInstance = new LogInstance(current.id + 1);
                        nextInstance.open();
                        synchronized (instances) {
                        	instances.add(nextInstance);
                        }
                        current = nextInstance;
						Log.info("Logs rotated to @@.db", prefix, current.id);
					}
					current.logs.put(entity);

                    if (current.totalRows == 0) {
                        current.minTimestamp = entity.timestamp;
                    }
                    current.maxTimestamp = entity.timestamp;
                    current.totalRows++;
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
//	        builder = new LogBuilder<>(eventClass);
//	        builders.put(eventClass, builder);
	    }
	    LogEntity entity = new LogEntity();
		entity.timestamp = event.timestamp;
		entity.tag = builder.id;
		entity.message = builder.build(event); 
	    return entity;
	}

	
    /**
     * Searching logs in [t1,t2]
     * @param t1         millis (include)
     * @param t2         millis (include)
     * @param page       page number (from 0)
     * @param pageSize   page size
     */
    public static Seq<LogEntity> selectByTimerange(long t1, long t2, int page, int pageSize) {
        Seq<LogEntity> result = new Seq<>();
        int skipRows = page * pageSize;

        synchronized (instances) {
            for (int i = instances.size - 1; i >= 0; i--) {
                if(result.size >= pageSize) break;
                
                LogInstance instance = instances.get(i);

                if (instance.totalRows == 0) continue;
                if (instance.maxTimestamp < t1 || instance.minTimestamp > t2) continue; 

                int matchingRowsInFile = 0;
                boolean wasClosed = !instance.isOpen();
                
                try {
                    if (wasClosed) instance.open();

                    matchingRowsInFile = instance.logs.count("timestamp >= ? AND timestamp <= ?", t1, t2);

                    if (skipRows >= matchingRowsInFile) {
                        skipRows -= matchingRowsInFile;
                        continue;
                    }

                    int fileOffset = skipRows;
                    int fileLimit = pageSize - result.size;
                    skipRows = 0;

                    instance.logs.select("WHERE timestamp >= ? AND timestamp <= ? ORDER BY id DESC LIMIT ? OFFSET ?", e -> result.add(e), t1, t2, fileLimit, fileOffset);
                } catch (Exception e) {
                    Log.err("REST failed to query logs by time in file: " + instance.id, e);
                } finally {
                    if (wasClosed) instance.close();
                }
            }
        }
        return result;
    }
    
    public static String protectedJson(LogEntity entity) {
    	return builders[entity.id].protect(entity.message);
	}

//	public static void notify(NotifyTag tag, String message) {
//		notify(tag, null, message, false);
//	}
//
//	public static void notify(NotifyTag tag, @Nullable String uuid, String message) {
//		notify(tag, uuid, message, false);
//	}
//	
//	public static void notify(NotifyTag tag, @Nullable Player player, String message) {
//		notify(tag, player == null ? null : player.uuid(), message, false);
//	}
//
//	public static void notify(NotifyTag tag, @Nullable Player player, String sensitive, String message) {
//		notify(tag, player == null ? null : player.uuid(), message, false);
//	}
//	
//	public static void notify(NotifyTag tag, @Nullable String uuid, String message, boolean sensitive) {
//		LogEntity entity = new LogEntity();
//		entity.message = message;
//		entity.sensitive = sensitive;
//		entity.tag = tag.ordinal();
//		if(uuid != null) entity.uuid = packUuid(uuid);
//        entity.timestamp = Time.millis();
////		current.logs.put(entity);
//        queue.add(entity);
//	}
	
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

        public LogInstance(int fileIndex) {
            this.id = fileIndex;
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
        
	}


	public static void event(LogEvent event) {
		queue.add(event);
	}
	
}
