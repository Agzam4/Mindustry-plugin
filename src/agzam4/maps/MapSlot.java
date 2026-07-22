package agzam4.maps;

import arc.util.Nullable;
import arc.util.serialization.Jval;
import mindustry.maps.Map;

public abstract class MapSlot {

	public final int id;
	
	protected MapSlot(int id) {
		this.id = id;
	}


	public abstract @Nullable Map map();

	public abstract Jval save();

	public abstract String name();
}
