package agzam4.database;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

import agzam4.database.DBFields.FIELD;
import agzam4.database.DBFields.PRIMARY_KEY;
import agzam4.database.SQL.TableColumnInfo;
import arc.func.Func;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;

public class Table<T> {
	
	private String name;

	private final ObjectMap<String, MethodHandle> getters = new ObjectMap<>();
	private final ObjectMap<String, MethodHandle> setters = new ObjectMap<>();
	
	private String keyName;
	private final Seq<String> filedNames = new Seq<String>();

	private final Seq<String> columns = new Seq<String>();
	private final Seq<String> columnsNoKey;

	private final ObjectMap<String, TableColumnInfo> columnsInfos = new ObjectMap<>();
	
	private final Class<T> entityType;
	
	public Table(String name, Class<T> type) {
		entityType = type;
		this.name = name;
		Seq<TableColumnInfo> currentInfo = SQL.createTableInfo(type);
		Seq<TableColumnInfo> previousInfo = Database.queryTableInfo(name);
		
		if(previousInfo != null) {
			// Checking format matching of old table and new
			boolean same = currentInfo.size == previousInfo.size;
			if(same) {
				for (int i = 0; i < currentInfo.size; i++) {
					if(currentInfo.get(i).eql(previousInfo.get(i))) {
						continue;
					}
					same = false;
					Log.info("table fields not matching: [@]/[@]", currentInfo.get(i), previousInfo.get(i));
					break;
				}
			}

			if(!same) {
				/* 
				 * TODO: use new functions to change table
				 * - ALTER TABLE <TABLENAME> DROP <COLUMNNAME>;
				 * - ALTER TABLE <TABLENAME> ADD COLUMN <COLUM>;
				 */
				Log.info("[SQL] Changing the table: @ -> @", previousInfo, currentInfo);

				Seq<TableColumnInfo> toCopy = new Seq<>();
				for (var info : currentInfo) {
					TableColumnInfo c = previousInfo.find(i -> i.name.equals(info.name));
					if(c == null) continue;
					toCopy.add(c);
				}
				String copyList = toCopy.toString(", ", info -> info.name);

				// if not same rename table and copy fields
				Database.executeTransaction(
						"DROP TABLE IF EXISTS tmp;",
						Strings.format("CREATE TABLE tmp (@);", currentInfo.toString(",", info -> info.toString())),
						Strings.format("INSERT INTO tmp(@) SELECT @ FROM @;", copyList, copyList, name),
						Strings.format("DROP TABLE @;", name),
						Strings.format("ALTER TABLE tmp RENAME TO @;", name)
				);
			}
		}
		
		
		Database.execute(Strings.format("CREATE TABLE IF NOT EXISTS @ (@)", name, currentInfo.toString(",", info -> info.toString())));
		Log.info("[SQL] Table \"@\" inited", name);
		
	    try {
	        Lookup lookup = MethodHandles.lookup();
	        for (Field field : type.getDeclaredFields()) {
	            if (field.isAnnotationPresent(FIELD.class)) {
	                getters.put(field.getName(), lookup.unreflectGetter(field));
	                setters.put(field.getName(), lookup.unreflectSetter(field));
	                if (field.isAnnotationPresent(PRIMARY_KEY.class)) {
	                	keyName = field.getName();
	                } else {
	                	filedNames.add(field.getName());
	                }
	                columns.add(field.getName());
//	                boolean isArray = field.getType().isArray() || (field.getType() == Seq.class && field.getGenericType() instanceof ParameterizedType);
//	                isArrayColumn.add(isArray);
	            }
	        }
	    } catch (IllegalAccessException e) {
	        throw new RuntimeException("Failed to analyze fields", e);
	    }
	    columnsNoKey = columns.select(e -> !e.equals(keyName));
	}
	
	
	public void update(T entity) {
		String sql = Strings.format("UPDATE @ SET @ WHERE @ = ?", name, filedNames.toString(" ", n -> n + " = ?"), keyName);
		Log.info("update: @", sql);
		Object[] args = new Object[filedNames.size+1];
		for (int i = 0; i < filedNames.size; i++) args[i] = column(filedNames.get(i), entity);
		args[args.length-1] = key(entity);
		Database.update(sql, args);
	}

	public void put(T entity) {
		String sql = Strings.format("INSERT OR REPLACE INTO @ (@) VALUES (@)", name, columns.toString(", "), columns.toString(", ", n -> "?"));
		Object[] args = new Object[columns.size];
		for (int i = 0; i < args.length; i++) args[i] = column(columns.get(i), entity);
		Database.update(sql, args);
	}

	public void putNoKey(T entity) {
		String sql = Strings.format("INSERT INTO @ (@) VALUES (@)", name, columnsNoKey.toString(", "), columnsNoKey.toString(", ", n -> "?"));
		Object[] args = new Object[columnsNoKey.size];
		for (int i = 0; i < args.length; i++) args[i] = column(columnsNoKey.get(i), entity);
		Database.update(sql, args);
	}

	public @Nullable T get(Object key) {
		String sql = Strings.format("SELECT * FROM @ WHERE @ = ?", name, keyName);
		return Database.query(sql, r -> {
			try {
				if(!r.next()) return null;
				T entity = entityType.getConstructor().newInstance();
				for (int i = 0; i < columns.size; i++) {
					var obj = r.getObject(columns.get(i));
					setters.get(columns.get(i)).invoke(entity, obj);
				}
				return entity;
			} catch (Throwable e) {
				Log.err(e);
			}
			return null;
		}, key);
	}

	public @Nullable Seq<T> query(Object... args) {
		if(args.length%2 != 0) throw new RuntimeException("Query arguments must be n*2 \"(column1, value1, column2, value2, ...)\"");
		StringBuilder sql = new StringBuilder("SELECT * FROM ");
		sql.append(name).append(" WHERE");
		Object[] sqlArgs = new Object[args.length/2];
		for (int i = 0; i < sqlArgs.length; i++) {
			sqlArgs[i] = args[i*2+1];
			sql.append(' ');
			sql.append(args[i*2]);
			sql.append(" = ?");
		}
		return Database.query(sql.toString(), r -> {
			try {
				Seq<T> ts = new Seq<>();
				while (r.next()) {
					T entity = entityType.getConstructor().newInstance();
					for (int i = 0; i < columns.size; i++) {
						var obj = r.getObject(columns.get(i));
						setters.get(columns.get(i)).invoke(entity, obj);
					}
					ts.add(entity);
				}
				return ts;
			} catch (Throwable e) {
				Log.err(e);
			}
			return null;
		}, sqlArgs);
	}

	public @Nullable T get(Object key, EntityInit<T> def) {
		T e = get(key);
		if(e == null) {
			e = def.get();
			put(e);
		}
		return e;
	}
	
	public interface EntityInit<T> {
		public T get();
	}
	
	public void debug(T entity) {
		try {
			Log.info("key=@", key(entity));
		} catch (Throwable e) {
			Log.err(e);
		}
	}

	public @Nullable Object key(T entity) {
		try {
			return getters.get(keyName).invoke(entity);
		} catch (Throwable e) {
			Log.err(e);
		}
		return null;
	}

	public @Nullable Object column(String name, T entity) {
		try {
			return getters.get(name).invoke(entity);
		} catch (Throwable e) {
			Log.err(e);
		}
		return null;
	}

}
