package agzam4.commands.server;

import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.bans.Bans;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;
import arc.util.Strings;

public class Banlist extends CommandHandler<Object> {

	// TODO: IP list management
	{
		parms = "<list> <reload>";
		desc = "Reloading list from disk";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(require(args.length == 0, sender, "[red]<list> is missed")) return;
		if(args[0].equalsIgnoreCase("bots")) {
			if(args[1].equalsIgnoreCase("add")) {
				long size = Bans.bots.list.size();
				Bans.bots.list.addSubnet(args[1]);
				sender.sendMessage(Strings.format("Added [accent]@[] new IPs", Bans.bots.list.size() - size));
				return;
			}
			if(args[1].equalsIgnoreCase("remove")) {
				long size = Bans.bots.list.size();
				Bans.bots.list.removeSubnet(args[1]);
				sender.sendMessage(Strings.format("Removed [accent]@[] IPs", size, Bans.bots.list.size()));
				return;
			}
			if(args[1].equalsIgnoreCase("reload")) {
				Bans.bots.load();
				sender.sendMessage(Strings.format("Loaded [accent]@[] IPs!", Bans.bots.list.size()));
				return;
			}
			sender.sendMessage("[red]arguments is missed");
			return;
		}
		sender.sendMessage(Strings.format("List not found: \"@\"", args[0]));
	}
	
	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("bots");
		if(args.length == 1) return Seq.with("reload", "add", "remove");
		return super.complete(args, receiver, type);
	}
	
}
