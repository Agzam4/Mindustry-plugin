package agzam4.commands.server;

import agzam4.AgzamPlugin;
import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.Game;
import agzam4.commands.CommandHandler;
import agzam4.commands.Server;
import arc.Core;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.net.Administration.PlayerInfo;

public class ExtrastarCommand extends CommandHandler<Object> {

	{
		parms = "[add/remove] [uidd/name]";
		desc = "Сделайть рейтинг звезд особым";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(args.length == 0) {
			if(require(Server.extrastarUids.isEmpty(), sender, "[gold]Нет игроков")) return;
			StringBuilder sb = new StringBuilder("[gold]Игроки с дополнительными звездами:[white]");
			Server.extrastarUids.each(uidd -> sb.append("\n" + uidd + " (" + Vars.netServer.admins.getInfo(uidd).lastName + ")"));
			sender.sendMessage(sb.toString());
			return;
		}
		if(args.length == 2) {
			Player playert = Game.findPlayer(args[1]);
			if(playert != null) args[1] = playert.uuid();

			if(args[0].equalsIgnoreCase("add")) {
				if(require(Server.extrastarUids.contains(args[1]), sender, "[red]Игрок уже есть")) return;
				PlayerInfo info = Vars.netServer.admins.getInfo(args[1]);
				if(require(info == null, sender, "[red]Игрок не найден")) return;
				Server.extrastarUids.add(args[1]);
				save();
				sender.sendMessage("[gold]Игрок []" + info.lastName + " [gold]добавлен");
			} else if(args[0].equalsIgnoreCase("remove")) {
				if(require(!Server.extrastarUids.contains(args[1]), sender, "[red]UIDD не найден")) return;
				PlayerInfo info = Vars.netServer.admins.getInfo(args[1]);
				if(require(info == null, sender, "[red]Игрок не найден")) return;
				Server.extrastarUids.remove(args[1]);
				save();
				sender.sendMessage("[gold]Игрок []" + info.lastName + " [gold]убран");
			} else {
				sender.sendMessage("[red]Только add/remove");
			}
			return;
		}
		sender.sendMessage("[red]Неверные аргументы");
	}
	
	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("add", "remove");
		if(args.length == 1) return completePlayers();
		return super.complete(args, receiver, type);
	}

	private void save() {
		Core.settings.putJson(AgzamPlugin.name() + "-extrastar-uids", ObjectSet.class, Server.extrastarUids);
	}
}
