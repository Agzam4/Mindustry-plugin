package agzam4.commands.server;

import java.util.Arrays;

import agzam4.CommandsManager;
import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.admins.Admins;
import agzam4.Game;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.gen.Player;

public class AsCommand extends CommandHandler<Object> {

	private static final String[] empty = new String[0];
	
	{
		parms = "<target> <command> [args...]";
		desc = "Выполнить команду от другой сущности игрока";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(require(args.length <= 1, sender, "Мало аргументов")) return;
		
		var player = Game.findPlayer(args[0]);
		if(require(player == null, sender, "Игрок не найден")) return;
		
		var command = CommandsManager.playerCommands().find(c -> c.text.equals(args[1]));
		if(require(command == null, sender, "Команда не найдена")) return;
		
		var handler = command.handler(sender);
		if(require(handler == null, sender, command.hasHandler() ? "Команда не доступна" : "Команда не поддерживается")) return;
		
		String[] subargs = args.length <= 2 ? empty : args[2].split(" ");
		handler.command(subargs, sender, player, type);
		sender.sendMessage("Завершено от сущности " + player.coloredName());
	}

	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		Log.info(Arrays.toString(args));
		if(args.length == 0) return completePlayers();
		if(args.length == 1) return CommandsManager.playerCommands().select(c -> Admins.has(receiver, c.text)).map(c -> c.text);
		var player = Game.findPlayer(args[0]);
		if(player == null) return null;
		var command = CommandsManager.playerCommands().find(c -> c.text.equals(args[1]));
		if(command == null) return null;
		var handler = command.handlerAny(receiver);
		if(handler == null) return null;
		
		String[] subargs = new String[args.length-2];
		for (int i = 2; i < args.length; i++) subargs[i-2] = args[i];
		return handler.complete(subargs, player, type);
	}
	
}
