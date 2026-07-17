package agzam4.commands.players;

import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.api.ApiServer;
import agzam4.api.auth.AuthTokens;
import agzam4.commands.CommandHandler;
import mindustry.gen.Call;
import mindustry.gen.Player;

public class AuthCommand extends CommandHandler<Player> {

	{
		desc = "Авторизация на сайте";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Player player, ReceiverType type) {
		if(player != null) Call.openURI(player.con, ApiServer.authUrl.string() + "/auth/" + AuthTokens.create(player.uuid(), player.usid()));
		sender.sendMessage("OK"); // TODO
	}

}
