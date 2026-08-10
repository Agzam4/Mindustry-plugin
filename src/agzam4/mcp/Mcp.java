package agzam4.mcp;

import java.io.IOException;
import com.sun.net.httpserver.HttpExchange;

import agzam4.AgzamPlugin;
import agzam4gen.mcp.tools.McpTools;
import agzam4proc.apt.api.lib.ApiResponse;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;

public class Mcp {

    private static McpStatelessSyncServer server;
    private static McpStatelessTransport transport;
	
	public static void init() {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(Mcp.class.getClassLoader());
			transport = new McpStatelessTransport(McpJsonDefaults.getMapper());
			
			var tools = McpTools.build();
			
			var builder = McpServer.sync(transport)
			        .serverInfo("mindustry-server", AgzamPlugin.version())
			        .instructions("Mindustry server tools") // TODO: better instructions
			        .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build());
			
			tools.each((tool, handler) -> {
				builder.toolCall(tool, handler);
			});
			server = builder.build();
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	public static void processMessage(HttpExchange exchange) throws ApiResponse, IOException {
         transport.handle(exchange);
	}
	
}
