package agzam4.api;

import java.io.IOException;
import java.io.InputStreamReader;

import com.sun.net.httpserver.HttpExchange;

import agzam4gen.api.dependencies.Body;
import agzam4proc.api.ApiAnnotations.Dependency;
import agzam4proc.api.ApiAnnotations.Parm;
import agzam4proc.api.ApiSnippets.ApiResponse;
import arc.util.serialization.Jval;

public class Dependencies {
	
	@Dependency
	public class BodyDependency {
		
		public static Jval depends(HttpExchange exchange) throws ApiResponse, IOException {
			if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				String response = "Method not allowed";
				exchange.sendResponseHeaders(405, response.getBytes().length);
				exchange.getResponseBody().write(response.getBytes());
				exchange.getResponseBody().close();
				throw new ApiResponse(":)");
			}
			return Jval.read(new InputStreamReader(exchange.getRequestBody()));
		}
			
	}

	
	
	@Dependency
	public class BodyParmDependency {
		
		public static String depends(@Body Jval jval, @Parm String name) {
			return jval.getString(name);
		}
			
	}

}
