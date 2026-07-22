package agzam4.maps;

import arc.util.serialization.Jval;
import mindustry.maps.Map;

public class DefaultMapSlot extends MapSlot {

	public final Map map;
	
	public DefaultMapSlot(int id, Map map) {
		super(id);
		this.map = map;
	}

	@Override
	public Map map() {
		return map;
	}

	@Override
	public Jval save() {
		var jval = Jval.newObject();
		jval.put("default", map.file.nameWithoutExtension());
		return jval;
	}

	@Override
	public String name() {
		return map.name();
	}
	
	
}
