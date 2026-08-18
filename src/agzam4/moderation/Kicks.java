package agzam4.moderation;

import agzam4.Images;
import agzam4.bot.Bots;
import agzam4.bot.TelegramBot;
import agzam4.logs.Logs;
import agzam4gen.config.ModerationConfig;
import agzam4gen.config.ServerConfig;
import agzam4.logs.LogEvents.KickLogEvent;
import agzam4.server.Server;
import agzam4.bot.Bots.NotifyTag;
import arc.Events;
import arc.struct.ObjectMap;
import arc.util.Nullable;
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

	public static void kickByName(String kicker, Player target, String reason) {
		kick(null, kicker, target, reason);
	}
	
	public static void kick(Player kicker, Player target, String reason) {
		kick(kicker, kicker.coloredName(), target, reason);
	}

	private static void kick(@Nullable Player kicker, String kickerName, Player target, String reason) {
		int minutes = ModerationConfig.startBanTime;
		Integer last = lastkickTime.get(target.uuid());
		if(last != null) minutes = last;
		minutes = Math.min(minutes, ModerationConfig.maxBanTime);
		lastkickTime.put(target.uuid(), (int)(minutes*ModerationConfig.banTimeMultiplier));
		Bots.notify(NotifyTag.votekick, Strings.format("Выдан бан на <b>@</b> минут\nПричина: <i>@</i>\nБан выдал: <i>@</i>", minutes, TelegramBot.strip(reason), TelegramBot.strip(kickerName)));
        Bots.notify(NotifyTag.votekick, Images.screenshot(target));
		
        kick(kicker, kickerName, target, reason, minutes * 60);
		Call.sendMessage(Strings.format("[white]Игрок [orange]@[white] забанен на [orange]@[] минут [lightgray](причина: @)", target.plainName(), minutes, reason));
	}

	public static void kick(Player kicker, Player target, String reason, long seconds) {
		kick(kicker, kicker.coloredName(), target, reason, seconds);
	}

	public static void kick(String kicker, Player target, String reason, long seconds) {
		kick(null, kicker, target, reason, seconds);
	}

	private static void kick(@Nullable Player kicker, String kickerName, Player target, String reason, long seconds) {
		sendDiscord(target.con);
		target.kick(Strings.format("Вы были забанены на [red]@[] минут\nПричина: [orange]@[white]\nБан выдал: [orange]@[white]\nОбжаловать: @\n[white]Сервер: @", 
				seconds/60, reason, kickerName, ServerConfig.discordLink, Config.serverName.get()), seconds * 1000);
		
		if(kicker != null) {
			Logs.event(new KickLogEvent(kicker, target, reason, seconds));
		}
//		Logs.notify(NotifyTag.votekick, kicker, Strs.format("Бан выдан: `$`\nБан выдал: `$`\nПричина: `$`\nВремя: $", target.uuid(),  kicker == null ? kickerName : kicker.uuid(), reason));
	
		Vars.netServer.admins.handleKicked(target.uuid(), target.ip(), seconds * 1000);		
	}

	public static void sendDiscord(NetConnection con) {
		if(ServerConfig.discordLink != null && !ServerConfig.discordLink.isEmpty()) Call.openURI(con, ServerConfig.discordLink);
	}
	
}
