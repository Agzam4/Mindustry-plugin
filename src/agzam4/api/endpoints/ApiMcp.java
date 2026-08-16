package agzam4.api.endpoints;

import java.io.IOException;
import java.util.UUID;

import com.sun.net.httpserver.HttpExchange;

import agzam4.admins.Admins;
import agzam4.commands.Permissions;
import agzam4.mcp.Mcp;
import agzam4gen.api.dependencies.Auth;
import agzam4gen.api.dependencies.HeaderParm;
import agzam4gen.api.dependencies.McpRpc;
import agzam4proc.apt.api.ApiAnnotations.Parm;
import agzam4proc.apt.api.ApiAnnotations.Post;
import agzam4proc.apt.api.ApiAnnotations.Router;
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
	public static String[] tokens(@Auth PlayerInfo info) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMcp)) throw ApiResponse.forbidden;
		return Mcp.tokens();
	}
	
	@Post
	public static String createToken(@Auth PlayerInfo info) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMcp)) throw ApiResponse.forbidden;
        String token = UUID.randomUUID().toString().replace("-", "");
		Mcp.createToken(token);
		return token;
	}

	@Post
	public static boolean deleteToken(@Auth PlayerInfo info, String token) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMcp)) throw ApiResponse.forbidden;
		return Mcp.removeToken(token);
	}
	
	
	
	
}
