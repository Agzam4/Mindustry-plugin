package agzam4.commands.admin;

import agzam4.Game;
import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import agzam4.net.NetMenu;
import arc.func.Cons2;
import arc.util.Strings;
import mindustry.content.StatusEffects;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import mindustry.net.Administration.Config;
import mindustry.type.StatusEffect;

public class MCommand extends CommandHandler<Player> {

	{
		desc = "Открыть меню";
	}

	@Override
	public void command(String[] args, ResultSender sender, Player admin, ReceiverType type) {
		 var players = new NetMenu("[white]" + Config.serverName.get().toString());

		 for (int i = 0; i < Groups.player.size(); i++) {
			 Player player = Groups.player.index(i);
			 if(admin == null) continue;
			 players.button(player.coloredName(), () -> {
				 var playerControl = new NetMenu(player.coloredName());
				 playerControl.build(() -> {
					 if(Admins.has(admin, "team")) {
						 for (var team : Team.baseTeams) {
							 playerControl.button(team.emoji.isEmpty() ? Strings.format("[#@]@", team.color.toString(), Iconc.logic) : team.emoji, () -> {
								 player.team(team);
							 });
						 }
						 playerControl.row();
					 }
					 playerControl.button("[green]\ue80f Вылечить", () -> {
						 if(player.unit() == null) return;
						 player.unit().heal();
					 });
					 playerControl.button("[red]\uue815 Уничтожить", () -> {
						 if(player.unit() == null) return;
						 player.unit().kill();
					 });
					 playerControl.row();

					 if(player.unit() != null) {
						 Cons2<StatusEffect, String> createEffect = (e,name) -> {
							 if(player.unit().hasEffect(e)) {
								 playerControl.button(Strings.format("[scarlet]@[] @", Iconc.cancel, name), () -> {
									 if(player.unit() == null) return;
									 player.unit().unapply(e);
								 });
								 return;
							 }
							 playerControl.button(Strings.format("[lime]@[] @", Iconc.add, name), () -> {
								 if(player.unit() == null) return;
								 player.unit().apply(e, Float.MAX_VALUE);
							 });
						 };
						 createEffect.get(StatusEffects.invincible, "Неуязвимость");
						 createEffect.get(StatusEffects.fast, "Скорость");
					 }

					 playerControl.row();
					 playerControl.button("[gold]\ue86d Сброс юнита", () -> {
						 if(player.unit() != null) {
							 Game.clearUnit(player);
						 }
					 });
				 });
				 playerControl.show(admin);
			 }).row();
		 }
		 players.show(admin);
	}
}
