package agzam4.bans;

import java.util.concurrent.TimeUnit;

import agzam4.database.DBFields.DEFAULT;
import agzam4.database.DBFields.FIELD;
import agzam4.database.DBFields.PRIMARY_KEY;
import agzam4.database.Databases;
import agzam4.database.Entity;
import agzam4.database.Table;
import agzam4proc.lib.PVars;
import arc.Events;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.gen.Player;
import mindustry.net.Administration.PlayerInfo;

public class Bans {

	public static final Fi dir = PVars.dataDirectory.child("iplists");
	
	public static BanGroup bots = new BanGroup(dir.child("bots.txt"));
	
	public static Table<BanEntity> table;

	public static void init() {
		dir.mkdirs();
		bots.load();
		table = Databases.mainDatabase.createTable("bans", BanEntity.class);
	}
	
	/**
	 * @param key - IP or UUID
	 */
	public static @Nullable BanEntity getEntity(String key) {
		var e = table.get(key);
		if(e == null) return null;
		if(e.escalation < Time.millis() && e.ban < Time.millis()) {
			table.delete(e);
			return null;
		}
		return e;
	}
	

	public static void putEntity(Player player, int banMinutes, int escalationMinutes, int level) {
		var e = new BanEntity();
		e.ban = Time.millis() + TimeUnit.MINUTES.toMillis(banMinutes);
		e.escalation = Time.millis() + TimeUnit.MINUTES.toMillis(escalationMinutes);
		e.escalationLevel = level;
		
		e.key = player.uuid();
		table.put(e);
		e.key = player.ip();
		table.put(e);
	}
	
	public static class BanEntity extends Entity {
		
		public @FIELD @PRIMARY_KEY String key;
		public @FIELD @DEFAULT(value="0") Long ban = 0L; // end of ban
		public @FIELD @DEFAULT(value="0") Long escalation = 0L; // and of escalation
		public @FIELD @DEFAULT(value="0") Integer escalationLevel = 0;
		
	}
	
	public static int escalationLevel(Player player) {
		if(player == null) return 0;
		int level = 0;
		{
			var e = getEntity(player.uuid());
			if(e != null && e.escalation >= Time.millis()) level = Math.max(level, e.escalationLevel);
		}
		{
			var e = getEntity(player.ip());
			if(e != null && e.escalation >= Time.millis()) level = Math.max(level, e.escalationLevel);
		}
		return level;
	}

	public static int escalationLevel(String... keys) {
		int level = 0;
		for (int i = 0; i < keys.length; i++) {
			var e = getEntity(keys[i]);
			if(e != null && e.escalation >= Time.millis()) level = Math.max(level, e.escalationLevel);
		}
		return level;
	}

	public static long escalation(String... keys) {
		long end = 0;
		for (int i = 0; i < keys.length; i++) {
			var e = getEntity(keys[i]);
			if(e != null && e.escalation >= Time.millis()) end = Math.max(end, e.escalation);
		}
		return end;
	}

	public static void unbanIp(String ip) {
		Vars.netServer.admins.kickedIPs.remove(ip);
		Vars.netServer.admins.bannedIPs.remove(ip);

		var e = new BanEntity();
		e.key = ip;
		table.delete(e);
	}

	public static void unbanInfo(PlayerInfo info) {
		info.banned = false;
		info.lastKicked = Time.millis();
		Bans.unbanIp(info.lastIP);
		var e = new BanEntity();
		e.key = info.id;
		table.delete(e);
	}
	
}
