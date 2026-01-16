package agzam4.commands.any;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.commands.CommandHandler;
import agzam4.votes.SkipmapVoteSession;
import mindustry.game.Team;
import mindustry.gen.Player;

public class SkipmapCommand extends CommandHandler<Object> {

	{
		desc = "Начать голосование за пропуск карты";
	}

	@Override
	public void command(String[] arg, ResultSender sender, Object reciver, ReceiverType type) {
		if(require(SkipmapVoteSession.current != null, sender, "[red]Голосование уже идет: [gold]/smvote <y/n>")) return;
		if(reciver instanceof Player player) {
			if(require(player.team() == Team.derelict, sender, "[red]Вы не можете использовать эту команду")) return;
			SkipmapVoteSession session = new SkipmapVoteSession();
			session.vote(player, 1);
			return;
		}
		new SkipmapVoteSession();
	}


}