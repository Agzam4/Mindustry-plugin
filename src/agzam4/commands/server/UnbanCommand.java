package agzam4.commands.server;

import java.util.concurrent.TimeUnit;

import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.admins.Admins;
import agzam4.api.auth.SensitiveData;
import agzam4.commands.CommandHandler;
import agzam4.commands.Permissions;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;

public class UnbanCommand extends CommandHandler<Object> {

	{
		parms = "<ip/uuid/id> <value/all>";
		desc = "Completely unban a person by IP or ID.";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(require(args.length == 0, sender, "[red]<ip/ID/all> is missed")) return;
		
		if(args[0].equalsIgnoreCase("ip")) {
			if(require(!Admins.has(sender, Permissions.sensitiveData), sender, "Нет прав")) return;
			
			if(args[1].equalsIgnoreCase("all")) {
				int size = Vars.netServer.admins.kickedIPs.size;
				int dos = Vars.netServer.admins.dosBlacklist.size;
				int bans = Vars.netServer.admins.dosBlacklist.size;
				Vars.netServer.admins.kickedIPs.clear();
				Vars.netServer.admins.dosBlacklist.clear();
				Vars.netServer.admins.bannedIPs.clear();
				sender.sendMessage(Strings.format("Unbanned: @ kicks, @ dos, @ bans", size, dos, bans));
				return;
			}
			boolean ip = Vars.netServer.admins.kickedIPs.remove(args[1]) != null;
			boolean dos = Vars.netServer.admins.dosBlacklist.remove(args[1]);
			boolean ban = Vars.netServer.admins.bannedIPs.remove(args[1]);
			if(require(!ip && !dos && !ban, sender, "ip not found")) return;

			sender.sendMessage(Strings.format("Cleared:@@", ip ? " kick" : "", dos ? " dos" : "", ban ? " ban" : ""));
			return;
		}
		if(args[0].equalsIgnoreCase("uuid")) {
			if(require(!Admins.has(sender, Permissions.sensitiveData), sender, "Нет прав")) return;
			unbanUuid(sender, args[1]);
			return;
		}

		if(args[0].equalsIgnoreCase("id")) {
			String uuid = SensitiveData.resolve(Strings.parseInt(args[1], 0));
			if(require(uuid == null, sender, "id not found")) return;
			unbanUuid(sender, uuid);
			var info = Vars.netServer.admins.playerInfo.get(uuid);
			if(info != null) {
				info.banned = false;
				info.lastKicked = Time.millis();
				Vars.netServer.admins.kickedIPs.remove(info.lastIP);
				Vars.netServer.admins.bannedIPs.remove(info.lastIP);
			}
			return;
		}

		sender.sendMessage(Strings.format("Unknow argument: \"@\"", args[0]));
		
	}
	
	private void unbanUuid(CommandSender sender, String uuid) {
		if(uuid.equalsIgnoreCase("all")) {
			int size = Vars.netServer.admins.kickedIPs.size;
			int dos = Vars.netServer.admins.dosBlacklist.size;
			Vars.netServer.admins.kickedIPs.clear();
			Vars.netServer.admins.dosBlacklist.clear();
			sender.sendMessage(Strings.format("Unbanned: @ kicks, @ dos", size, dos));
			Vars.netServer.admins.playerInfo.each((_uuid,info) -> {
				if(Time.millis() < info.lastKicked || info.banned) {
					info.banned = false;
					info.lastKicked = Time.millis();
					sender.sendMessage("Unbanned player: " + info.lastName);
				}
			});
			return;
		}
		PlayerInfo info = Vars.netServer.admins.playerInfo.get(uuid);
		if(require(info == null, sender, "uuid not found")) return;
		info.banned = false;
		if(Time.millis() < info.lastKicked) {
			info.lastKicked = Time.millis();
		}
		sender.sendMessage(Strings.format("Unbaned:@@", info.lastName));		
	}

	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		boolean sd = Admins.has(receiver, Permissions.sensitiveData);
		if(args.length == 0) return sd ? Seq.with("id") : Seq.with("ip", "uuid", "id");
		if(args.length == 1) {
			if(sd && args[0].equalsIgnoreCase("ip")) return bannedIp();
			if(sd && args[0].equalsIgnoreCase("uuid")) return bannedUuid();
		}
		return super.complete(args, receiver, type);
	}
	
	private Seq<String> bannedIp() {
		Seq<String> bans = new Seq<String>();
		Vars.netServer.admins.dosBlacklist.each(s -> bans.add(s + " dos"));
		Vars.netServer.admins.bannedIPs.each(ip -> bans.add(ip + " ban"));
		Seq<String> cleanup = new Seq<String>();
		Vars.netServer.admins.kickedIPs.each((ip, time) -> {
			if(Time.millis() < time) bans.add(ip + " (" + TimeUnit.MILLISECONDS.toMinutes(time - Time.millis()) + " minutes)");
			else cleanup.add(ip);
		});
		if(cleanup.size > 0) {
			cleanup.each(ip -> Vars.netServer.admins.kickedIPs.remove(ip));
			cleanup.clear();
		}
		return bans;
	}
	
	private Seq<String> bannedUuid() {
		Seq<String> bans = new Seq<String>();

		Vars.netServer.admins.playerInfo.each((key, info) -> {
			if(info == null) return;
			if(info.banned) {
				bans.add(info.id + " " + info.lastName);
				return;
			}
			if(Time.millis() < info.lastKicked) {
				bans.add(info.id + " " + info.lastName + "(" + TimeUnit.MILLISECONDS.toMinutes(info.lastKicked - Time.millis()) + " minutes)");
			}
		});
		return bans;
	}
	
}
