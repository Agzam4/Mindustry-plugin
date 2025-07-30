package agzam4.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import agzam4.achievements.AchievementsManager;
import agzam4.achievements.AchievementsManager.Achievement;
import agzam4.database.DBFields.AUTOINCREMENT;
import agzam4.database.DBFields.DEFAULT;
import agzam4.database.DBFields.FIELD;
import agzam4.database.DBFields.PRIMARY_KEY;
import agzam4.database.SQL.TableColumnInfo;
import arc.func.Cons;
import arc.func.Func;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.gen.Player;

public class Database {
	
	private static @Nullable Connection connection;

	public static Table<PlayerEntity> players;
	public static Table<AchievementEntity> achievements;
	
	public static void init(String path) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		
        String url = "jdbc:sqlite:" + path + ".db";
//        DriverManager.setLogWriter(new PrintWriter(System.err));
        try {
        	var c = DriverManager.getConnection(url);
        	Log.info("Connected to SQL");
        	connection = c;
        	players = new Table<PlayerEntity>("players", PlayerEntity.class);
        	achievements = new Table<AchievementEntity>("achievements", AchievementEntity.class);
        	Log.info("player: @", players.get("myuuid"));
        } catch (SQLException e) {
        	Log.err(e);
        }
	}
	
	/**
	 * Executes the given SQL statement
	 * @param SQL statement
	 */
	public static void execute(String sql) {
		try (Statement s = connection.createStatement()) {
			s.execute(sql);
		} catch (SQLException e) {
			Log.err(e);
		}
	}
	
	/**
	 * Executes the SQL statement in this PreparedStatement object, 
	 * which must be an SQL Data Manipulation Language (DML) statement, 
	 * such as INSERT, UPDATE or DELETE; or an SQL statement that returns nothing, 
	 * such as a DDL statement.
	 * 
	 * @param SQL statement
	 * @param args - object to "?" places
	 */
	public static void update(String sql, Object... args) {
		try (PreparedStatement s = connection.prepareStatement(sql)) {
			for (int i = 0; i < args.length; i++) {
				s.setObject(i+1, args[i]);
			}
			s.executeUpdate();
		} catch (SQLException e) {
			Log.err(e);
		}		
	}
	
	/**
	 * Executes the several SQL statement, and rollback on error
	 * @param executes
	 * @return true if all success
	 */
	public static boolean executeTransaction(String...executes) {
		try (Statement s = connection.createStatement()) {
			connection.setAutoCommit(false);
			Log.info("[SQL] == [Transaction] ==========");
			for (int i = 0; i < executes.length; i++) {
				Log.info("[SQL] Transaction > @", executes[i]);
				s.execute(executes[i]);
			}
			connection.commit();
			return true;
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				Log.err(e1);
			}
			Log.err(e);
			return false;
		} finally {
			try {
				Log.info("[SQL] == [Transaction] ==========");
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				Log.err(e);
	        	Log.err("SQL:\n@", Arrays.toString(executes));
			}
		}
	}

	/**
	 * Executes the several SQL statement, and rollback on error
	 * @param sql - SQL statement
	 * @param cons - consumer of results
	 * @return
	 */
	public static void queryCons(String sql, Cons<ResultSet> cons) {
		try (Statement stmt = connection.createStatement()) {
			ResultSet set = stmt.executeQuery(sql);
			while (set.next()) cons.get(set);
		} catch (SQLException e) {
			Log.err(e);
		}
	}

	public static @Nullable <T> T query(String sql, Func<ResultSet, T> func) {
		T result = null;
		try (Statement stmt = connection.createStatement()) {
			result = func.get(stmt.executeQuery(sql));
		} catch (SQLException e) {
			Log.err(e);
		}
		return result;
	}

	public static @Nullable <T> T query(String sql, Func<ResultSet, T> func, Object... args) {
		try (PreparedStatement s = connection.prepareStatement(sql)) {
			for (int i = 0; i < args.length; i++) {
				s.setObject(i+1, args[i]);
			}
			return func.get(s.executeQuery());
		} catch (SQLException e) {
			Log.err(e);
		}	
		return null;
	}

	/**
	 * Query info about table by name
	 * @param name - name of table
	 * @return map of type by filed name
	 */
	public static @Nullable Seq<TableColumnInfo> queryTableInfo(String name) {
		return query("PRAGMA table_info(" + name + ")", result -> {
			try {
				Seq<TableColumnInfo> infos = new Seq<>();
				while (result.next()) infos.add(new TableColumnInfo(result));
				return infos;
			} catch (SQLException e) {
				Log.err(e);
			}
			return null;
		});
	}

	public static @Nullable  Seq<String> queryTableList() {
		return query("SELECT name FROM sqlite_master WHERE type='table'", result -> {
			try {
				Seq<String> tables = new Seq<>();
				while (result.next()) tables.add(result.getString("name"));
			} catch (SQLException e) {
				Log.err(e);
			}
			return null;
		});
	}
	

	public static class AchievementEntity extends Entity {

		public @FIELD @AUTOINCREMENT @PRIMARY_KEY Integer id;
		public @FIELD @DEFAULT(value="none") String uuid = "none";
		public @FIELD @DEFAULT(value="0") Integer map = 0;
		public @FIELD @DEFAULT(value="0") Integer type = 0;
		public @FIELD @DEFAULT(value="0") Integer tier = 0;
		
	}
	
	public static class PlayerEntity extends Entity {
		
		public @FIELD @PRIMARY_KEY String uuid;
		public @FIELD @DEFAULT(value="0") Integer playtime = 0;

		/**
		 * Set <MapId, achievementTypes, achievementTiers>
		 */
//		public @FIELD Seq<Integer> achievementMaps = new Seq<>();
//		public @FIELD Seq<Integer> achievementTypes = new Seq<>();
//		public @FIELD Seq<Integer> achievementTiers = new Seq<>();

		public long joinTime;
		
		@Override
		public String toString() {
			return Strings.format("uuid=@, playtime=@, achievementMaps=@, achievementTypes=@, achievementTiers=@", 
					uuid, playtime, null, null, null);
		}
		
		private @Nullable Seq<AchievementEntity> achievements = null;
		
		private void loadAchievements() {
			this.achievements = Database.achievements.query("uuid", uuid);
			Log.info("@ loaded: @", uuid, achievements);
		}
		
		public boolean achievement(Achievement achievement, int tier) {
			Log.info("== Grant ==");
			Log.info("tier: @", tier);
			final int mapId = AchievementsManager.mapId();
			@Nullable AchievementEntity entity = achievements.find(e -> e.type == achievement.id && e.map == mapId);
			if(tier < 1) return false;
			if(entity == null) {
				entity = new AchievementEntity();
				entity.uuid = uuid;
				entity.map = mapId;
				entity.tier = tier;
				entity.type = achievement.id;
				achievements.add(entity);
				Database.achievements.putNoKey(entity);
				Log.info("Status: NEW");
				Log.info("===========");
				return true;
			}
			if(entity.tier < tier) {
				entity.tier = tier;
				Database.achievements.put(entity);
				Log.info("Status: UPDATED");
				Log.info("===========");
				return true;
			}
			Log.info("Status: Reject");
			Log.info("===========");
			return false;
		}
		

		public int achievement(Achievement achievement) {
			final int mapId = AchievementsManager.mapId();
			@Nullable AchievementEntity entity = achievements.find(e -> e.type == achievement.id && e.map == mapId);
			if(entity == null) return 0;
			return entity.tier;
		}
		
		public void eachAchievements(Cons<AchievementEntity> cons) {
			achievements.each(cons);
		}
//		
//		public boolean achievement(Achievement achievement, int mapId, int tier) {
//			if(tier <= 0) return false;
//			Log.info("| @ @ @", achievement, mapId, tier);
//			if(achievementMaps == null || achievementTypes == null || achievementTiers == null) {
//				achievementMaps = new Seq<>();
//				achievementTypes = new Seq<>();
//				achievementTiers = new Seq<>();
//			}
//			if((achievementMaps.size != achievementTypes.size) || (achievementMaps.size != achievementTiers.size)) {
//				Log.err("length is different uuid=@", uuid);
//				return false;
//			}
//			for (int i = 0; i < achievementMaps.size; i++) {
//				if(achievementMaps.get(i) != mapId) continue;
//				if(achievementTypes.get(i) != achievement.id) continue;
//				
//				if(achievementTiers.get(i) >= tier) return false; // Found but tier is less
//				achievementTiers.set(i, tier); // update tier
//				return true;
//			}
//			// not found: adding new
//			achievementMaps.add(mapId);
//			achievementTypes.add(achievement.id);
//			achievementTiers.add(tier);
//			return true;
//		}
//
//		public int achievementTier(Achievement achievement) {
//			return achievementTier(achievement, AchievementsManager.mapsIds.get(Vars.state.map.name()));
//		}
//
//		public int achievementTier(Achievement achievement, int mapId) {
//			if(achievementMaps == null || achievementTypes == null || achievementTiers == null) {
//				achievementMaps = new Seq<>();
//				achievementTypes = new Seq<>();
//				achievementTiers = new Seq<>();
//			}
//			if((achievementMaps.size != achievementTypes.size) || (achievementMaps.size != achievementTiers.size)) {
//				Log.err("length is different uuid=@", uuid);
//				return 0;
//			}
//			for (int i = 0; i < achievementMaps.size; i++) {
//				if(achievementMaps.get(i) != mapId) continue;
//				if(achievementTypes.get(i) != achievement.id) continue;
//				return achievementTiers.get(i);
//			}
//			return 0;
//		}
	}

	
	public static PlayerEntity player(Player player) {
		return player(player.uuid());
	}

	public static PlayerEntity player(String playerUuid) {
		var entity = players.get(playerUuid, () -> new PlayerEntity() {{
			uuid = playerUuid;
			playtime = 0;
		}});
		entity.loadAchievements();
		return entity;
	}


//	public static boolean achievement(String uuid, Achievement achievement, int map, int tier) {
//		var table = mapsAchievementsTable(map);
//		MapAchievementEntity entity = table.query("uuid", uuid, "type", achievement.id);
//		if(tier > entity.tier) {
//			entity.tier = tier;
//			table.put(entity);
//			return true;
//		}
//		return false;
//	}
//	
//	private static Table<MapAchievementEntity> mapsAchievementsTable(int mapId) {
//		@Nullable Table<MapAchievementEntity> table = mapsAchievements.get(mapId);
//		if(table == null) {
//			table = new Table<MapAchievementEntity>("map_achievements_" + mapId, MapAchievementEntity.class);
//			mapsAchievements.put(mapId, table);
//		}
//		return table;
//	}
	
}