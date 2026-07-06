package agzam4.api.endpoints;

import agzam4.Game;
import agzam4.admins.Admins;
import agzam4.api.auth.SensitiveData;
import agzam4.commands.Permissions;
import agzam4.managers.Players;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import agzam4proc.api.lib.ApiResponse;
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
	public static ResolvedPlayerStats me(@Auth PlayerInfo info) throws ApiResponse {
		return new ResolvedPlayerStats(info);
	}
	
}



