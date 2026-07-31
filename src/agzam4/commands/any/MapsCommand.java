package agzam4.commands.any;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.commands.CommandHandler;
import agzam4.maps.MapSlot;
import agzam4.maps.MapsManager;
import arc.struct.Seq;
import arc.util.Strings;

public class MapsCommand extends CommandHandler<Object> {

	{
		parms = "[all/custom/default/event]";
		desc = "Показывает список доступных карт. Отображает все карты по умолчанию";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		String types = "all";
		if(args.length == 0) types = "all";
		else types = args[0];
		
		if(types.equals("playlist")) {
			sender.sendMessage("[white]Playlist:");
        	for(var map : MapsManager.bungle){
        		if(!map.enabled) continue;
        		var m = map.map();
        		if(m == null) continue;
        		sender.sendMessage(Strings.format("[gold]#@ @ [white]| @ [white](@x@, рекорд: @)", 
        				map.id+1, map.custom ? "Кастомная" : "Дефолтная", map.name(), m.width, m.height, m.getHightScore()));
        	}
			return;
		}
		boolean custom  = types.equals("custom") || types.equals("c") || types.equals("all");
		boolean def     = types.equals("default") || types.equals("all");
		
        if(require(MapsManager.maps.isEmpty(), sender, "[red]Карты не найдены")) return;
        
        Seq<MapSlot> all = MapsManager.maps.select(m -> {
        	if(!m.enabled) return false;
        	if(!custom && m.custom) return false;
        	if(!def && !m.custom) return false;
        	return true;
        });

        if(require(MapsManager.maps.isEmpty(), sender, "[red]Подходящих под фильтр карт нет")) return;
        
        if(all.isEmpty()){
        	sender.sendMessage("Кастомные карт нет на этом сервере, используйте [gold]all []аргумет.");
        }else{
        	sender.sendMessage("[white]Maps:");
        	for(var map : all){
        		var m = map.map();
        		if(m == null) continue;
        		sender.sendMessage(Strings.format("[gold]#@ @ [white]| @ [white](@x@, рекорд: @)", 
        				map.id+1, map.custom ? "Кастомная" : "Дефолтная", map.name(), m.width, m.height, m.getHightScore()));
        	}
        }
	}

	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("all", "custom", "default", "playlist");
		return super.complete(args, receiver, type);
	}
}
