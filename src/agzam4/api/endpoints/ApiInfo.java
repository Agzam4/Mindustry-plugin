package agzam4.api.endpoints;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import agzam4.Game;
import agzam4.admins.AdminData;
import agzam4.admins.Admins;
import agzam4.api.auth.SensitiveData;
import agzam4.api.auth.SensitiveData.SensitiveType;
import agzam4.commands.Permissions;
import agzam4.managers.Players;
import agzam4gen.api.dependencies.*;
import agzam4proc.apt.api.ApiAnnotations.*;
import agzam4proc.apt.api.lib.ApiResponse;
import arc.func.Func;
import arc.func.Func2;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;

@Router("/info")
public class ApiInfo {

    @Type
    public static class ResolvedPlayerStats {

    	public String uuid;
    	public String name;
		public long playtime;
    	
    	public ResolvedPlayerStats(PlayerInfo info) {
    		this.uuid = info.id;
    		this.name = info.lastName;
    		this.playtime = Players.gamePlaytime(info.id);
		}

    }
	
    
    @Type
    public static class ResolvedPlayerInfo {

    	public String uuid;
    	public String name;
    	
    	public ResolvedPlayerInfo(String uuid, String name) {
    		this.uuid = uuid;
    		this.name = name;
		}
    	
    }

    @Type
    public static class PlayerTrace {

    	public String uuid;
    	
    	// Logs
    	public String name;
    	public String[] names;
    	public @Nullable String ip;
    	public @Nullable String[] ips;

    	// Stats
        public int timesKicked;
        public int timesJoined;
		public long playtime;
        
        public boolean admin, helper;
        public @Nullable String[] permissions;
        
        // Bans
        public long lastKicked;
        public boolean permaban, dosBlacklist;

    	public PlayerTrace() {}

    }

	@Post
	public static ResolvedPlayerInfo[] resolvePlayer(@Auth PlayerInfo info, @BodyParm int[] ids) throws ApiResponse {
		boolean allowSensitiveData = Admins.has(info, Permissions.sensitiveData);
		ResolvedPlayerInfo[] result = new ResolvedPlayerInfo[ids.length];
		for (int i = 0; i < result.length; i++) {
			String uuid = SensitiveData.resolve(ids[i]);
			if(uuid == null) continue;
			result[i] = new ResolvedPlayerInfo(allowSensitiveData ? uuid : "", Game.nameByUuid(uuid));
		}
		return result;
	}

	@Post
	public static PlayerTrace playerTrace(@Auth PlayerInfo info, @BodyParm int id) throws ApiResponse {
		if(!Admins.has(info, Permissions.logs)) throw new ApiResponse("Forbidden").forbidden();
		
		boolean allowSensitiveData = Admins.has(info, Permissions.sensitiveData);
		boolean allowTraceAdmins = Admins.has(info, Permissions.traceAdmins);
		
		String uuid = SensitiveData.resolve(id);
		PlayerInfo target = Vars.netServer.admins.getInfo(uuid);
		if(target == null) return null;
		if(uuid == null) return null;
		PlayerTrace trace = new PlayerTrace();
		AdminData data = Admins.adminData(target);
		

		trace.name = target.lastName;
		trace.admin = target.admin;
		trace.helper = data != null;
		
		if((!trace.admin && !trace.helper) || allowTraceAdmins) {
			if(allowSensitiveData) {
				trace.uuid = uuid;
				trace.ip = target.lastIP;
				trace.ips = target.ips.toArray(String.class);
				if(data != null) trace.permissions = data.permissions();
			}
			
			trace.timesJoined = target.timesJoined;
			trace.timesKicked = target.timesKicked;
			
			trace.names = target.names.toArray(String.class);
			trace.playtime = Players.gamePlaytime(info.id);
			
			trace.lastKicked = target.lastKicked;
			trace.permaban = target.banned;
			
			for (int i = 0; i < target.ips.size; i++) {
				String ip = target.ips.get(i);
				if(!trace.dosBlacklist) trace.dosBlacklist = Vars.netServer.admins.dosBlacklist.contains(ip);
				trace.lastKicked = Math.max(trace.lastKicked, Vars.netServer.admins.kickedIPs.get(ip, 0L));
			}
		}
		
		return trace;
	}
	
	
	@Post
	public static ResolvedPlayerStats me(@Auth PlayerInfo info) throws ApiResponse {
		return new ResolvedPlayerStats(info);
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
    

    public static class SearchResultPlayer {

        public PlayerInfo player;
        public int score;

        public SearchResultPlayer(PlayerInfo player, int score) {
            this.player = player;
            this.score = score;
        }
    }

    private static Func<String, String> stripString = s -> Game.strip(s).trim().toLowerCase(Locale.ROOT);
    private static Func<String, String> cleanString = s -> s.replaceAll("(?U)[^\\p{L}\\p{N}_]", "");
	
	@Post
	public static PlayerSearchResult[] search(@Auth PlayerInfo info2, @BodyParm String query, @BodyParm int limit) throws ApiResponse {
		if(!Admins.has(info2, Permissions.logs)) throw new ApiResponse("Forbidden").forbidden();
		if(limit > 100) throw new ApiResponse("limit must be <= 100").forbidden();

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

}



