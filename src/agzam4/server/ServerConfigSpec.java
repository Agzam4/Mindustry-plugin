package agzam4.server;

import agzam4proc.apt.config.ConfigAnnotations.Config;

@Config("server")
public class ServerConfigSpec {

	/** Link to the Discord server, used in the /discord command */
	public static String discordLink = "";
	
}
