package agzam4.commands.admin;

import agzam4.CommandsManager.ReceiverType;
import agzam4.admins.Admins;
import agzam4.CommandsManager.CommandSender;
import agzam4.bans.BanGroup;
import agzam4.bans.Bans;
import agzam4.commands.CommandHandler;
import agzam4.commands.Permissions;
import agzam4.net.NetMenu;
import agzam4.utils.Log;
import mindustry.gen.Groups;
import mindustry.gen.Iconc;
import mindustry.gen.Player;

public class MbCommand extends CommandHandler<Player> {

	{
		desc = "Массовый бан";
	}

	@Override
	public void command(String[] args, CommandSender sender, Player admin, ReceiverType type) {
		var types = new NetMenu("Массовый бан");
		types.button("Причина: [gray]бот", () -> show(admin, "бот", Bans.bots));
		types.show(admin);
	}

	private void show(Player admin, String reason, BanGroup list) {
		if(admin == null) return;
		var players = new NetMenu(reason);
		players.button(Iconc.refresh + " Обновить", () -> show(admin, reason, list)).row();

		for (int i = 0; i < Groups.player.size(); i++) {
			Player player = Groups.player.index(i);
			if(player == admin) continue;
			if(player.admin) continue;
			if(Admins.has(player, "votekick")) continue;
			if(Admins.has(player, Permissions.whitelist)) continue;

			players.button(player.coloredName(), () -> {
				try {
					// Do not use IPs from PlayerInfo to prevent bots with 
					// stolen UUIDs from getting the original player's IP banned
					list.banBy(admin, player.ip(), reason); 
				} catch (Exception e) {
					Log.err(e);
				}
				show(admin, reason, list);
			}).row();
		}
		players.show(admin);
	}

}
