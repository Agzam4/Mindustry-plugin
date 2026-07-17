package agzam4.api.endpoints;

import java.util.concurrent.TimeUnit;

import agzam4.Game;
import agzam4.admins.AdminData;
import agzam4.admins.Admins;
import agzam4.api.auth.SensitiveData;
import agzam4.commands.Permissions;
import agzam4.managers.Players;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import agzam4proc.api.lib.ApiResponse;
import arc.util.Nullable;
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
	
}



