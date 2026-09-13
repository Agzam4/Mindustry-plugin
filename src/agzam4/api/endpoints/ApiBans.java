package agzam4.api.endpoints;

import agzam4.admins.Admins;
import agzam4.bans.Bans;
import agzam4gen.api.dependencies.Auth;
import agzam4gen.api.dependencies.BodyParm;
import agzam4proc.apt.api.ApiAnnotations.Post;
import agzam4proc.apt.api.ApiAnnotations.Router;
import agzam4proc.apt.api.ApiAnnotations.Type;
import agzam4proc.apt.api.lib.ApiResponse;
import mindustry.net.Administration.PlayerInfo;

@Router("/bans")
public class ApiBans {

	@Type
	public static class IpListInfo {
		
		public String name;
		public long size;
		
	}
	
    @Post
    public static IpListInfo[] groups(@Auth PlayerInfo player) throws ApiResponse {
    	if(!Admins.has(player, "banslist")) throw ApiResponse.forbidden;
    	return new IpListInfo[] {
    			new IpListInfo() {{
    					name = "bots";
    					size = Bans.bots.list.size();
    			}}
    	};
    }

    @Post
    public static long add(@Auth PlayerInfo player, @BodyParm String group, @BodyParm String cidr) throws ApiResponse {
    	if(!Admins.has(player, "banslist")) throw ApiResponse.forbidden;
    	try {
        	Bans.bots.list.addSubnet(cidr);
        	return Bans.bots.list.size();
		} catch (Exception e) {
			 throw new ApiResponse(e.getMessage()).serverError();
		}
    }

    @Post
    public static long remove(@Auth PlayerInfo player, @BodyParm String group, @BodyParm String cidr) throws ApiResponse {
    	if(!Admins.has(player, "banslist")) throw ApiResponse.forbidden;
    	try {
    		Bans.bots.removeSubnet(cidr);
    		return Bans.bots.list.size();
		} catch (Exception e) {
			 throw new ApiResponse(e.getMessage()).serverError();
		}
    }

    @Post
    public static boolean test(@Auth PlayerInfo player, @BodyParm String group, @BodyParm String ip) throws ApiResponse {
    	if(!Admins.has(player, "banslist")) throw ApiResponse.forbidden;
    	try {
        	return Bans.bots.has(ip);
		} catch (Exception e) {
			 throw new ApiResponse(e.getMessage()).serverError();
		}
    }
    

    @Post
    public static String get(@Auth PlayerInfo player, @BodyParm String group, @BodyParm String cidr) throws ApiResponse {
    	if(!Admins.has(player, "banslist")) throw ApiResponse.forbidden;
    	return Bans.bots.list.file.readString();
    }
	
}
