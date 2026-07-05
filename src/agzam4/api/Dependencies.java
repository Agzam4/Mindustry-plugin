package agzam4.api;

import java.io.IOException;
import java.io.InputStreamReader;

import com.sun.net.httpserver.HttpExchange;

import agzam4.admins.AdminData;
import agzam4.admins.Admins;
import agzam4.api.auth.AuthDatabase;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import agzam4proc.api.lib.ApiResponse;
import arc.util.Strings;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;

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
	@RequireBody
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

		@DependencyImpl
		public static String[] dependsStrings(@Body Jval jval, @CallerParm String name) {
			var array = jval.get(name).asArray();
			var strings = new String[array.size];
			for (int i = 0; i < strings.length; i++) strings[i] = array.get(i).asString();
			return strings;
		}
		
		@DependencyImpl
		public static int[] dependsInts(@Body Jval jval, @CallerParm String name) {
			var array = jval.get(name).asArray();
			var ints = new int[array.size];
			for (int i = 0; i < ints.length; i++) ints[i] = array.get(i).asInt();
			return ints;
		}

		@DependencyImpl
		public static long[] dependsLongs(@Body Jval jval, @CallerParm String name) {
			var array = jval.get(name).asArray();
			var longs = new long[array.size];
			for (int i = 0; i < longs.length; i++) longs[i] = array.get(i).asLong();
			return longs;
		}
		
		@DependencyImpl
		public static boolean[] dependsBooleans(@Body Jval jval, @CallerParm String name) {
			var array = jval.get(name).asArray();
			var booleans = new boolean[array.size];
			for (int i = 0; i < booleans.length; i++) booleans[i] = array.get(i).asBool();
			return booleans;
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

	@Dependency
	public class AuthDependency {

		@DependencyImpl
		public static String dependsUuid(@SessionId String sessionId, @SessionIp String ip) throws ApiResponse {
			var uuid = AuthDatabase.validate(sessionId, ip);
			if(uuid == null) throw new ApiResponse("Unauthorized").unauthorized();
			return uuid;
		}

		@DependencyImpl
		public static PlayerInfo dependsPlayerInfo(@SessionId String sessionId, @SessionIp String ip) throws ApiResponse {
			var uuid = AuthDatabase.validate(sessionId, ip);
			if(uuid == null) throw new ApiResponse("Unauthorized").unauthorized();
			var info = Vars.netServer.admins.playerInfo.get(uuid);
			if(info == null) throw new ApiResponse("Unauthorized").unauthorized();
			return info;
		}
		
		@DependencyImpl
		public static AdminData dependsAdminData(@SessionId String sessionId, @SessionIp String ip) throws ApiResponse {
			var uuid = AuthDatabase.validate(sessionId, ip);
			if(uuid == null) throw new ApiResponse("Unauthorized").unauthorized();
			var info = Vars.netServer.admins.playerInfo.get(uuid);
			if(info == null) throw new ApiResponse("Unauthorized").unauthorized();
			var admin = Admins.adminData(info);
			if(admin == null) throw new ApiResponse("Forbidden").forbidden();
			return admin;
		}
		
	}

	@Dependency
	@Deprecated
	public class PermissionDependency {

		@DependencyImpl
		public static boolean depends(@Auth PlayerInfo info, @CallerParm String permission) throws ApiResponse {
			return Admins.has(info, Strings.camelToKebab(permission));
		}
		
	}

	@Dependency
	@Deprecated
	public class RequirePermissionDependency {

		@DependencyImpl
		public static PlayerInfo depends(@Auth PlayerInfo info, @CallerParm String permission) throws ApiResponse {
			if(!Admins.has(info, Strings.camelToKebab(permission))) throw new ApiResponse("Forbidden").forbidden();
			return info;
		}
		
	}

	
	
}
