package agzam4.commands.server;

import agzam4.AgzamPlugin;
import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.commands.CommandHandler;
import agzam4.commands.Server;
import arc.Core;

public class SetdiscordCommand extends CommandHandler<Object> {

	{
		parms = "<link>";
		desc = "\ue80d Сервера";
	}

	@Override
	public void command(String[] arg, CommandSender sender, Object receiver, ReceiverType type) {
		if(arg.length != 1) return;
		Server.discordLink = arg[0];
		Core.settings.put(AgzamPlugin.name() + "-discord-link", Server.discordLink);
		sender.sendMessage(type.bungle("setdiscord"));
	}
	
	
}
