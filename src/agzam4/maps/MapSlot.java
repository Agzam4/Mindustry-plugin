package agzam4.maps;

import agzam4.events.ServerEventsManager;
import arc.struct.ObjectMap;
import arc.util.Nullable;
import arc.util.serialization.Jval;
import mindustry.maps.Map;

public abstract class MapSlot {

	/**
	 * Override events on map
	 * true - enable event
	 * false - never use event on this map
	 * not in map means - use default
	 */
	public ObjectMap<String, Boolean> events = ObjectMap.of();
	
	
	public boolean enabled = true;
	
	public final int id;
	
	protected MapSlot(int id) {
		this.id = id;
	}

	public abstract @Nullable Map map();
	

	public void read(Jval jval) {
		enabled = !jval.getBool("disabled", false);
		if(jval.has("events")) {
			var statuses = jval.get("events");
			ServerEventsManager.events.each(e -> {
				if(!statuses.has(e.name)) return;
				events.put(e.name, statuses.getBool(e.name, false));
			});
		}
	}

	public Jval save() {
		var val = Jval.newObject();
		if(!enabled) val.put("disabled", true);
		
		var jevents = Jval.newObject();
		events.each((e,v) -> jevents.put(e, v));
		val.put("events", jevents);
		
		return val;
	}

	public abstract String name();

	public boolean custom = false;
	
}
