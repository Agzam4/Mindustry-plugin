package agzam4.commands.any;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.commands.CommandHandler;
import agzam4.events.EventMap;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.maps.Map;

public class MapsCommand extends CommandHandler<Object> {

	{
		parms = "[all/custom/default/event]";
		desc = "Показывает список доступных карт. Отображает все карты по умолчанию";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		String types = "all";
		if(args.length == 0) types = Vars.maps.getShuffleMode().name();
		else types = args[0];
		if(types.startsWith("event")) {
			int id = 0;
			for(EventMap map : EventMap.maps){
				id++;
				String mapName = Strings.stripColors(map.map().name());
				sender.sendMessage(Strings.format("[gold]$@ @ [white]| @ [white](@x@, рекорд: @)", 
						id, map.events(), mapName, map.map().width, map.map().height, map.map().getHightScore()));
			}
			return;
		}
		boolean custom  = types.equals("custom") || types.equals("c") || types.equals("all");
		boolean def     = types.equals("default") || types.equals("all");
		if(!Vars.maps.all().isEmpty()) {
			Seq<Map> all = new Seq<>();
			if(custom) all.addAll(Vars.maps.customMaps());
			if(def) all.addAll(Vars.maps.defaultMaps());
			if(all.isEmpty()){
				sender.sendMessage("Кастомные карт нет на этом сервере, используйте [gold]all []аргумет.");
			}else{
				sender.sendMessage("[white]Maps:");
				int id = 0;
				for(Map map : Vars.maps.all()){
					id++;
					if((def && !map.custom) || (custom && map.custom)) {
						String mapName = Strings.stripColors(map.name());
						sender.sendMessage(Strings.format("[gold]#@ @ [white]| @ [white](@x@, рекорд: @)", 
								id, map.custom ? "Кастомная" : "Дефолтная", mapName, map.width, map.height, map.getHightScore()));
					}
				}
			}
		} else {
			sender.sendMessage("Карты не найдены");
		}
	}

	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("all", "custom", "default", "event");
		return super.complete(args, receiver, type);
	}
}
