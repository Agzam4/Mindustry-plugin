package agzam4.commands.server;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.commands.CommandHandler;
import arc.util.Time;
import mindustry.Vars;

public class BansCommand extends CommandHandler<Object> {

	{
		desc = "Список банов";
	}

	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		sender.sendMessage("Banned players [ID]:");
		Vars.netServer.admins.playerInfo.each((key, info) -> {
			if(info == null) return;
			if(info.banned) {
				sender.sendMessage("> " + key + " - banned");
			}
			if(Time.millis() < info.lastKicked) {
				sender.sendMessage("> " + key + " - kicked [lightgray](" + (info.lastKicked - Time.millis())/1000/60 + " minutes)");
			}
		});
		sender.sendMessage("Banned players [IP]:");
		Vars.netServer.admins.kickedIPs.each((key, time) -> {
			if(Time.millis() < time) {
				sender.sendMessage("> " + key + " - kicked [lightgray](" + (time - Time.millis())/1000/60 + " minutes)");
			}
		});
		sender.sendMessage("Dos players [IP]:");
		Vars.netServer.admins.dosBlacklist.each((value) -> {
			sender.sendMessage("> " + value + " - banned");
		});
	}


}
