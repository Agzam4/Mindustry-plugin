package agzam4.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import reactor.core.publisher.Mono;

public class McpStatelessTransport implements McpStatelessServerTransport {

	private static final String APPLICATION_JSON = "application/json";
	private static final String TEXT_EVENT_STREAM = "text/event-stream";

	private final McpJsonMapper jsonMapper;
	private volatile McpStatelessServerHandler mcpHandler;

	public McpStatelessTransport(McpJsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
		this.mcpHandler = mcpHandler;
	}

	@Override
	public Mono<Void> closeGracefully() {
		return Mono.empty();
	}

	public void handle(HttpExchange exchange) throws IOException {
		try {
			if ("POST".equals(exchange.getRequestMethod())) {
				handlePost(exchange);
			} else {
				exchange.sendResponseHeaders(405, -1);
			}
		} finally {
			exchange.close();
		}
	}

	private void handlePost(HttpExchange exchange) throws IOException {
		String accept = exchange.getRequestHeaders().getFirst("Accept");
		if (accept == null || !(accept.contains(APPLICATION_JSON) && accept.contains(TEXT_EVENT_STREAM))) {
			sendMcpError(exchange, 400, McpSchema.ErrorCodes.METHOD_NOT_FOUND,
					"Both application/json and text/event-stream required in Accept header");
			return;
		}

		try {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);

			if (message instanceof McpSchema.JSONRPCRequest request) {
				McpSchema.JSONRPCResponse response = mcpHandler
						.handleRequest(McpTransportContext.EMPTY, request)
						.block();
				writeJson(exchange, 200, jsonMapper.writeValueAsString(response));
			} else if (message instanceof McpSchema.JSONRPCNotification notification) {
				mcpHandler.handleNotification(McpTransportContext.EMPTY, notification).block();
				exchange.sendResponseHeaders(202, -1);
			} else {
				sendMcpError(exchange, 400, McpSchema.ErrorCodes.INVALID_REQUEST,
						"The server accepts either requests or notifications");
			}
		} catch (IllegalArgumentException e) {
			sendMcpError(exchange, 400, McpSchema.ErrorCodes.INVALID_REQUEST, "Invalid message format");
		} catch (Exception e) {
			sendMcpError(exchange, 500, McpSchema.ErrorCodes.INTERNAL_ERROR,
					"Unexpected error: " + e.getMessage());
		}
	}

	private void sendMcpError(HttpExchange exchange, int httpCode, int mcpErrorCode, String message) throws IOException {
		McpError error = McpError.builder(mcpErrorCode).message(message).build();
		writeJson(exchange, httpCode, jsonMapper.writeValueAsString(error));
	}

	private void writeJson(HttpExchange exchange, int status, String json) throws IOException {
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type",
				APPLICATION_JSON + "; charset=" + StandardCharsets.UTF_8.name());
		exchange.sendResponseHeaders(status, bytes.length);
		try (var os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

}
