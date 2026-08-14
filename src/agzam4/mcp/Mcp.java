package agzam4.mcp;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;

import com.sun.net.httpserver.HttpExchange;

import agzam4.AgzamPlugin;
import agzam4gen.mcp.tools.McpTools;
import agzam4proc.apt.api.lib.ApiResponse;
import arc.Core;
import arc.struct.ObjectMap;
import arc.util.Log;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.meta.BlockGroup;

public class Mcp {

    private static McpStatelessSyncServer server;
    private static McpStatelessTransport transport;
    public static ObjectMap<String, BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult>> tools = ObjectMap.of();
    public static ObjectMap<String, BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult>> schemes = ObjectMap.of();
	
	public static void init() {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(Mcp.class.getClassLoader());
			transport = new McpStatelessTransport(McpJsonDefaults.getMapper());
			

			var builder = McpServer.sync(transport)
			        .serverInfo("mindustry-server", AgzamPlugin.version())
//			        .instructions("Mindustry server tools") // TODO: better instructions
			        .capabilities(McpSchema.ServerCapabilities.builder().tools(true)
//			        		.resources(true, false)
			        		.build());
			
			
			McpTools.build().each((tool, handler) -> {
				builder.toolCall(tool, handler);
				tools.put(tool.name(), handler);
			});
		
//			builder.resources(
//					new SyncResourceSpecification(McpSchema.Resource.builder("mindustry://glossary", "glossary").mimeType("text/markdown").build(),
//							(context, req) -> {
//								StringBuilder resources = new StringBuilder();
//								resources.append("# Blocks").append('\n');
//								for (var g : BlockGroup.values()) {
//									if(g == BlockGroup.none) continue;
//									resources.append("## ").append(g).append('\n');
//									Vars.content.blocks().select(b -> b.group == g).sort((b1,b2) -> b1.health - b2.health).each(b -> {
//										resources.append("- ").append(b.name).append('\n');
//									});
//								}
//								Log.info(resources);
//								return McpSchema.ReadResourceResult.builder(List.of(
//										McpSchema.TextResourceContents.builder("mindustry://glossary", resources.toString()).mimeType("text/markdown").build()
//										)).build();
//							}
//					)
//			);
//			
			server = builder.build();
			
			
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	public static void processMessage(HttpExchange exchange) throws ApiResponse, IOException {
         transport.handle(exchange);
	}
	
}
