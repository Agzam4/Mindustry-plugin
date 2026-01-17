package agzam4.commands.server;

import agzam4.Game;
import agzam4.PlayersData;
import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;
import mindustry.gen.Player;

public class SetcustomCommand extends CommandHandler<Object> {

	{
		parms = "<player> <join/leave> [сообщение...]";
		desc = "Установить сообщение подключения/отключения [lightgray]([coral]@name[] - для имени)";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(args.length == 0) return;
		Player p = Game.findPlayer(args[0]);
		if(require(p == null, sender, "[red]Игрок не найден")) return;

		boolean join = args[1].equalsIgnoreCase("join");
		boolean leave = args[1].equalsIgnoreCase("leave");
		if(require(!join && !leave, sender, "[red]Доступно только join/leave")) return;

		String message = args.length == 2 ? "" : args[2];
		boolean longname = receiver instanceof Player player ? Admins.has(player, "longname") : true;
		if(!longname && message.length() > 200) message = message.substring(0, 200);
		if(join) PlayersData.data(p.uuid()).connectMessage = message.isEmpty() ? null : message;
		if(leave) PlayersData.data(p.uuid()).disconnectedMessage = message.isEmpty() ? null : message;
		PlayersData.save();
		sender.sendMessage("[gold]Установлено " + args[0] + ": []" + message);
	}

	@Override
	public Seq<String> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return completePlayers();
		if(args.length == 1) return Seq.with("join", "leave");
		return Seq.with("@name");
	}
	
}
