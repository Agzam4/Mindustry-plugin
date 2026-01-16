package agzam4.commands.admin;

import agzam4.PlayersData;
import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import agzam4.commands.Permissions;
import mindustry.gen.Player;

public class NickCommand extends CommandHandler<Player> {

	{
		parms = "[ник...]";
		desc = "Установить никнейм на сервере";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Player player, ReceiverType type) {
		String name = args.length == 0 ? "" : args[0];
		name = name.replaceAll(" ", "_");
		if(!Admins.has(player, Permissions.longname)) {
			if(name.length() > 100) name = name.substring(0, 100);
		}
		PlayersData.data(player.uuid()).name = name.isEmpty() ? null : name;
		PlayersData.save();
		if(!name.isEmpty()) player.name(name);
		player.sendMessage("[gold]Установлено имя: []" + player.coloredName());
	}

}
