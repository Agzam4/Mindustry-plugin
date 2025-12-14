package agzam4.managers;

import java.util.concurrent.TimeUnit;

import agzam4.database.Database;
import agzam4.database.Database.PlayerEntity;
import arc.Events;
import arc.struct.ObjectMap;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.gen.Player;

public class Players {

	private static ObjectMap<String, PlayerEntity> joined = new ObjectMap<>(); // UUID, PlayerEntity

	private static ObjectMap<String, PlayerMapSession> mapPlaytime = new ObjectMap<>(); // UUID, PlayerMapSession
	
	public static void init() {
    	Events.on(PlayerJoin.class, e -> {
    		/**
    		 * Loading PlayerEntity
    		 */
			PlayerEntity playerEntity = Database.player(e.player);
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
    			Database.players.put(playerEntity);
    		}
    		
    		var mpt = mapPlaytime.get(e.player.uuid());
    		if(mpt != null) mpt.update();
    	});

    	Events.on(GameOverEvent.class, e -> {
    		mapPlaytime.clear();
    	});
	}

	/**
	 * @param player
	 * @return player minutes on current map
	 */
	public static int mapPlaytime(Player player) {
		var mpt = mapPlaytime.get(player.uuid());
		if(mpt == null) return 0;
		return mpt.mapPlaytime;
	}
	
	public static @Nullable PlayerEntity joinedEntity(Player player) {
		return joined.get(player.uuid());
	}
	
	@Deprecated
	public static ObjectMap<String, PlayerEntity> getJoined() {
		return joined;
	}
	
	private static class PlayerMapSession {
		
		private long sessionJoinTime = 0; // millis
		private int mapPlaytime = 0; // minutes
		
		public void update() {
			mapPlaytime += TimeUnit.MILLISECONDS.toMinutes(Time.millis() - sessionJoinTime);
			sessionJoinTime = Time.millis();
		}
		
		
		
	}
	
}
