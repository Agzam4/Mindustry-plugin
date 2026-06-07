package agzam4.antigrif;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Nullable;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.world.Block;
import mindustry.world.Tile;

public abstract class AntigriefSystem<Action> {

	public ObjectSet<Block> blocks = ObjectSet.with();
	public final ObjectMap<String, Action> grief = ObjectMap.of();

	public abstract void config();
	public abstract void register();
	public abstract void unregister();

	public AntigriefSystem() {
		config();
	}
	
	public boolean isBlock(@Nullable Tile tile) {
		if(tile == null) return false;
		return blocks.contains(tile.block());
	}

	public void onGrief(Player griefer, String message) {
		Call.sendMessage(griefer.coloredName() + ":[white] " + message);
	}
	
	public void warn(Player griefer, String message) {
		
	}
	
}
