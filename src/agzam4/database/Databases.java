package agzam4.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import agzam4.achievements.AchievementsManager;
import agzam4.achievements.AchievementsManager.Achievement;
import agzam4.database.DBFields.AUTOINCREMENT;
import agzam4.database.DBFields.DEFAULT;
import agzam4.database.DBFields.FIELD;
import agzam4.database.DBFields.PRIMARY_KEY;
import agzam4.utils.Log;
import arc.func.Cons;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Player;

public class Databases {
	
	private static @Nullable Connection connection;

	public static Table<PlayerEntity> players;
	public static Table<AchievementEntity> achievements;
	
	public static Database mainDatabase, logsDatabase;
	
	public static void init() throws ClassNotFoundException, SQLException {
		mainDatabase = new Database(Vars.dataDirectory.child("database.db"));
        players = mainDatabase.createTable("players", PlayerEntity.class);
        achievements = mainDatabase.createTable("achievements", AchievementEntity.class);
	}
	
	public static class KickEntity extends Entity {

		public @FIELD @AUTOINCREMENT @PRIMARY_KEY Integer id;
		
		/**
		 * Initiator of kick
		 */
		public @FIELD @DEFAULT(value="none") String player = "none";
		
		/**
		 * Kicked player
		 */
		public @FIELD @DEFAULT(value="none") String target = "none";
		
		/**
		 * Reason of kick (grief/spam/...)
		 */
		public @FIELD @DEFAULT(value="none") String reason = "none";
		
		/**
		 * Is kick checked by admins
		 */
		public @FIELD @DEFAULT(value="false") boolean approved = false;
		
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
		public @FIELD @DEFAULT(value="0") Integer playtime = 0; // minutes
		public @FIELD @DEFAULT(value="0") Integer truekicks = 0;
		public @FIELD @DEFAULT(value="0") Integer freekicks = 0;
		
		
		public int rate() {
			// log(true-kicks + 1) - log(free-kicks + 1) + log(play-time/60 + 1)
			return (int) Mathf.log2((truekicks + playtime/60f + 2f)/(freekicks+1f));
		}
		
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
			this.achievements = Databases.achievements.query("uuid", uuid);
		}
		
		public boolean achievement(Achievement achievement, int tier) {
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
				Databases.achievements.putNoKey(entity);
				Log.info("Status: NEW");
				Log.info("===========");
				return true;
			}
			if(entity.tier < tier) {
				entity.tier = tier;
				Databases.achievements.put(entity);
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

		public int sessionPlaytime() {
			return (int) TimeUnit.MILLISECONDS.toMinutes(Time.millis() - joinTime);
		}
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