package agzam4proc.api;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import agzam4proc.api.ApiSnippets.ApiResponse;

public class ApiSnippets {

	public interface RouterHandler {
		
		String get(HttpExchange e) throws ApiResponse, IOException;
		
	};

	public static class ApiResponse extends Exception {
		
		public String content;
		public int code = 200;

		public ApiResponse(String string) {
			this.content = string;
		}

		public ApiResponse wrongParms() {
			code = 400;
			return this;
		}
		public ApiResponse serverError() {
			code = 500;
			return this;
		}
	}
	
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
	
}
