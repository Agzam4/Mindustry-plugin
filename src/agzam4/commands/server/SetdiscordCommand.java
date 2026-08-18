package agzam4.commands.server;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.commands.CommandHandler;
import agzam4gen.config.ServerConfig;

public class SetdiscordCommand extends CommandHandler<Object> {

	{
		parms = "<link>";
		desc = "\ue80d Сервера";
	}

	@Override
	public void command(String[] arg, CommandSender sender, Object receiver, ReceiverType type) {
		if(arg.length != 1) return;
		ServerConfig.discordLink(arg[0]);
		sender.sendMessage(type.bungle("setdiscord"));
	}
	
}
