package agzam4.events;

import java.io.IOException;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.io.MapIO;
import mindustry.maps.Map;

public class EventMap {
	
	public static Seq<EventMap> maps = new Seq<EventMap>();;
	public static Fi eventMapsDirectory;
	
	public static void load() {
		eventMapsDirectory = Vars.dataDirectory.child("event_maps");
		if(!eventMapsDirectory.exists()) eventMapsDirectory.mkdirs();
		for (Fi file : eventMapsDirectory.list()) {
			try {
				EventMap map = new EventMap(file);
				maps.add(map);
			} catch (IOException e) {
				Log.err("Error to load @ map", file);
			}
		}
	}

	public static void reload() {
		maps.clear();
		load();
	}
	
	final Map map;
	final boolean[] events;
	
	public EventMap(Fi file) throws IOException {
		map = MapIO.createMap(file, false);
		String[] worlds = map.description().replaceAll("\n", " ").split(" ");
		events = new boolean[0]; /// FIXME//ServerEventsManager.getServerEventsCount()];
		Log.info("Loading map @", file);
		for (int i = 0; i < worlds.length; i++) {
			if(!worlds[i].startsWith("#")) continue;
			ServerEvent event = ServerEventsManager.find(worlds[i].substring(1));
			if(event == null) continue;
//			events[event.ordinal()] = true;
			Log.info("> Event: @", event);
		}
	}

	public Map map() {
		return map;
	}

	public void setNextMapOverride() {
		ServerEventsManager.setNextMapEvents(this);
		Vars.maps.setNextMapOverride(map);
	}

	public boolean has(ServerEvent event) { // FIXME
		return false;//events[event.ordinal()];
	}

	public String events() {
		StringBuilder es = new StringBuilder();
//		for (int i = 0; i < ServerEvents.values().length; i++) {
//			if(!events[i]) continue;
//			if(es.length() != 0) es.append(' ');
//			es.append('[');
//			es.append(ServerEvents.values()[i].color());
//			es.append("]#");
//			es.append(ServerEvents.values()[i].tag().toLowerCase());
//			es.append("[]");
//		}
		return es.toString();
	}

	
	
}
