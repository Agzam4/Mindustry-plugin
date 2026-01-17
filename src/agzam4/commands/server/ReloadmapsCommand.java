package agzam4.commands.server;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.achievements.AchievementsManager;
import agzam4.commands.CommandHandler;
import agzam4.events.EventMap;
import mindustry.Vars;

public class ReloadmapsCommand extends CommandHandler<Object> {

	{
		desc = "Перезагрузить карты";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		int beforeMaps = Vars.maps.all().size;
		Vars.maps.reload();
		if (Vars.maps.all().size > beforeMaps) {
			sender.sendMessage("[gold]" + (Vars.maps.all().size - beforeMaps) + " новых карт было найдено");
		} else if (Vars.maps.all().size < beforeMaps) {
			sender.sendMessage("[gold]" + (beforeMaps - Vars.maps.all().size) + " карт было удалено");
		} else {
			sender.sendMessage("[gold]Карты перезагружены");
		}
		beforeMaps = EventMap.maps.size;
		EventMap.reload();
		if (EventMap.maps.size > beforeMaps) {
			sender.sendMessage("[gold]" + (EventMap.maps.size - beforeMaps) + " ивентных новых карт было найдено");
		} else if (EventMap.maps.size < beforeMaps) {
			sender.sendMessage("[gold]" + (beforeMaps - EventMap.maps.size) + " ивентных карт было удалено");
		} else {
			sender.sendMessage("[gold]Ивентные карты перезагружены");
		}
		AchievementsManager.updateMaps();
	}

}
