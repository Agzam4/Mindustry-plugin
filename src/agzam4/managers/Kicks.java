package agzam4.managers;

import agzam4.CommandsManager;
import agzam4.Images;
import agzam4.bot.Bots;
import agzam4.bot.TelegramBot;
import agzam4.bot.Bots.NotifyTag;
import arc.Events;
import arc.struct.ObjectMap;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType.GameOverEvent;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.net.NetConnection;
import mindustry.net.Administration.Config;

public class Kicks {

	/**
	 * Clears after game over
	 */
	private static ObjectMap<String, Integer> lastkickTime = new ObjectMap<>();
	
	public static void init() {
		Events.on(GameOverEvent.class, e -> {
			lastkickTime.clear();
		});
	}

	public static void kick(Player kicker, Player target, String reason) {
		kick(kicker.coloredName(), target, reason);
	}

	public static void kick(String kicker, Player target, String reason) {
		int minutes = 5;
		Integer last = lastkickTime.get(target.uuid());
		if(last != null) minutes = last;
		minutes = Math.min(minutes, 60);
		lastkickTime.put(target.uuid(), minutes*2);
		Bots.notify(NotifyTag.votekick, Strings.format("Выдан бан на <b>@</b> минут\nПричина: <i>@</i>\nБан выдал: <i>@</i>", minutes, TelegramBot.strip(reason), TelegramBot.strip(kicker)));
        Bots.notify(NotifyTag.votekick, Images.screenshot(target));
		
        kick(kicker, target, reason, minutes * 60);
		Call.sendMessage(Strings.format("[white]Игрок [orange]@[white] забанен на [orange]@[] минут [lightgray](причина: @)", target.plainName(), minutes, reason));
	}

	public static void sendDiscord(NetConnection con) {
		if(CommandsManager.discordLink != null && !CommandsManager.discordLink.isEmpty()) Call.openURI(con, CommandsManager.discordLink);
	}

	public static void kick(Player kicker, Player target, String reason, long seconds) {
		kick(kicker.coloredName(), target, reason, seconds);
	}

	public static void kick(String kicker, Player target, String reason, long seconds) {
		sendDiscord(target.con);
		target.kick(Strings.format("Вы были забанены на [red]@[] минут\nПричина: [orange]@[white]\nБан выдал: [orange]@[white]\nОбжаловать: @\n[white]Сервер: @", 
				seconds/60, reason, kicker, CommandsManager.discordLink, Config.serverName.get()), seconds * 1000);
		Vars.netServer.admins.handleKicked(target.uuid(), target.ip(), seconds * 1000);		
	}
	
}
