package agzam4.commands.server;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.Game;
import agzam4.commands.CommandHandler;
import arc.func.Cons;
import arc.func.Cons2;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;

public class InfoCommand extends CommandHandler<Object> {

	{
		parms = "<uuid/ip/name> [joinfilter] [namecheck]";
		desc = "Пробивает инофрмацию о игроке";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(require(args.length < 1, sender, "wrong arguments")) return;
		
		
		final int limit = args.length > 1 ? Strings.parseInt(args[1], 1) : 1;
		if(require(limit < 1, sender, "joinfilter должен быть больше 1")) return;
		
		boolean useNames = args.length > 2 ? args[1].equalsIgnoreCase("namecheck") : false;

		ObjectSet<String> ips = ObjectSet.with(), 
				uuids = ObjectSet.with(), 
				names = ObjectSet.with(), 
				single = ObjectSet.with();
		
		ObjectSet<PlayerInfo> infos = ObjectSet.with();
		
		Cons<PlayerInfo> add = info -> {
			infos.add(info);
			uuids.add(info.id);
			names.addAll(info.names);
			ips.addAll(info.ips);
			if(info.timesJoined <= limit) single.add(info.id);
		};
		
		Queue<String> uuidQueue = new Queue<>();
		Queue<String> ipQueue = new Queue<>();
		Queue<String> namesQueue = new Queue<>();
		
		if(!useNames) {
			var player = Game.findPlayer(args[0]);
			if(player != null) uuidQueue.add(player.uuid());
		}
		
		uuidQueue.add(args[0]);
		ipQueue.add(args[0]);
		namesQueue.add(args[0]);
		
		while (!ipQueue.isEmpty() || !uuidQueue.isEmpty()) {
			// UUID -> IP,[name]
			while (!uuidQueue.isEmpty()) {
				var uuid = uuidQueue.removeFirst();
				var info = Vars.netServer.admins.playerInfo.get(uuid);
				if(info == null) continue;

				add.get(info);
				
				info.ips.each(i -> {
					if(!ips.contains(i)) ipQueue.add(i);
				});
				if(useNames) {
					info.names.each(i -> {
						if(!names.contains(i)) namesQueue.add(i);
					});
				}
			}

			// IP -> UUID
			while (!ipQueue.isEmpty()) {
				var ip = ipQueue.removeFirst();
				
				for (PlayerInfo info : Vars.netServer.admins.playerInfo.values()) {
					if(uuids.contains(info.id)) continue; // already added
					if(info.ips.contains(ip)) uuidQueue.add(info.id);
				}
			}
			
			// Names -> UUID
			if(useNames) {
				while (!namesQueue.isEmpty()) {
					var name = namesQueue.removeFirst();
					for (PlayerInfo info : Vars.netServer.admins.playerInfo.values()) {
						if(uuids.contains(info.id)) continue; // already added
						if(info.names.contains(name)) uuidQueue.add(info.id);
					}
				}
			}
		}
		
//		uuids.remove(args[0]);
//		ips.remove(args[0]);
//		namesQueue.remove(args[0]);

		Cons2<String, ObjectSet<?>> send = (title, seq) -> {
			int id = 0;
			StringBuilder message = new StringBuilder("[gold]").append(title).append(" (").append(seq.size).append("):");
			if(seq.size == 0) {
				message.append("[gray] <empty>");
				sender.sendMessage(message.toString());
				return;
			}
			for (var p : seq) {
				if(message.length() > 0) message.append('\n');
				message.append(Strings.format("[gray]@. [white]@", id + 1, p));
				if(id%10 == 0) {
					sender.sendMessage(message.toString());
					message.setLength(0);
				}
				id++;
			}
			if(message.length() > 0) sender.sendMessage(message.toString());
		};
		send.get("UUIDs", uuids);
		send.get("IPs", ips);
		send.get("Names", names);
		send.get("Joined less " + limit + " times", single);
	}

	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return completePlayers();
		if(args.length == 1) return Seq.with("0", "1", "2", "5", "10");
		if(args.length == 2) return Seq.with("namecheck");
		return super.complete(args, receiver, type);
	}
}
