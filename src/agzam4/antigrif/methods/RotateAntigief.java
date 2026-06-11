package agzam4.antigrif.methods;

import agzam4.antigrif.AntigriefSystem;
import agzam4.antigrif.Antigriefs.Config;
import arc.Events;
import arc.func.Cons;
import arc.math.geom.Geometry;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.BuildRotateEvent;
import mindustry.gen.Building;
import mindustry.gen.Player;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.liquid.Conduit;

public class RotateAntigief extends AntigriefSystem<agzam4.antigrif.methods.RotateAntigief.RotateActions> {
	
	@Config
	public boolean checknext = true;

	@Config
	public boolean checknextinputs = true;
	
//	private Tile[] prevSenders = new Tile[4];
	
	public final Cons<BuildRotateEvent> rotate = e -> {
		Log.info("Event: @ @ @", e.unit, e.build, e.unit.controller());
		if(!blocks.contains(e.build.block)) return;
		if(!(e.unit.controller() instanceof Player player)) return;
//		int x = e.build.tileX();
//		int y = e.build.tileY();
//		int senderId = 0;
//		for (int i = 0; i < 4; i++) {
//			if(i == e.previous) continue;
//			Tile t = Vars.world.tile(x - Geometry.d4x[i], y - Geometry.d4y[i]);
//			if(t.build == null) continue;
////			prevSenders[senderId++];
////			t.build.rot
//		}
		
		if(checknext) {
			Log.info("validNext: @ @", validNext(e.build, e.previous), validNext(e.build, e.build.rotation));
			if(validNext(e.build, e.previous) && !validNext(e.build, e.build.rotation)) {
				onGrief(player, "Блок никуда не ведет: " + e.build.tileX() + " " + e.build.tileY());
			}
		}
		
		if(checknextinputs) {
			int x = e.build.tileX() + Geometry.d4x[e.previous];
			int y = e.build.tileY() + Geometry.d4y[e.previous];
			
			Building next = Vars.world.build(x, y);
			Log.info("Build: @", next);
			if(next == null) return;
			var edges = next.block.getEdges();
			boolean hasInputs = false;
			search:
			for (int i = 0; i < edges.length; i++) {
				int xx = x + edges[i].x;
				int yy = y + edges[i].y;
				var from = Vars.world.build(xx, yy);
				if(from == null) continue;
				if(from.front() != next) continue;
				for (int item = 0; item < Vars.content.items().size; item++) {
					if(next.acceptItem(from, Vars.content.item(item))) {
						hasInputs = true;
						
						Log.info("Found: @ --@--> @", from, next.acceptItem(from, Vars.content.item(item)), next);
						break search;
					}
				}
			}
			if(!hasInputs) {
				onGrief(player, "Блок лишен входных ресурсов: " + next.tileX() + " " + next.tileY());
			}
			
//			for (int i = 0; i < 4; i++) {
//				if(i == e.previous) continue;
//				Tile t = Vars.world.tile(x - Geometry.d4x[i], y - Geometry.d4y[i]);
//				if(t.build == null) continue;
//				prevSenders[senderId++];
//				t.build.rot
//			}
		}
		
//		Tile prevBack = Vars.world.tile(x - Geometry.d4x[e.previous], y - Geometry.d4y[e.previous]);
//		if(prevBack.build == null) return; // previous state - wrong (no
//		Tile prevNext = Vars.world.tile(x + Geometry.d4x[e.previous], y + Geometry.d4y[e.previous]);
//		if(prevNext.build == null || prevNext.build.acc) return; // previous state - wrong
		
		
//		if()
		
//		e.previous;
	};
	
	private boolean validNext(Building current, int rotation) {
		Tile next = Vars.world.tile(current.tileX() + Geometry.d4x[rotation], current.tileY() + Geometry.d4y[rotation]);
		if(next == null) return false;
		if(next.build == null) return false;
		return validAccept(current.block, next.block());
	}

	private boolean validAccept(Block current, Block next) {
		if(current.hasItems && next.acceptsItems) return true;
		if(current.outputsLiquid && next.hasLiquids) return true;
		return false;
	}

	@Override
	public void config() {
		Vars.content.blocks().each(b -> {
			if(b instanceof Conveyor || b instanceof Conduit) blocks.add(b);
		});
	}
	
	
	@Override
	public void register() {
		Events.on(BuildRotateEvent.class, rotate);
	}

	@Override
	public void unregister() {
		Events.remove(BuildRotateEvent.class, rotate);
	}
	
	public static class RotateActions {
		
	}
}
