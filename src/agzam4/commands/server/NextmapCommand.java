package agzam4.commands.server;

import agzam4.Game;
import agzam4.CommandsManager.ResultSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import agzam4.events.EventMap;
import agzam4.events.ServerEventsManager;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.maps.Map;

public class NextmapCommand extends CommandHandler<Object> {

	{
		parms = "[название...]";
		desc = "Устанавливает следущую карту";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		if(args.length == 0) {
			StringBuilder maps = new StringBuilder("Карты:");
			int id = 0;
			for(Map map : Vars.maps.all()){
				id++;
				String mapName = Strings.stripColors(map.name());
				if(type == ReceiverType.bot) {
					maps.append(Strings.format("\n<code>#@</code> <i>@</i> <code>@</code> <i>(@x@, рекорд: @)</i>", 
							id, map.custom ? "Кастомная" : "Дефолтная", mapName, map.width, map.height, map.getHightScore()));
				} else {
					maps.append(Strings.format("\n[gold]#@ @ [white]| @ [white](@x@, рекорд: @)", 
							id, map.custom ? "Кастомная" : "Дефолтная", mapName, map.width, map.height, map.getHightScore()));
				}
			}
			sender.sendMessage(maps.toString());
			return;
		}
		
        Map res = Vars.maps.all().find(map -> map.plainName().replace('_', ' ').equalsIgnoreCase(Game.strip(args[0]).replace('_', ' ')));
        boolean canEventmaps = receiver instanceof Player player ? Admins.has(player, "eventmaps") : true;
        if(args[0].startsWith("$") && canEventmaps) {
			try {
				EventMap em = EventMap.maps.get(Integer.parseInt(args[0].substring(1))-1);
				em.setNextMapOverride();
				sender.sendMessage(type.format("nextmap.set-event", em.map().plainName()));
			} catch (Exception e) {
				sender.sendMessage(e.getMessage());
			}
        	return;
        }
        if(res == null && args[0].startsWith("#")) {
			try {
				res = Vars.maps.all().get(Integer.parseInt(args[0].substring(1))-1);
			} catch (Exception e) {
				sender.sendMessage(e.getMessage());
			}
		}
        if(res != null){
        	Vars.maps.setNextMapOverride(res);
        	ServerEventsManager.setNextMapEvents(null);
            sender.sendMessage(type.format("nextmap.set", res.plainName()));
        }else{
        	sender.sendMessage(type.err("nextmap.not-found"));
        }
    }
	
	@Override
	public Seq<String> complete(String[] args, Object receiver, ReceiverType type) {
		return Vars.maps.all().map((m -> m.plainName().replace(' ', '_')));
	}
	
}
