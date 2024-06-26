package example.events;

import mindustry.content.Blocks;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.world.Block;
import mindustry.world.Tile;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

import arc.util.Log;

import static mindustry.Vars.*;

public class LivingWorldEvent extends ServerEvent {

	public LivingWorldEvent() {
		super("living_world"); // living_world
		color = "lime";
	}

	@Override
	public void init() {
		
	}

	@Override
	public void announce() {
		Call.announce("[lime]Событие \"Живая руда\" начнется на следующей карте!");
		Call.sendMessage("[lime]Событие \"Живая руда\" начнется на следующей карте!");	
	}

	@Override
	public void playerJoin(PlayerJoin e) {
		if(e.player == null) return;
		e.player.sendMessage("[lime]Руда теперь убегает, окружайте ее блоками, чтобы не дать уйти");
	}
	
	@Override
	public void tap(TapEvent e) {
		Log.info(e.tile);
		if(e.tile == null) return;
		if(e.tile.overlay() == null) return;
		if(e.tile.overlay() == Blocks.air) return;
		Log.info(e.tile.overlay());
		
		for (int i = 0; i < oreTiles.size(); i++) {
			Point p = oreTiles.get(i);
			if(p.x == e.tile.centerX() && p.y == e.tile.centerY()) {
				moveOre(i);
				break;
			}
		}
	}
	
	int updates = 0;
	
	@Override
	public void update() {
		if(oreMap == null) return;
		updates++;
		
		if(updates % 6 == 0 && oreTiles.size() > 0) {
			moveOre(random.nextInt(oreTiles.size()));
		}
	}
	
	private void moveOre(int index) {
		Point oreTile = oreTiles.get(index);
		
		ArrayList<Point> canmove = new ArrayList<>();
		
		for (int i = 0; i < nearTiles.length; i++) {
			int x = oreTile.x + nearTiles[i].x;
			int y = oreTile.y + nearTiles[i].y;
			if(!isInArray(x, y)) continue;
			Tile tile = world.tile(x, y);
			if(tile == null) continue;
			Building building = tile.build;
			if(building != null) continue;

			boolean checkBlock = false;
			if(tile.block() == null) {
				checkBlock = true;
			} else {
				if(tile.block() == Blocks.air) {
					checkBlock = true;
				}
			}

			if(!checkBlock) continue;
			if(tile.overlay() != Blocks.air) continue;
			
			canmove.add(new Point(x, y));
		}
		
		if(canmove.size() > 0) {
			int randomMove = random.nextInt(canmove.size());
			Point move = canmove.get(randomMove);
			
			Tile current = world.tile(oreTile.x, oreTile.y);
			Tile changed = world.tile(move.x, move.y);
			
			oreTiles.remove(index);
			oreTiles.add(move);

			changed.setFloorNet(changed.floor(), current.overlay());
			current.setFloorNet(current.floor(), Blocks.air);
		}
	}
	
	private static final Point[] nearTiles = {
//			new Point(+1, +1),
			new Point( 0, +1),
//			new Point(-1, +1),
			
			new Point(+1,  0),
			new Point(-1,  0),
			
//			new Point(+1, -1),
			new Point( 0, -1)
//			new Point(-1, -1)
	};
	
	private static final Block[] ores = {
			Blocks.oreCopper, Blocks.oreLead, Blocks.oreScrap, Blocks.oreCoal, Blocks.oreTitanium, 
			Blocks.oreThorium, Blocks.oreBeryllium, Blocks.oreTungsten, Blocks.oreCrystalThorium
	};
	
	private boolean isInArray(int x, int y) {
		return 0 <= x && x < w 
			&& 0 <= y && y < h;
	}
	

	private int[] oreCounter;

	private int[][] oreMap;
	private int w, h;
	
	ArrayList<Point> oreTiles;
	Random random;
	
	@Override
	public void generateWorld() {
		updates = 0;
		oreTiles = new ArrayList<>();
		random = new Random();
		oreCounter = new int[ores.length];

		w = world.width();
		h= world.height();
		oreMap = new int[w][h];
		
		for (int y = 0; y < world.height(); y++) {
			for (int x = 0; x < world.width(); x++) {
				Tile tile = world.tile(x, y);
				if(tile == null) continue;
				Block overlay = tile.overlay();
				if(overlay == null) continue;
				
				int oreId = -1;
				for (int i = 0; i < ores.length; i++) {
					if(overlay == ores[i]) {
						oreCounter[i]++;
						oreId = i;
						oreTiles.add(new Point(x, y));
						break;
					}
				}
				oreMap[x][y] = oreId;
			}
		}
		Call.sendMessage("[lime]Руда теперь убегает, окружайте ее блоками, чтобы не дать уйти");
	}
}
