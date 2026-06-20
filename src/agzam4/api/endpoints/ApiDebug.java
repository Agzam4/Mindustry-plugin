package agzam4.api.endpoints;

import agzam4.api.ApiAnnotations.HeadField;
import agzam4.api.ApiAnnotations.PostEndpoint;
import agzam4.utils.Log;
import agzam4proc.api.annotations.Router;

@Router("/debug")
public class ApiDebug {
	
	@PostEndpoint
	public static String ping(@HeadField("Client-Ip") String ip, @HeadField("Session-Id") String session) {
		Log.info("[yellow]PING[]");
		return "Pong " + session + " " + ip;
	}
	
}
