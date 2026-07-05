package agzam4.api.endpoints;

import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;

@Router("/debug")
public class ApiDebug {
	
	@Post
	public static String ping(@SessionIp String ip, @SessionId String session) {
//		Log.info("[yellow]PING[]");
		return "Pong " + session + " " + ip;
	}
	
}
