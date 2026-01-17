package agzam4.commands.any;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.admins.Admins;
import agzam4.bot.TUser.MessageData;
import agzam4.commands.CommandHandler;
import agzam4.commands.Server;
import agzam4.managers.Kicks;
import agzam4.votes.KickVoteSession;
import arc.util.Strings;
import mindustry.gen.Call;
import mindustry.gen.Player;

public class VoteCommand extends CommandHandler<Object> {

	{
		parms = "<y/n/c>";
		desc = "Проголосуйте, чтобы выгнать текущего игрока";
	}

	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(require(KickVoteSession.current == null, sender, "[red]Ни за кого не голосуют")) return;
        boolean permission = Admins.has(receiver, "votekick");
        
		if(permission && args[0].equalsIgnoreCase("c")){
			String name = 
					receiver instanceof Player p ? p.name : 
					receiver instanceof Server s ? s.name : 
					receiver instanceof MessageData c ? c.user.name : "<неизвестный>";
			Call.sendMessage(Strings.format("[lightgray]Голосование отменено администратором[orange] @[lightgray].", name));
			KickVoteSession.current.cancel();
			return;
		}
		
		if(!(receiver instanceof Player player)) {
			sender.sendMessage("Только игроки и пользователи с правами могут голосовать");
			return;
		}
		
		if(require(receiver instanceof Player p && p.isLocal(), sender, "[red]Локальные игроки не могут голосовать. Вместо этого кикните игрока сами")) return;

		int sign = switch(args[0].toLowerCase()){
		case "y", "yes" -> 1;
		case "n", "no" -> -1;
		default -> 0;
		};
		
		if(permission && sign > 0) {
    		Kicks.kick(KickVoteSession.current.kicker, KickVoteSession.current.target, KickVoteSession.current.reason);
			KickVoteSession.current.cancel();
    		return;
		}
		
		if(KickVoteSession.current.isPlayerVoted(player, sign)) {
			sender.sendMessage(Strings.format("[red]Вы уже проголосовали за @", args[0].toLowerCase()));
			return;
		}
		if(require(KickVoteSession.current.target == player, sender, "[red]Ты не можешь голосовать на за себя")) return;
		if(require(KickVoteSession.current.target.team() != player.team(), sender, "[red]Ты не можешь голосовать на за другие команды")) return;
		if(require(sign == 0, sender, "[red]Голосуйте либо \"y\" (да), либо \"n\" (нет)")) return;
		KickVoteSession.current.vote(player, sign);
    }

}
