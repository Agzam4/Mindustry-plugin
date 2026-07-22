package agzam4.maps;

import arc.util.serialization.Jval;
import mindustry.maps.Map;

public class CustomMapSlot extends MapSlot {

	public final Map map;
	
	public CustomMapSlot(int id, Map map) {
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
		jval.put("custom", map.file.nameWithoutExtension());
		return jval;
	}

	@Override
	public String name() {
		return map.name();
	}
}
