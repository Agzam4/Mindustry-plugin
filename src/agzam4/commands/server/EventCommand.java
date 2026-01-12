package agzam4.commands.server;

import agzam4.CommandsManager.ResultSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.commands.CommandHandler;
import agzam4.events.ServerEvent;
import agzam4.events.ServerEventsManager;
import arc.struct.Seq;
import mindustry.gen.Iconc;

public class EventCommand extends CommandHandler<Object> {

	{
		parms = "[id] [on/off/force]";
		desc = "Включить/выключить событие";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		if(args.length == 0) {
			StringBuilder msg = new StringBuilder();
			for (int i = 0; i < ServerEventsManager.events.size; i++) {
				if(i != 0) msg.append('\n');
				ServerEvent event = ServerEventsManager.events.get(i);
				if(type == ReceiverType.player) {
					msg.append(event.isRunning() ? "[lime]" + Iconc.ok + " " : "[scarlet] " + Iconc.cancel + " ");
					msg.append(event.name);
					msg.append(' ');
				} else if(type == ReceiverType.bot) {
					msg.append(event.isRunning() ? "\u2714 " : "\u274c ");
					msg.append("<code>");
					msg.append(event.name);
					msg.append("</code>");
				} else {
					msg.append(event.isRunning() ? "V " : "X ");
					msg.append("");
					msg.append(event.name);
					msg.append("");
				}
			}
			sender.sendMessage(msg.toString());
			return;
		}
		if(args.length == 1) {
			ServerEvent event = ServerEventsManager.events.find(e -> args[0].equals(e.name));
			if(require(event == null, sender, "Событие не найдено, [blue]/event[] для списка событий")) return;
			sender.sendMessage("Событие " + event.name + "[white] имеет значение: " + event.isRunning());
			return;
		}
		if(args.length == 2) {
			boolean isOn = false;
			boolean isFast = false;
			if(args[1].equals("on")) {
				isOn = true;
			} else if(args[1].equals("off")) {
				isOn = false;
			} else if(args[1].equals("force")) {
				isOn = true;
				isFast = true;
			} else {
				sender.sendMessage("Неверный аргумент, используйте [gold]on/off[]");
				return;
			}
			ServerEvent event = ServerEventsManager.events.find(e -> args[0].equals(e.name));
			if(require(event == null, sender, "[red]Событие не найдено, [gold]/event [red] для списка событий")) return;

			boolean running = event.isRunning();
			boolean await = ServerEventsManager.activeEvents.contains(event);
			
			if(require(running && isOn, sender, "[red]Событие уже запущено")) return;
			if(require(await && isOn, sender, "[red]Событие начнется на следующей карте")) return;
			if(require(!await && !isOn, sender, "[red]Событие итак не запущено")) return;

			if(isOn) {
				if(isFast) {
					ServerEventsManager.fastRunEvent(event.name);
					sender.sendMessage("[white]Событие резко запущено!");
				} else {
					ServerEventsManager.runEvent(event.name);
					sender.sendMessage("[green]Событие запущено!");
				}
			} else {
				ServerEventsManager.stopEvent(event.name);
				sender.sendMessage("[red]Событие остановлено!");
			}

			return;
		}
			
	}

	
	@Override
	public Seq<String> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return ServerEventsManager.events.map(e -> e.name);
		if(args.length == 1) {
			ServerEvent event = ServerEventsManager.events.find(e -> args[0].equals(e.name));
			if(event != null) return event.isRunning() ? Seq.with("off") : Seq.with("on", "force");
		}
		return super.complete(args, receiver, type);
	}
}
