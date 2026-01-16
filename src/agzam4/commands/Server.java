package agzam4.commands;

import agzam4.AgzamPlugin;
import arc.Core;

public class Server {
	
	public String name = "сервер";

	public static String discordLink = Core.settings.getString(AgzamPlugin.name() + "-discord-link", null);
	
	
}
