package agzam4.commands.server;

import java.util.concurrent.TimeUnit;

import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;

public class UnbanCommand extends CommandHandler<Object> {

	{
		parms = "<dos/ip/ID/all>";
		desc = "Completely unban a person by IP or ID.";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(require(args.length == 0, sender, "[red]<ip/ID/all> is missed")) return;
		if(args[0].equalsIgnoreCase("all")) {
			sender.sendMessage("Unbanned players [ID]:");
			Vars.netServer.admins.playerInfo.each((key, info) -> {
				if(info == null) return;
				info.banned = false;
				if(Time.millis() < info.lastKicked) {
					info.lastKicked = Time.millis();
				}
			});
			sender.sendMessage("Unbanned all IPs");
			Vars.netServer.admins.kickedIPs.clear();
			Vars.netServer.admins.dosBlacklist.clear();
			return;
		}
		if(Vars.netServer.admins.dosBlacklist.remove(args[0])) {
			sender.sendMessage("Unbanned dos player: " + args[0]);
			return;
		}
		PlayerInfo info = Vars.netServer.admins.playerInfo.get(args[0]);
		Long ip = Vars.netServer.admins.kickedIPs.remove(args[0]);
		
		if(require(info == null && ip == null, sender, "player/ip not found")) return;
		if(info != null) {
			info.banned = false;
			if(Time.millis() < info.lastKicked) {
				info.lastKicked = Time.millis();
			}
			sender.sendMessage("Unbanned player: " + info.lastName);
		}
		if(ip != null) sender.sendMessage("Unbanned IP: " + args[0]);
	}
	
	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return banned();
		return super.complete(args, receiver, type);
	}
	
	private Seq<String> banned() {
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
