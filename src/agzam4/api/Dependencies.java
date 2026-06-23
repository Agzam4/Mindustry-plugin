package agzam4.api;

import java.io.IOException;
import java.io.InputStreamReader;

import com.sun.net.httpserver.HttpExchange;

import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import agzam4proc.api.lib.ApiResponse;
import arc.util.Strings;
import arc.util.serialization.Jval;

public class Dependencies {
	
	/**
	 * Extracting and validating the HTTP request body
	 * <p>
	 * Only allows {@code POST} requests:<br>
	 * Sends an HTTP 405 (Method Not Allowed) if method is not POST.
	 * </p>
	 */
	@Dependency
	public class BodyDependency {

		@DependencyImpl
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

	
	/**
	 * Extracts a specific string parameter from JSON body
	 */
	@Dependency
	public class BodyParmDependency {

		@DependencyImpl
		public static String dependsString(@Body Jval jval, @CallerParm String name) {
			return jval.getString(name);
		}

		@DependencyImpl
		public static int dependsInt(@Body Jval jval, @CallerParm String name) {
			return jval.getInt(name, 0);
		}
		
		@DependencyImpl
		public static long dependsLong(@Body Jval jval, @CallerParm String name) {
			return jval.getLong(name, 0L);
		}

		@DependencyImpl
		public static boolean dependsBoolean(@Body Jval jval, @CallerParm String name) {
			return jval.getBool(name, false);
		}
		
	}

	/**
	 * Extracts custom HTTP headers
	 */
	@Dependency
	public class HeaderParmDependency {

		@DependencyImpl
		public static String depends(HttpExchange e, @CallerParm String header) throws ApiResponse {
			var h = e.getRequestHeaders().getFirst("Agzam4-" + header);
			if(h == null) throw new ApiResponse(Strings.format("Wrong header: no \"Agzam4-@\" header", header)).wrongParms();
			return h;
		}

	}
	

	/**
	 * Retrieves session IP from headers
	 */
	@Dependency
	public class SessionIpDependency {

		@DependencyImpl
		public static String depends(@HeaderParm @Parm("Client-Ip") String ip) throws ApiResponse {
			return ip;
		}

	}

	/**
	 * Retrieves session ID from headers
	 */
	@Dependency
	public class SessionIdDependency {

		@DependencyImpl
		public static String depends(@HeaderParm @Parm("Session-Id") String id) throws ApiResponse {
			return id;
		}

	}
	
}
