package agzam4.commands.server;

import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.Game;
import agzam4.commands.CommandHandler;
import agzam4.managers.Players;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.gen.Player;

public class StatCommand extends CommandHandler<Object> {

	{
		desc = "Статы игрока";
		parms = "<player>";
	}

	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(require(args.length != 1, sender, "Неверные аргументы")) return;
		Player player = Game.findPlayer(args[0]);
		if(require(player == null, sender, "Игрок не найден")) return;
		
		sender.sendMessage(Strings.format("Время на карте: @ min\nВремя на сервере: @ min", Players.mapPlaytime(player), Players.gamePlaytime(player)));
		
	}


	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return completePlayers();
		return super.complete(args, receiver, type);
	}
}