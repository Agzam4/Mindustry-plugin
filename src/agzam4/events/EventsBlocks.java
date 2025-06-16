package agzam4.events;

import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

public class EventsBlocks {

	
	private static TileData[] world;
	private static ObjectSet<Integer> update = new ObjectSet<Integer>();
	private static ObjectSet<Integer> remove = new ObjectSet<Integer>();
	private static Queue<Runnable> planned = new Queue<Runnable>();
	private static int placedPerTick = 0;
	

	private static int changesLimit = 10;
	private static int tickchanges = 0;

	public static void reset() {
		world = null;
		update.clear();
		remove.clear();
		planned.clear();
	}
	
	public static void set() {
		world = new TileData[Vars.world.width()*Vars.world.height()];
	}

	public static void update() {
		if(world == null) return;
		placedPerTick = 0;
		tickchanges = 0;
		update.each(id -> {
			if(world[id] == null) return;
			world[id].update(id);
		});
		if(remove.size > 0) {
			remove.each(r -> {
				world[r] = null;
				update.remove(r);
			});
			remove.clear();
		}
		for (int i = placedPerTick; i < changesLimit; i++) {
			if(planned.size == 0) break;
			planned.removeFirst().run();
		}
	}
	
	public static boolean hasDifference(Tile tile, @Nullable Block block, @Nullable Block floor, @Nullable Block overlay) {
		if(block != null && tile.block() != block) return true;
		if(floor != null && tile.floor() != floor) return true;
		if(overlay != null && tile.overlay() != overlay) return true;
		return false;
	}
	
	private static void initTileData(Tile tile) {
		if(world[tile.array()] == null) {
			world[tile.array()] = TileData.from(tile);
			update.add(tile.array());
		}
	}
	
	public static void setTempTile(@Nullable Tile tile, @Nullable Block block, @Nullable Block floor, @Nullable Block overlay, int blifetime, int flifetime) {
		if(tile == null) return;
		if(block != null && !hasDifference(tile, block, null, null)) { // if not changes at block: just update lifetime
			initTileData(tile);
			world[tile.array()].blocklifetime(blifetime);
			block = null;
		}
		if((floor != null || overlay != null) && !hasDifference(tile, null, floor, overlay)) { // if not changes at floor: just update lifetime
			initTileData(tile);
			world[tile.array()].floorlifetime(flifetime);
			floor = null;
			overlay = null;
		}
		if(block == null && floor == null && overlay == null) return; // no changes :)
		
		placedPerTick++;
		if(placedPerTick > changesLimit*3) {
			final Block b = block;
			final Block f = floor;
			final Block o = overlay;
			planned.add(() -> {
				initTileData(tile);
				world[tile.array()].floorlifetime(flifetime);
				world[tile.array()].blocklifetime(blifetime);
				if(b != null && tile.build == null) tile.setNet(b);
				if(o == Blocks.air && f != null) {
					tile.setFloorNet(f);
				} else if(f != null || o != null) {
					tile.setFloorNet(f == null ? tile.floor() : f, o == null ? tile.overlay() : o);
				}
			});
		} else {
			initTileData(tile);
			world[tile.array()].floorlifetime(flifetime);
			world[tile.array()].blocklifetime(blifetime);
			if(block != null && tile.build == null && tile.block() != block) tile.setNet(block);
			if(overlay == Blocks.air && floor != null) {
				if(tile.floor() != floor && tile.overlay() != Blocks.air) tile.setFloorNet(floor);
			} else if(floor != null || overlay != null) {
				if((floor == null || tile.floor() == floor) && (overlay == null || tile.overlay() == overlay)) return;
				tile.setFloorNet(floor == null ? tile.floor() : floor, overlay == null ? tile.overlay() : overlay);
			}
		}
	}
	
	public static boolean hasTmpFloor(Tile tile) {
		if(world[tile.array()] == null) return false;
		return world[tile.array()].flifetime > 0;
	}
	
	
	static class TileData {

		private Block block;
		private Floor floor, overlay;
		private int flifetime, blifetime;

		public static TileData from(Tile tile) {
			TileData td = new TileData();
			td.block = tile.build == null ? tile.block() : null;
			td.floor = tile.floor();
			td.overlay = tile.overlay();
			return td;
		}

		public void update(int id) {
			updatefloor(id);
			updateblock(id);
		}

		private void updateblock(int id) {
			if(blifetime <= 0) return;
			blifetime--;
			if(blifetime == 0) {
				if(tickchanges < changesLimit) {
					Tile tile = Vars.world.tiles.geti(id);
					if(block != null) tile.setNet(block);
					if(flifetime <= 0) remove.add(id);
					tickchanges++;
				} else {
					blifetime++;
				}
			}	
		}

		private void updatefloor(int id) {
			if(flifetime <= 0) return;
			flifetime--;
			if(flifetime == 0) {
				if(tickchanges < changesLimit) {
					Tile tile = Vars.world.tiles.geti(id);
					tile.setFloorNet(floor, overlay);
					if(blifetime <= 0) remove.add(id);
					tickchanges++;
				} else {
					flifetime++;
				}
			}			
		}

		public void floorlifetime(int lifetime) {
			if(lifetime < 1) lifetime = 1;
			this.flifetime = Math.max(lifetime, this.flifetime);
		}

		public void blocklifetime(int lifetime) {
			if(lifetime < 1) lifetime = 1;
			this.blifetime = Math.max(lifetime, this.blifetime);
		}
		
		public void lifetime(int lifetime) {
			this.flifetime = Math.max(lifetime, this.flifetime);
			this.blifetime = Math.max(lifetime, this.blifetime);
		}
		
		
	}
}
