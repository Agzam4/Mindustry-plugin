package agzam4.commands.server;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;

public class SandboxCommand extends CommandHandler<Object> {
	
	{
		parms = "[on/off] [team]";
		desc = "Бесконечные ресурсы";
	}

	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		if(require(args.length == 0, sender, "enabled: " + Vars.state.rules.infiniteResources)) return;
		Team team = null;
		if(args.length == 2) {
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
		}
		if(args[0].equals("on")) {
			if(team == null) {
				Vars.state.rules.infiniteResources = true;
				sender.sendMessage("Включено!");
			} else {
				team.rules().infiniteResources = true;
				sender.sendMessage("Включено для команды [#" + team.color + "]" + team.name);
			}
			if(receiver instanceof Player p) Call.setRules(p.con, Vars.state.rules);
			else Call.setRules(Vars.state.rules);
		}else if(args[0].equals("off")) {
			if(team == null) {
				Vars.state.rules.infiniteResources = false;
				sender.sendMessage("Выключено!");
			} else {
				team.rules().infiniteResources = false;
				sender.sendMessage("Выключено для команды [#" + team.color + "]" + team.name);
			}
			if(receiver instanceof Player p) Call.setRules(p.con, Vars.state.rules);
			else Call.setRules(Vars.state.rules);
		} else {
			sender.sendMessage("Только on/off");
		}
	}
	
	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("on", "off");
		if(args.length == 1) return completeTeams();
		return super.complete(args, receiver, type);
	}

}
