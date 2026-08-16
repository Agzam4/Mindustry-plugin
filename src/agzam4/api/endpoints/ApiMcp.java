package agzam4.api.endpoints;

import java.io.IOException;
import java.util.UUID;

import com.sun.net.httpserver.HttpExchange;

import agzam4.admins.Admins;
import agzam4.commands.Permissions;
import agzam4.mcp.Mcp;
import agzam4gen.api.dependencies.Auth;
import agzam4gen.api.dependencies.BodyParm;
import agzam4gen.api.dependencies.HeaderParm;
import agzam4gen.api.dependencies.McpRpc;
import agzam4proc.apt.api.ApiAnnotations.Parm;
import agzam4proc.apt.api.ApiAnnotations.Post;
import agzam4proc.apt.api.ApiAnnotations.Router;
import agzam4proc.apt.api.ApiAnnotations.Type;
import agzam4proc.apt.api.lib.ApiResponse;
import arc.struct.ObjectSet;
import mindustry.net.Administration.PlayerInfo;

@Router("/mcp")
public class ApiMcp {
	
	// TODO: protection & disable MCP config

	@Post
	public static void server(@HeaderParm @Parm("Authorization") String auth, @McpRpc HttpExchange body) throws ApiResponse, IOException {
		if(!Mcp.hasToken(auth)) throw ApiResponse.forbidden;
		Mcp.processMessage(body);
	}
	
	@Post
	public static McpSession[] tokens(@Auth PlayerInfo info) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMcp)) throw ApiResponse.forbidden;
		var list = Mcp.tokens();
		McpSession[] result = new McpSession[list.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new McpSession();
			result[i].token = list[i];
		}
		return result;
	}
	
	@Post
	public static String createToken(@Auth PlayerInfo info) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMcp)) throw ApiResponse.forbidden;
        String token = UUID.randomUUID().toString().replace("-", "");
		var ok = Mcp.createToken(token);
		if(!ok) throw new ApiResponse("Dublicating token");
		return token;
	}

	@Post
	public static boolean deleteToken(@Auth PlayerInfo info, @BodyParm String token) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMcp)) throw ApiResponse.forbidden;
		return Mcp.removeToken(token);
	}
	
	
	@Type
	public static class McpSession {
		
		public String token;
	
	}
	
	
	
}
