package agzam4.api.endpoints;

import agzam4.Game;
import agzam4.PlayersData;
import agzam4.admins.Admins;
import agzam4.api.auth.SensitiveData;
import agzam4.api.auth.SensitiveData.SensitiveType;
import agzam4.commands.Permissions;
import agzam4gen.api.dependencies.Auth;
import agzam4gen.api.dependencies.BodyParm;
import agzam4proc.api.ApiAnnotations.Post;
import agzam4proc.api.ApiAnnotations.Router;
import agzam4proc.api.ApiAnnotations.Type;
import agzam4proc.api.lib.ApiResponse;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;

@Router("/admins")
public class ApiAdminds {

	@Type
	public static class HelperTrace {

		public String name;
		public @Nullable String adminName;
		
		public int id;
		
		public String[] permissions;
		
	}

	@Post
	public static HelperTrace[] helpers(@Auth PlayerInfo info) throws ApiResponse {
		if(!Admins.has(info, "helper")) throw new ApiResponse("Forbidden").forbidden();
		boolean allowSensitiveData = Admins.has(info, Permissions.sensitiveData);
		
		var all = Admins.admins();
		var keys = all.keys().toSeq();
		HelperTrace[] helpers = new HelperTrace[keys.size];
		for (int i = 0; i < helpers.length; i++) {
			var trace = new HelperTrace();
			var hinfo = keys.get(i);
			var data = all.get(hinfo);
			var playerData = PlayersData.data(hinfo.id);
			
			if(allowSensitiveData) {
				trace.id = SensitiveData.insertOrGet(hinfo.id, SensitiveType.uuid);
			}
			trace.name = hinfo.lastName;
			trace.adminName = playerData.name;
			
			trace.permissions = data.permissions();
			
			helpers[i] = trace;
		}
		return helpers;
	}


	@Post
	public static boolean addPermission(@Auth PlayerInfo info, @BodyParm int id, @BodyParm String permission) throws ApiResponse {
		if(!Admins.has(info, "helper")) throw new ApiResponse("Forbidden").forbidden();

		String uuid = SensitiveData.resolve(id);
		if(uuid == null) throw new ApiResponse("Player not found");
		var targetInfo = Vars.netServer.admins.getInfo(uuid);
		if(targetInfo == null) throw new ApiResponse("Player not found");
		var target = Admins.adminData(targetInfo);
		if(target == null) throw new ApiResponse("Player not helper");
		return target.add(permission);
	}
	
	@Post
	public static boolean removePermission(@Auth PlayerInfo info, @BodyParm int id, @BodyParm String permission) throws ApiResponse {
		if(!Admins.has(info, "helper")) throw new ApiResponse("Forbidden").forbidden();
		
		String uuid = SensitiveData.resolve(id);
		if(uuid == null) throw new ApiResponse("Player not found");
		var targetInfo = Vars.netServer.admins.getInfo(uuid);
		if(targetInfo == null) throw new ApiResponse("Player not found");
		var target = Admins.adminData(targetInfo);
		if(target == null) throw new ApiResponse("Player not helper");
		return target.remove(permission);
	}
	
	
}
