package agzam4.commands.server;

import java.util.Arrays;

import agzam4.CommandsManager;
import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.admins.Admins;
import agzam4.Game;
import agzam4.commands.CommandHandler;
import agzam4.managers.Players;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Groups;

public class MuteCommand extends CommandHandler<Object> {

	private static final String[] empty = new String[0];
	
	{
		parms = "<add/remove/list/clear> [target]";
		desc = "Замутить/разметить игрока";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(require(args.length <= 1, sender, "Мало аргументов")) return;

		if(args[0].equalsIgnoreCase("list")) {
			StringBuilder builder = new StringBuilder();
			builder.append("Players:");
			Players.muted.each((p) -> {
				var info = Vars.netServer.admins.playerInfo.get(p);
				if(info == null) builder.append("\nunknow");
				else builder.append("\n").append(info.lastName);
			});
			sender.sendMessage(builder.toString());
			return;
		}
		if(args[0].equalsIgnoreCase("clear")) {
			int size = Players.muted.size;
			Players.muted.clear();
			sender.sendMessage("[stat]Unmuted:[] " + size);
			return;
		}


		int act = 0;
		if(args[0].equalsIgnoreCase("add")) act++;
		if(args[0].equalsIgnoreCase("remove")) act--;

		if(require(act == 0, sender, "Неверный аргумент")) return;
		if(require(args.length <= 2, sender, "Мало аргументов")) return;
		
		var player = Game.findPlayer(args[1]);

		if(act == 1) {
			if(require(!Players.mute(player), sender, "Already muted")) return;
			sender.sendMessage("Muted!");
			return;
		}
		if(require(!Players.unmute(player), sender, "Already unmuted")) return;
		sender.sendMessage("Unmuted!");
		return;
	}

	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("add", "remove", "list", "clear");
		if(args.length == 1 && (args[0].equalsIgnoreCase("add")) || args[0].equalsIgnoreCase("remove")) return completePlayers();
		return null;
	}
	
}
