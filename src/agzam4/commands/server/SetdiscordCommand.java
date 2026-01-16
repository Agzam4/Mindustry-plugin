package agzam4.commands.server;

import agzam4.AgzamPlugin;
import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.commands.CommandHandler;
import arc.Core;

public class SetdiscordCommand extends CommandHandler<Object> {

	public static String discordLink = Core.settings.getString(AgzamPlugin.name() + "-discord-link", null);
	
	{
		parms = "<link>";
		desc = "\ue80d Сервера";
	}

	@Override
	public void command(String[] arg, ResultSender sender, Object receiver, ReceiverType type) {
		if(arg.length != 1) return;
		discordLink = arg[0];
		Core.settings.put(AgzamPlugin.name() + "-discord-link", discordLink);
		sender.sendMessage(type.bungle("setdiscord"));
	}
	
	
}
