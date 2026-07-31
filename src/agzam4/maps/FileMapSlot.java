package agzam4.maps;

import agzam4.utils.Log;
import arc.func.Prov;
import arc.struct.Seq;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.maps.Map;

public class FileMapSlot extends MapSlot {

	public enum MapFileTypes {
		
		buildIn("default", () -> Vars.maps.defaultMaps()), 
		custom("custom", () -> Vars.maps.customMaps());
		
		public final String tag;
		public final Prov<Seq<Map>> maps;
		
		private MapFileTypes(String tag, Prov<Seq<Map>> maps) {
			this.tag = tag;
			this.maps = maps;
		}
		
	}
	
	public MapFileTypes type;
	public Map map;

	public FileMapSlot(MapFileTypes type, int id, Jval jval) {
		super(id);
		this.type = type;
		this.custom = type == MapFileTypes.custom;
		read(jval);
	}
	
	public FileMapSlot(MapFileTypes type, int id, Map map) {
		super(id);
		this.map = map;
		this.type = type;
		this.custom = type == MapFileTypes.custom;
	}

	@Override
	public Map map() {
		return map;
	}
	
	@Override
	public void read(Jval jval) {
		super.read(jval);
		String name = jval.getString(type.tag);
		map = type.maps.get().find(m -> m.file.nameWithoutExtension().equals(name));
		if(map == null) Log.warn("Map not found: @", name);
	}

	@Override
	public Jval save() {
		var jval = super.save();
		jval.put(type.tag, map.file.nameWithoutExtension());
		return jval;
	}

	@Override
	public String name() {
		return map.name();
	}
	
	@Override
	public String toString() {
		return "FM-" + id + ":" + map;
	}
	
}
