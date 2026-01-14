package agzam4.commands.server;

import agzam4.Game;
import agzam4.PlayersData;
import agzam4.CommandsManager.ResultSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import agzam4.commands.Permissions;
import arc.struct.Seq;
import mindustry.gen.Player;

public class SetnickCommand extends CommandHandler<Object> {

	{
		parms = "<player> [ник...]";
		desc = "Установить никнейм на сервере";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		if(args.length == 0) return;
		Player p = Game.findPlayer(args[0]);
		if(require(p == null, sender, "[red]Игрок не найден")) return;

		String name = args.length == 1 ? "" : args[1];
		name = name.replaceAll(" ", "_");
		boolean longname = receiver instanceof Player player ? Admins.has(player, Permissions.longname) : true;
		if(!longname && name.length() > 100) name = name.substring(0, 100);
		PlayersData.data(p.uuid()).name = name.isEmpty() ? null : name;
		PlayersData.save();
		sender.sendMessage("[gold]Установлено имя: []" + p.coloredName() + " [gray]->[] " + name);
		if(!name.isEmpty()) p.name(name);
	}

	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return completePlayers();
		return super.complete(args, receiver, type);
	}
}
