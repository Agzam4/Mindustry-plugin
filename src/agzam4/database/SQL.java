package agzam4.database;

import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;

import agzam4.database.DBFields.DEFAULT;
import agzam4.database.DBFields.FIELD;
import agzam4.database.DBFields.PRIMARY_KEY;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;

public class SQL {

	public static final ObjectMap<Class<?>, String> typesByClass = ObjectMap.of(String.class, "TEXT", Integer.class, "INTEGER");
	
	public static class TableColumnInfo {

		public String name;
		public String type;
		public boolean isNullable; 
		public @Nullable String def;
		public boolean isPrimaryKey;

		public TableColumnInfo(String name, String type, boolean isNullable, String def, boolean isPrimaryKey) {
			this.name = name;
			this.type = type;
			this.isNullable = isNullable;
			this.def = def;
			this.isPrimaryKey = isPrimaryKey;
		}
		
		public TableColumnInfo(ResultSet result) throws SQLException {
			name = result.getString("name");
			type = result.getString("type");
			isNullable = result.getInt("notnull") == 0;
			def = result.getString("dflt_value");
			isPrimaryKey = result.getInt("pk") == 1;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(name).append(' ').append(type);
			if(isPrimaryKey) sb.append(" PRIMARY KEY");
			if(def != null) sb.append(" DEFAULT ").append(def);
			return sb.toString();
		}
		
		public boolean eql(TableColumnInfo i) {
			if(isNullable != i.isNullable) return false;
			if(isPrimaryKey != i.isPrimaryKey) return false;
			if((def == null) != (i.def == null)) return false;
			if(!name.equals(i.name)) return false;
			if(!type.equals(i.type)) return false;
			if(def != null && i.def != null) return def.equals(i.def);
			return true;
		}
		
		public boolean isArray() {
			return type.charAt(type.length()-1) == ']';
		}
	}


	public static Seq<TableColumnInfo> createTableInfo(Class<?> type) {
		Seq<TableColumnInfo> infos = new Seq<>();
		for (var f : type.getDeclaredFields()) {
			@Nullable FIELD field = f.getDeclaredAnnotation(FIELD.class);
			boolean isArray = false;//f.getType().isArray() || f.getType() == Seq.class;
			Class<?> elementType = f.getType();
			if(f.getType().isArray()) {
				elementType = f.getType().getComponentType();
				isArray = true;
			}
			if(f.getType() == Seq.class && f.getGenericType() instanceof ParameterizedType) {
				elementType = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
				isArray = true;
			}
			
			String filedType = isArray ? (SQL.typesByClass.get(elementType) + "[]") : SQL.typesByClass.get(f.getType());
			if(field == null || filedType == null) continue;
			Log.info(filedType);
			@Nullable PRIMARY_KEY primaryKey = f.getAnnotation(PRIMARY_KEY.class);
			@Nullable DEFAULT def = f.getAnnotation(DEFAULT.class);
			infos.add(new TableColumnInfo(f.getName(), filedType, true, def == null ? null : def.value(), primaryKey != null));
		}
		return infos;
	}
	
	public static class Entity {
		
		
		
		
	}
}
