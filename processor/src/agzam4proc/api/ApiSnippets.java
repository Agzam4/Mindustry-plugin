package agzam4proc.api;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import agzam4proc.api.lib.ApiResponse;

public class ApiSnippets {

	public interface RouterHandler {

		String get(HttpExchange e) throws ApiResponse, IOException;

	};

	public interface RouterSseHandler {

		void get(HttpExchange e) throws ApiResponse, IOException;

	};

	public static void registerEndpoint(HttpServer server, String path, RouterHandler handler) {
		server.createContext(path, exchange -> {
			try {
				String response = handler.get(exchange);
				exchange.sendResponseHeaders(200, response.getBytes().length);
				exchange.getResponseBody().write(response.getBytes());
				exchange.getResponseBody().close();
			} catch (IOException e) {
				throw e;
			} catch (ApiResponse e) {
				String response = e.content;
				exchange.sendResponseHeaders(e.code, response.getBytes().length);
				exchange.getResponseBody().write(response.getBytes());
				exchange.getResponseBody().close();
			} catch (Throwable e) {
				String response = (e.getCause().getClass().getSimpleName()) + (e.getCause().getMessage() == null ? "" : ": " + e.getCause().getMessage());
				exchange.sendResponseHeaders(500, response.getBytes().length);
				exchange.getResponseBody().write(response.getBytes());
				exchange.getResponseBody().close();
			}
		});
	}


	public static void registerSseEndpoint(HttpServer server, String path, RouterSseHandler handler) {
		server.createContext(path, exchange -> {
			try {
				handler.get(exchange);
			} catch (IOException e) {
				throw e;
			} catch (ApiResponse e) {
				String response = e.content;
				exchange.sendResponseHeaders(e.code, response.getBytes().length);
				exchange.getResponseBody().write(response.getBytes());
				exchange.getResponseBody().close();
			} catch (Throwable e) {
				String response = (e.getCause().getClass().getSimpleName()) + (e.getCause().getMessage() == null ? "" : ": " + e.getCause().getMessage());
				exchange.sendResponseHeaders(500, response.getBytes().length);
				exchange.getResponseBody().write(response.getBytes());
				exchange.getResponseBody().close();
			}
		});
	}
}
