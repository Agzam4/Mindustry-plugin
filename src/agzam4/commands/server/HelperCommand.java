package agzam4.commands.server;

import agzam4.CommandsManager;
import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.Game;
import agzam4.admins.AdminData;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import agzam4.commands.Permissions;
import agzam4.utils.Log;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.net.Administration.PlayerInfo;

public class HelperCommand extends CommandHandler<Object> {
	
	{
		parms = "<add/remove/refresh> [args...]";
		desc = "Добавить помошника / разрешения";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		int code = 0;
		if(args[0].equalsIgnoreCase("add")) code = 1;
		else if(args[0].equalsIgnoreCase("remove")) code = -1;
		else if(args[0].equalsIgnoreCase("refresh")) code = 2;
		if(code == 0) {
            Player found = Game.findPlayer(args[0]);
			if(require(found == null, sender, "[red]UIID не найден")) return;
            
			AdminData data = Admins.adminData(found.getInfo());
			if(require(data == null, sender, "[red]Игрок не помошник")) return;
			if(args.length == 1) {
				if(data.permissionsCount() == 0) sender.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [lightgray]<empty>", found.plainName()));
				else sender.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [gold]@", found.plainName(), data.permissionsAsString(' ')));
				return;
			}
			String[] keys = args[1].split(" ");
			for (int i = 0; i < keys.length; i++) {
				String arg = keys[i];
				if(arg.length() < 2) continue;
				Log.info("Argumet: \"@\" with char \"@\" and value \"@\"", arg, arg.charAt(0), arg.substring(1));
				if(arg.charAt(0) == '+') data.add(arg.substring(1));
				else if(arg.charAt(0) == '-') data.remove(arg.substring(1));
			}
			if(data.permissionsCount() == 0) sender.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [lightgray]<empty>", found.plainName()));
			else sender.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [gold]@", found.plainName(), data.permissionsAsString(' ')));
			Admins.save();
			return;
		} else {
			if(require(args.length < 2, sender, "[red]Слишком мало аргументов")) return;

			if(code == -1) {
	            PlayerInfo info = Vars.netServer.admins.playerInfo.get(args[1]);
	            if(info == null) {
	                Player found = Game.findPlayer(args[1]);
	            	if(found != null) info = Vars.netServer.admins.playerInfo.get(found.uuid());
	            }
				if(require(info == null, sender, "[red]Игрок не найден")) return;
				
				if(Admins.remove(info)) sender.sendMessage("Игрок [gold]" + info.plainLastName() + "[] успешно удален!");
				else sender.sendMessage("[red]Игрок [gold]" + info.plainLastName() + "[] не найден!");
				return;
			}

            Player found = Game.findPlayer(args[1]);
			if(require(found == null, sender, "[red]Игрок не найден")) return;
			
			if(code == 2) {
				if(Admins.refresh(found)) sender.sendMessage("Игрок [gold]" + found.plainName() + "[] успешно обновлен!");
				else sender.sendMessage("[red]Игрок [gold]" + found.plainName() + "[] уже обновлен!");
			} if(code == 1) {
				if(Admins.add(found)) sender.sendMessage("Игрок [gold]" + found.plainName() + "[] успешно добавлен!");
				else sender.sendMessage("[red]Игрок [gold]" + found.plainName() + "[] уже добавлен!");
			}
			Admins.save();
		}
	}

	@Override
	public Seq<String> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return completePlayers().addAll("add ", "remove", "refresh");
		if(args.length == 1) {
			if(args[0].equals("remove")) {
				Seq<String> list = Seq.with();
				Admins.admins().each((i,d) -> list.add(i.id + " " + i.lastName));
				return list;
			}
			if(args[0].equals("add") || args[0].equals("refresh")) return completePlayers();
		}
        Player found = Game.findPlayer(args[0]);
        if(found == null) return null;
        var data = Admins.adminData(found);
        if(data == null) return null;
        

		Seq<String> list = new Seq<String>();
		ObjectSet<String> set = new ObjectSet<String>(args.length-1);
		for (int i = 1; i < args.length; i++) {
			if(args[i].length() < 2) continue;
			set.add(args[i].substring(1));
		}
		
		for (var p : Permissions.values()) {
			if(set.contains(p.name)) continue;
			list.add((data.has(p.name) ? "-" : "+") + p.name);
		}
		
		Log.info(data.permissionsAsString(' '));
		CommandsManager.playerCommands().each(c -> {
			if(!c.admin) return;
			if(set.contains(c.text)) return;
			list.add((data.has(c.text) ? "-" : "+") + c.text);
		});
		return list;
	}
	
}
