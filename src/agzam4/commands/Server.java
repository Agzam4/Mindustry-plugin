package agzam4.commands;

import agzam4.AgzamPlugin;
import arc.Core;
import arc.struct.ObjectSet;
import arc.util.Nullable;

public class Server {
	
	public String name = "сервер";

	public static @Nullable String discordLink;
	public static int doorsCap;
    public static ObjectSet<String> extrastarUids;
	
	@SuppressWarnings("unchecked")
	public static void init() {
		doorsCap = Core.settings.getInt(AgzamPlugin.name() + "-doors-cap", Integer.MAX_VALUE);
		discordLink = Core.settings.getString(AgzamPlugin.name() + "-discord-link", null);
		extrastarUids = Core.settings.getJson(AgzamPlugin.name() + "-extrastar-uids", ObjectSet.class, () -> new ObjectSet<String>());
	}
	
}
