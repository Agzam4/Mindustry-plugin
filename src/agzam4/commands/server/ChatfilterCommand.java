package agzam4.commands.server;

import agzam4.AgzamPlugin;
import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.commands.CommandHandler;
import arc.Core;
import arc.struct.Seq;

public class ChatfilterCommand extends CommandHandler<Object> {

	public static boolean chatFilter = Core.settings.getBool(AgzamPlugin.name() + "-chat-filter", false);
	
	{
		parms = "<on/off>";
		desc = "Включить/выключить фильтр чата";
	}

	@Override
	public void command(String[] argы, CommandSender sender, Object receiver, ReceiverType type) {
		if(require(argы.length == 0, sender, "[red]Недостаточно аргументов")) return;
		if(argы[0].equals("on")) {
			chatFilter = true;
			Core.settings.put(AgzamPlugin.name() + "-chat-filter", chatFilter);
			sender.sendMessage("[green]Чат фильтр включен");
		}else if(argы[0].equals("off")) {
			chatFilter = false;
			Core.settings.put(AgzamPlugin.name() + "-chat-filter", chatFilter);
			sender.sendMessage("[red]Чат фильтр выключен");
		}else {
			sender.sendMessage("Неверный аргумент, используйте [gold]on/off");
			return;
		}
	}
	
	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("on", "off");
		return super.complete(args, receiver, type);
	}
	
}
