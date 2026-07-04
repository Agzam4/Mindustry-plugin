package agzam4.api.auth;

import agzam4.database.DBFields.AUTOINCREMENT;
import agzam4.database.DBFields.FIELD;
import agzam4.database.DBFields.PRIMARY_KEY;
import agzam4.database.DBFields.UNIQUE;
import agzam4.database.Entity;
import agzam4.database.Database;
import agzam4.database.Table;
import arc.files.Fi;
import arc.util.Nullable;

public class SensitiveData {

	public static class SensitiveEntity extends Entity {
		public @FIELD @AUTOINCREMENT @PRIMARY_KEY Integer id;
		public @FIELD @UNIQUE String value;
		public @FIELD String type;
	}

	private static Database db;
	private static Table<SensitiveEntity> table;

	public static void init(Fi path) throws Exception {
		db = new Database(path);
		table = db.createTable("sensitive", SensitiveEntity.class);
	}

	public enum SensitiveType {
		
		uuid, ip
		
	}
	
	public static synchronized int insertOrGet(String value, SensitiveType type) {
		var existing = table.query("value", value);
		if(!existing.isEmpty()) return existing.first().id;

		var entity = new SensitiveEntity();
		entity.value = value;
		entity.type = type.name();
		table.putNoKey(entity);

		var after = table.query("value", value);
		return after.isEmpty() ? -1 : after.first().id;
	}

	public static @Nullable String resolve(int id) {
		var entity = table.get(id);
		return entity != null ? entity.value : null;
	}
}
