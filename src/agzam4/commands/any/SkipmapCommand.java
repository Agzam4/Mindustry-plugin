package agzam4.commands.any;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.commands.CommandHandler;
import agzam4.managers.Players;
import agzam4.votes.SkipmapVoteSession;
import agzam4gen.config.MapsConfig;
import agzam4gen.config.ModerationConfig;
import mindustry.game.Team;
import mindustry.gen.Player;

public class SkipmapCommand extends CommandHandler<Object> {

	{
		desc = "Начать голосование за пропуск карты";
	}

	@Override
	public void command(String[] arg, CommandSender sender, Object reciver, ReceiverType type) {
		if(require(SkipmapVoteSession.current != null, sender, "[red]Голосование уже идет: [gold]/smvote <y/n>")) return;
		if(reciver instanceof Player player) {
			if(require(player.team() == Team.derelict, sender, "[red]Вы не можете использовать эту команду")) return;
			
	        boolean permission = sender.hasPermissions("skipmap");
	        if(require(!permission && Players.mapPlaytime(player) < MapsConfig.skipmapRequiredMapPlaytime, sender,"[red]Вам запрещено голосовать")) return;
	        if(require(!permission && Players.gamePlaytime(player) < MapsConfig.skipmapRequiredTotalPlaytime, sender,"[red]Вам запрещено голосовать")) return;
	        
			SkipmapVoteSession session = new SkipmapVoteSession();
			session.vote(player, 1);
			return;
		}
		new SkipmapVoteSession();
	}


}