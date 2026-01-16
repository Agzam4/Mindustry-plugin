package agzam4.commands.any;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import agzam4.votes.SkipmapVoteSession;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Player;

public class SmvoteCommand extends CommandHandler<Object> {

	{
		parms = "<y/n>";
		desc = "Проголосовать за/протов пропуск карты";
	}

	@Override
	public void command(String[] arg, ResultSender sender, Object reciver, ReceiverType type) {
		String voteSign = arg[0].toLowerCase();
		if(Admins.has(reciver, "smvote") && voteSign.equals("c")) {
			SkipmapVoteSession.current.cancel();
			return;
		}
		if(!(reciver instanceof Player player)) {
			sender.sendMessage("Только игроки и пользователи с правами могут голосовать");
			return;
		}
		
		if(require(player.team() == Team.derelict, sender, "[red]Вы не можете использовать эту команду")) return;
		if(require(SkipmapVoteSession.current == null, sender, "[red]Нет открытого голосования")) return;
		if(require(player.isLocal(), sender, "[red]Локальные игроки не могут голосовать")) return;
		int sign = 0;
		if(voteSign.equals("y")) sign = +1;
		if(voteSign.equals("n")) sign = -1;
		if(require(SkipmapVoteSession.current.isPlayerVoted(player, sign), sender, "[red]Ты уже проголосовал. Молчи!")) return;
		if(require(sign == 0, sender, "[red]Голосуйте либо \"y\" (да), либо \"n\" (нет)")) return;
		SkipmapVoteSession.current.vote(player, sign);
	}

	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) {
			Seq<String> list = Seq.with("y", "n");
			if(Admins.has(receiver, "smvote")) list.add("c");
			return list;
		}
		return super.complete(args, receiver, type);
	}

}
