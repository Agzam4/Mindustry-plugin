package agzam4.commands.server;

import agzam4.CommandsManager.*;
import agzam4.commands.CommandHandler;
import arc.Core;
import mindustry.Vars;

public class JsCommand extends CommandHandler<Object> {

	{
		parms = "<script...>";
		desc = "Запустить JS";
	}

	@Override
	public void command(String[] arg, ResultSender sender, Object receiver, ReceiverType type) {
		if(type == ReceiverType.bot) {
			Core.app.post(() -> {
				sender.sendMessage(type.format("js", Vars.mods.getScripts().runConsole(arg[0])));
			});
		} else {
			sender.sendMessage(type.format("js", Vars.mods.getScripts().runConsole(arg[0])));
		}
	}
	

}
