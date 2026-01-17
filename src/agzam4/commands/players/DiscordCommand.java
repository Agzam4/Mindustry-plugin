package agzam4.commands.players;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.commands.CommandHandler;
import agzam4.commands.Server;
import mindustry.gen.Call;
import mindustry.gen.Player;

public class DiscordCommand extends CommandHandler<Player> {

	{
		desc = "\ue80d Сервера";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Player player, ReceiverType type) {
		if(Server.discordLink == null) {
			sender.sendMessage("[red]\ue80d Ссылка отсутствует");
		} else {
			if(Server.discordLink.isEmpty()) {
				sender.sendMessage("[red]\ue80d Ссылка отсутствует");
			} else {
				Call.openURI(player.con, Server.discordLink);
			}
		}
	}

}
