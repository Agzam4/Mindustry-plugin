package agzam4.maps;

import agzam4.Game;
import arc.struct.Seq;

public class MapCreator {
	
	
	public Seq<UserMapSlot> slots = Seq.with();
	public final String uuid;
	public int maxMaps = 0;
	
	protected MapCreator(String uuid) {
		this.uuid = uuid;
	}

	public String name() {
		return Game.nameByUuid(uuid);
	}
	
	@Override
	public String toString() {
		return "Maker[" + uuid + "]";
	}
}
