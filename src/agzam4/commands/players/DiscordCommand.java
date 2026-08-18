package agzam4.commands.players;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.commands.CommandHandler;
import agzam4gen.config.ServerConfig;
import mindustry.gen.Call;
import mindustry.gen.Player;

public class DiscordCommand extends CommandHandler<Player> {

	{
		desc = "\ue80d Сервера";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Player player, ReceiverType type) {
		if(require(ServerConfig.discordLink == null || ServerConfig.discordLink.isEmpty(), sender, "[red]\ue80d Ссылка отсутствует")) return;
		Call.openURI(player.con, ServerConfig.discordLink);
	}

}
