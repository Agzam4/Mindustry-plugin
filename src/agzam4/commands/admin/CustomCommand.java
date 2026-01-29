package agzam4.commands.admin;

import agzam4.PlayersData;
import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.commands.CommandHandler;
import agzam4.commands.Permissions;
import arc.struct.Seq;
import mindustry.gen.Player;

public class CustomCommand extends CommandHandler<Player> {

	{
		parms = "<join/leave> [сообщение...]";
		desc = "Установить сообщение подключения/отключения [lightgray]([coral]@name[] - для имени)";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Player player, ReceiverType type) {
		if(args.length == 0) return;
		boolean join = args[0].equalsIgnoreCase("join");
		boolean leave = args[0].equalsIgnoreCase("leave");
		if(require(!join && !leave, sender, "[red]Доступно только join/leave")) return;
		
		String message = args.length == 1 ? "" : args[1];
		if(!sender.hasPermissions(Permissions.longname)) {
			if(message.length() > 200) message = message.substring(0, 200);
		}
		if(join) PlayersData.data(player.uuid()).connectMessage = message.isEmpty() ? null : message;
		if(leave) PlayersData.data(player.uuid()).disconnectedMessage = message.isEmpty() ? null : message;
		PlayersData.save();
		sender.sendMessage("[gold]Установлено " + args[0] + ": []" + message);
	}
	
	@Override
	public Seq<?> complete(String[] args, Player receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("join", "leave");
		if(args.length == 1) return Seq.with("@name");
		return super.complete(args, receiver, type);
	}

}
