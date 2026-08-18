package agzam4.mcp;

import java.io.IOException;
import java.util.function.BiFunction;

import com.sun.net.httpserver.HttpExchange;

import agzam4.AgzamPlugin;
import agzam4gen.config.McpConfig;
import agzam4gen.mcp.tools.McpTools;
import agzam4proc.apt.api.lib.ApiResponse;
import agzam4proc.lib.PVars;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import mindustry.Vars;

public class Mcp {

	public static McpStatelessSyncServer server;
    private static McpStatelessTransport transport;
    public static ObjectMap<String, BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult>> tools = ObjectMap.of();
    public static ObjectMap<String, BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult>> schemes = ObjectMap.of();

    private static Fi tokensFile = PVars.dataDirectory.child("mcp").child("tokens.txt");
    
	private static ObjectSet<String> tokens = ObjectSet.with();
	
	public static void init() {
		if(!tokensFile.exists()) {
			tokensFile.parent().mkdirs();
		} else {
			tokens.addAll(tokensFile.readString().split("\n"));
		}
		
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
			
			if(!McpConfig.instructions.isEmpty()) {
				builder.instructions(McpConfig.instructions);
			}
			
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
		if(!McpConfig.enabled) throw ApiResponse.notFound;
		transport.handle(exchange);
	}

	public static String[] tokens() {
		return tokens.toSeq().toArray(String.class);
	}

	public static boolean hasToken(String token) {
		if(token.isEmpty()) return false;
		return tokens.contains(token);
	}

	public static boolean createToken(String token) {
		boolean ok = tokens.add(token);
		tokensFile.parent().mkdirs();
		tokensFile.writeString(Seq.with(token).toString("\n"));
		return ok;
	}
	
	public static boolean removeToken(String token) {
		boolean ok = tokens.remove(token);
		tokensFile.parent().mkdirs();
		tokensFile.writeString(Seq.with(token).toString("\n"));
		return ok;
	}
	
}
