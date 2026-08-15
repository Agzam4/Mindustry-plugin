package agzam4.managers;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import agzam4.Game;
import agzam4.api.auth.SensitiveData;
import agzam4.api.auth.SensitiveData.SensitiveType;
import agzam4.api.endpoints.ApiInfo.SearchResultPlayer;
import agzam4.database.Databases;
import agzam4.database.Databases.PlayerEntity;
import agzam4proc.apt.api.ApiAnnotations.Type;
import arc.Events;
import arc.func.Func;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.gen.Player;
import mindustry.net.Administration.PlayerInfo;

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

	public static int gamePlaytime(String uuid) {
		var ent = joinedEntity(uuid);
		if(ent == null) {
			ent = Databases.player(uuid);
			ent.joinTime = Time.millis();
		}
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


	@Type
    public static class PlayerSearchResult {

        public int id;
        public int score;

        public PlayerSearchResult(int id, int score) {
            this.id = id;
            this.score = score;
        }
    }

    private static Func<String, String> stripString = s -> Game.strip(s).trim().toLowerCase(Locale.ROOT);
    private static Func<String, String> cleanString = s -> s.replaceAll("(?U)[^\\p{L}\\p{N}_]", "");
	
	public static PlayerSearchResult[] search(String query, int limit) {
		Seq<SearchResultPlayer> results = new Seq<>(limit);
		if (query == null || query.trim().isEmpty()) return new PlayerSearchResult[0];

		String lowerQuery = stripString.get(query);
        String cleanQuery = cleanString.get(query);
        
        boolean isIpQuery = lowerQuery.matches("^[0-9a-f.:]+$");
        
		Vars.netServer.admins.playerInfo.each((uuid, player) -> {
			int score = 0;

			// IP search [150,200]
			if (isIpQuery) {
				for (var ip : player.ips) {
					if (ip.equals(lowerQuery)) {
						score = 200;
					}
				}
				if(score < 150) {
					for (var ip : player.ips) {
						if (ip.startsWith(lowerQuery)) {
							score = 150;
						}
					}
				}
				// In theory it is possible player with IP in nickname so not end checking
			}

			// Name search
			if (score == 0) {
				String strippedNick = stripString.get(player.lastName);

				if (strippedNick.equals(lowerQuery)) {
					score = 100;
				} else if (strippedNick.startsWith(lowerQuery)) {
					score = 80; 
				} else if (strippedNick.contains(lowerQuery)) {
					score = 60; 
				} else {
					String cleanNick = cleanString.get(strippedNick);
					if (!cleanQuery.isEmpty() && !cleanNick.isEmpty()) {
						if (cleanNick.equals(cleanQuery)) {
							score = 75; 
						} else if (cleanNick.startsWith(cleanQuery)) {
							score = 55;
						} else if (cleanNick.contains(cleanQuery)) {
							score = 45;
						}
					}
				}
			}

			// Name search
			if(score < 40) {
				for (String nick : player.names) {
					if (nick == null) continue;
					String strippedOld = Game.strip(nick).toLowerCase(Locale.ROOT);
					if (strippedOld.equals(lowerQuery)) { score = 40; break; }
					else if (strippedOld.startsWith(lowerQuery)) { score = 30; break; }
					else if (strippedOld.contains(lowerQuery)) { score = 20; break; }
				}
			}

			if(score <= 0) return; // Not found

			if(results.size < limit) {
				results.add(new SearchResultPlayer(player, score));
				if(results.size == limit) results.sort((r1, r2) -> Integer.compare(r2.score, r1.score));
				return;
			}
			
			var worstResult = results.peek(); 

			if(score > worstResult.score) {
				results.pop(); 
				results.add(new SearchResultPlayer(player, score));
				results.sort((r1, r2) -> Integer.compare(r2.score, r1.score));
			}
		});

		results.sort((r1, r2) -> Integer.compare(r2.score, r1.score));
		
		PlayerSearchResult[] r = new PlayerSearchResult[results.size];
		for (int i = 0; i < r.length; i++) {
			r[i] = new PlayerSearchResult(SensitiveData.insertOrGet(results.get(i).player.id, SensitiveType.uuid), results.get(i).score);
		}
		return r;
	
	}

	public static @Nullable PlayerInfo resolveId(int id) {
		var uuid = SensitiveData.resolve(id);
		if(uuid == null) return null;
		return Vars.netServer.admins.playerInfo.get(uuid);
	}
	

	
}
