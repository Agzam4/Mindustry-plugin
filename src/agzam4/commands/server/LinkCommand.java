package agzam4.commands.server;

import agzam4.CommandsManager.ResultSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.Game;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class LinkCommand extends CommandHandler<Object> {

	{
		parms = "<link> [player]";
		desc = "Отправить ссылку всем/игроку";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		if(args.length == 1) {
			Call.openURI(args[0]);
		} else if(args.length == 2) {
			Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(args[1])));
			if(targetPlayer != null) {
				Call.openURI(targetPlayer.con, args[0]);
				sender.sendMessage("[gold]Готово!");
			} else {
				sender.sendMessage("[red]Игрок не найден");
			}
		}
	}

	@Override
	public Seq<String> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 1) return Game.playersNames();
		return super.complete(args, receiver, type);
	}
}
