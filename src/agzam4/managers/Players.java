package agzam4.managers;

import java.util.concurrent.TimeUnit;

import agzam4.database.Databases;
import agzam4.database.Databases.PlayerEntity;
import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.gen.Player;

public class Players {

	private static ObjectMap<String, PlayerEntity> joined = ObjectMap.of(); // UUID, PlayerEntity

	private static ObjectMap<String, PlayerMapSession> mapPlaytime = ObjectMap.of(); // UUID, PlayerMapSession
	
	private static ObjectSet<String> disabled = ObjectSet.with();
	
	public static void init() {
    	Events.on(PlayerJoin.class, e -> {
    		/**
    		 * Loading PlayerEntity
    		 */
			PlayerEntity playerEntity = Databases.player(e.player);
			playerEntity.joinTime = Time.millis();
    		joined.put(e.player.uuid(), playerEntity);
    		
    		mapPlaytime.get(e.player.uuid(), () -> new PlayerMapSession()).sessionJoinTime = Time.millis();
    	});

    	Events.on(PlayerLeave.class, e -> {
    		/**
    		 * Saving PlayerEntity
    		 */
    		@Nullable PlayerEntity playerEntity = joined.remove(e.player.uuid());
    		if(playerEntity != null) {
    			playerEntity.playtime += playerEntity.sessionPlaytime();
    			Databases.players.put(playerEntity);
    		}
    		
    		var mpt = mapPlaytime.get(e.player.uuid());
    		if(mpt != null) mpt.update();
    	});

    	Events.on(GameOverEvent.class, e -> {
    		mapPlaytime.clear();
    	});
    	
    	Vars.netServer.admins.addActionFilter(action -> {
    		if(action.player == null) return true;
    		return !disabled(action.player);
    	});
	}

	/**
	 * @param player
	 * @return player minutes on current map
	 */
	public static int mapPlaytime(Player player) {
		var mpt = mapPlaytime.get(player.uuid());
		if(mpt == null) return 0;
		return mpt.playtime();
	}
	
	/**
	 * @return player play time total minutes
	 */
	public static int gamePlaytime(Player player) {
		var ent = joinedEntity(player);
		if(ent == null) return 0;
		return ent.playtime + ent.sessionPlaytime();
	}
	
	public static @Nullable PlayerEntity joinedEntity(Player player) {
		return joined.get(player.uuid());
	}

	public static @Nullable PlayerEntity joinedEntity(String uuid) {
		return joined.get(uuid);
	}

	public static boolean disabled(Player player) {
//		long comp = Time.millis();
//		boolean d = disabled.get(player.uuid(), comp) < comp;
//		if(!d) disabled.remove(player.uuid()); // clear not disabled
		return disabled.contains(player.uuid());
	}

	public static boolean disable(Player player) {
		return disabled.add(player.uuid());
	}
	
	public static boolean enable(Player player) {
		return disabled.remove(player.uuid());
	}
	
	@Deprecated
	public static ObjectMap<String, PlayerEntity> getJoined() {
		return joined;
	}
	
	public static int joinedAmount() {
		return joined.size;
	}
	
	private static class PlayerMapSession {
		
		private long sessionJoinTime = 0; // millis
		private int mapPlaytime = 0; // minutes
		
		public void update() {
			mapPlaytime = playtime();
			sessionJoinTime = Time.millis();
		}
		
		private int playtime() {
			return (int) (mapPlaytime + TimeUnit.MILLISECONDS.toMinutes(Time.millis() - sessionJoinTime));
		}
		
	}
	
}
