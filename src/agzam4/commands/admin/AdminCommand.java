package agzam4.commands.admin;

import agzam4.Game;
import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration.PlayerInfo;

public class AdminCommand extends CommandHandler<Player> {

	{
		parms = "<add/remove> <name>";
		desc = "Добавить/удалить админа";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Player player, ReceiverType type) {
		if(require(args.length != 2 || !(args[0].equals("add") || args[0].equals("remove")), sender, "[red]Second parameter must be either 'add' or 'remove'.")) return;
		boolean add = args[0].equals("add");
		PlayerInfo target;
		Player playert = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(args[1])));
		if(playert != null) {
			target = playert.getInfo();
		} else {
			target = Vars.netServer.admins.getInfoOptional(args[1]);
			playert = Groups.player.find(p -> p.getInfo() == target);
		}
		if(require(!add && playert.uuid() == player.uuid(), sender, "[red] Вы не можете снять свой статус")) return;
		if(target != null){
			if(add) Vars.netServer.admins.adminPlayer(target.id, playert == null ? target.adminUsid : playert.usid());
			else Vars.netServer.admins.unAdminPlayer(target.id);
			if(playert != null) playert.admin(add);
			sender.sendMessage("[gold]Изменен статус администратора игрока: [" + Game.colorToHex(playert.color) + "]" + Strings.stripColors(target.lastName));
		} else {
			sender.sendMessage("[red]Игрока с таким именем или ID найти не удалось. При добавлении администратора по имени убедитесь, что он подключен к Сети; в противном случае используйте его UUID");
		}
		Vars.netServer.admins.save();
	}

	@Override
	public Seq<String> complete(String[] args, Player receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("add", "remove");
		if(args.length == 1) return Game.playersNames();
		return super.complete(args, receiver, type);
	}
	
}
