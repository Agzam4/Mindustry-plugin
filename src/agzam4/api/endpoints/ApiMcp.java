package agzam4.api.endpoints;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

import agzam4.mcp.Mcp;
import agzam4gen.api.dependencies.McpRpc;
import agzam4proc.apt.api.ApiAnnotations.Post;
import agzam4proc.apt.api.ApiAnnotations.Router;
import agzam4proc.apt.api.lib.ApiResponse;

@Router("/mcp")
public class ApiMcp {
	
	// TODO: protection & disable MCP config
	
	@Post
	public static void server(@McpRpc HttpExchange body) throws ApiResponse, IOException {
		Mcp.processMessage(body);
	}
}
