package agzam4.maps;

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
	public ObjectMap<String, Boolean> events;
	
	
	public boolean enabled = true;
	
	public final int id;
	
	protected MapSlot(int id) {
		this.id = id;
	}

	public abstract @Nullable Map map();
	

	public void read(Jval jval) {
		enabled = !jval.getBool("disabled", false);
	}

	public Jval save() {
		var val = Jval.newObject();
		if(!enabled) val.put("disabled", true);
		return val;
	}

	public abstract String name();

	public boolean custom = false;
	
}
