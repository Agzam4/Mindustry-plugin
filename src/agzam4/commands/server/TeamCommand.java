package agzam4.commands.server;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.commands.CommandHandler;
import agzam4.utils.Log;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.content.StatusEffects;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class TeamCommand extends CommandHandler<Object> {

	{
		parms = "[player] [team]";
		desc = "Установить команду для игрока";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		if(args.length < 1) {
			StringBuilder teams = new StringBuilder();
			for (int i = 0; i < Team.baseTeams.length; i++) {
				teams.append(Team.baseTeams[i].name);
				teams.append(", ");
			}
			for (int i = 0; i < Team.all.length; i++) {
				teams.append(Team.all[i].name);
				if(i != Team.all.length - 1) teams.append(", ");
			}
			sender.sendMessage("Команды:\n" + teams.toString());
		}
		if(args.length == 1) {
			Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(args[0])));
			if(targetPlayer == null) {
				sender.sendMessage("[red]Игрок не найден");
				return;
			}
			sender.sendMessage("Игрок состоить в команде: " +  targetPlayer.team().name);
			return;
		}
		if(args.length == 2) {
			Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(args[0])));
			if(targetPlayer == null) {
				sender.sendMessage("[red]Игрок не найден");
				return;
			}
			sender.sendMessage("Игрок состоить в команде: " +  targetPlayer.team().name);

			Team team = null;
			String targetTeam = args[1].toLowerCase();
			for (int i = 0; i < Team.baseTeams.length; i++) {
				if(Team.baseTeams[i].name.equals(targetTeam.toLowerCase())) {
					team = Team.baseTeams[i];
				}
			}
			for (int i = 0; i < Team.all.length; i++) {
				if(Team.all[i].name.equals(targetTeam.toLowerCase())) {
					team = Team.all[i];
				}
			}
			if(team == null) {
				sender.sendMessage("[red]Команда не найдена");
			} else {
				targetPlayer.team(team);
				if(team.name.equals(Team.crux.name)) {
					Log.info("crux");
					targetPlayer.unit().healTime(.01f);
					targetPlayer.unit().healthMultiplier(100);
					targetPlayer.unit().maxHealth(1000f);
					targetPlayer.unit().apply(StatusEffects.invincible, Float.MAX_VALUE);
				}
				sender.sendMessage("Игрок " + targetPlayer.name() + " отправлен в команду [#" + team.color + "]" + team.name);
				targetPlayer.sendMessage("Вы отправлены в команду [#" + team.color + "]" + team.name);
			}
			return;
		}
	}
	
	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return completePlayers();
		if(args.length == 1) return completeTeams();
		return super.complete(args, receiver, type);
	}


}
