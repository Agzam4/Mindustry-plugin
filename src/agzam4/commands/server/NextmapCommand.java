package agzam4.commands.server;

import agzam4.Game;
import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.commands.CommandHandler;
import agzam4.events.ServerEventsManager;
import agzam4.maps.MapsManager;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;

public class NextmapCommand extends CommandHandler<Object> {

	{
		parms = "[название...]";
		desc = "Устанавливает следущую карту";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(args.length == 0) {
			StringBuilder maps = new StringBuilder("Карты:");
			for(var map : MapsManager.list(receiver)){
				String mapName = Strings.stripColors(map.name());
				var m = map.map();
				if(type == ReceiverType.bot) {
					maps.append(Strings.format("\n<code>#@</code> <i>@</i> <code>@</code> <i>(@x@, рекорд: @)</i>", 
							map.id+1, map.custom ? "Кастомная" : "Дефолтная", mapName, m.width, m.height, m.getHightScore()));
				} else {
					maps.append(Strings.format("\n[gold]#@ @ [white]| @ [white](@x@, рекорд: @)", 
							map.id+1, map.custom ? "Кастомная" : "Дефолтная", mapName, m.width, m.height, m.getHightScore()));
				}
			}
			sender.sendMessage(maps.toString());
			return;
		}
		
        var res = MapsManager.list(receiver).find(map -> map.name().replace('_', ' ').equalsIgnoreCase(Game.strip(args[0]).replace('_', ' ')));
        if(res == null && args[0].startsWith("#")) {
        	res = MapsManager.list(receiver).find(m -> m.id == Integer.parseInt(args[0].substring(1))-1);
		}
        if(require(res == null, sender, type.err("nextmap.not-found"))) return;
        var map = res.map();
        if(require(map == null, sender, type.err("nextmap.not-found"))) return;
        
        MapsManager.nextMap(res);
        
        sender.sendMessage(type.format("nextmap.set", map.plainName()));
    }
	
	@Override
	public Seq<String> complete(String[] args, Object receiver, ReceiverType type) {
		return MapsManager.list(receiver).map((m -> m.name().replace(' ', '_')));
	}
	
}
