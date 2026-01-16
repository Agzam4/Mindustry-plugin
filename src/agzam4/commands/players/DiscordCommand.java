package agzam4.commands.players;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.commands.CommandHandler;
import agzam4.commands.server.SetdiscordCommand;
import mindustry.gen.Call;
import mindustry.gen.Player;

public class DiscordCommand extends CommandHandler<Player> {

	{
		desc = "\ue80d Сервера";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Player player, ReceiverType type) {
		if(SetdiscordCommand.discordLink == null) {
			sender.sendMessage("[red]\ue80d Ссылка отсутствует");
		} else {
			if(SetdiscordCommand.discordLink.isEmpty()) {
				sender.sendMessage("[red]\ue80d Ссылка отсутствует");
			} else {
				Call.openURI(player.con, SetdiscordCommand.discordLink);
			}
		}
	}

}
