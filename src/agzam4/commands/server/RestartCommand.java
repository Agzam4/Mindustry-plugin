package agzam4.commands.server;

import agzam4.Game;
import agzam4.CommandsManager;
import agzam4.CommandsManager.ResultSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;

public class RestartCommand extends CommandHandler<Object> {

	{
		parms = "[on/off/force]";
		desc = "Перезагрузить сервер";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		if(require(args.length != 1, sender, "Перезапуск: [lightgray]" + CommandsManager.needServerRestart)) return;
		if(args[0].equalsIgnoreCase("force")) {
			Game.stop();
			return;
		}
		if(args[0].equalsIgnoreCase("on")) {
			CommandsManager.needServerRestart = true;
			sender.sendMessage("Сервер будет перезапущен на следующей карте");
			return;
		}
		if(args[0].equalsIgnoreCase("off")) {
			if(require(!CommandsManager.needServerRestart, sender, "Перезапуск итак отменен")) return;
			CommandsManager.needServerRestart = false;
			sender.sendMessage("Перезапуск отменен");
			return;
		}
			
	}

	
	@Override
	public Seq<String> complete(String[] args, Object receiver, ReceiverType type) {
		if(CommandsManager.needServerRestart) return Seq.with("stop");
		return Seq.with("on", "force");
	}
	
}
