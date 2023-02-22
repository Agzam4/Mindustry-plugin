package example.events;

import mindustry.content.Blocks;
import mindustry.game.EventType.PlayerJoin;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.world.Block;
import mindustry.world.Tile;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

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
	
	int updates = 0;
	
	@Override
	public void update() {
		if(oreMap == null) return;
		updates++;
		
		if(updates % 6 == 0 && oreTiles.size() > 0) {
			int randomIndex = random.nextInt(oreTiles.size());
			
			Point oreTile = oreTiles.get(randomIndex);
			
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
				boolean checkOverlay = false;
				if(tile.overlay() == null) {
					checkOverlay = true;
				} else {
					if(tile.overlay() == Blocks.air) {
						checkOverlay = true;
					}
				}
				
				if(!checkBlock) continue;
				if(!checkOverlay) continue;
				
				canmove.add(new Point(x, y));
			}
			
			if(canmove.size() > 0) {
				int randomMove = random.nextInt(canmove.size());
				Point move = canmove.get(randomMove);
				
				Tile current = world.tile(oreTile.x, oreTile.y);
				Tile changed = world.tile(move.x, move.y);
				oreTile = move;

				changed.setFloorNet(changed.floor(), current.overlay());
				current.setFloorNet(current.floor(), Blocks.air);
			}
		}
		
//		if(lastWaveId != state.wave) {
//			lastWaveId = state.wave;
//			
//			int[][] newOreMap = new int[world.width()][world.height()];
//			for (int yy = 0; yy < world.height(); yy++) {
//				for (int xx = 0; xx < world.width(); xx++) {
//					if(!isInArray(xx, yy)) continue;
//					int ore = oreMap[xx][yy];
//					int same = 0;
//					Tile tile = world.tile(xx, yy);
//					if(tile == null) continue;
//					
//					
//					int newTile = ore;
//					
//					if(ore == -1) {
//						int counter[] = new int[ores.length];
//						
//						for (int i = 0; i < nearTiles.length; i++) {
//							int x = xx + nearTiles[i].x;
//							int y = yy + nearTiles[i].y;
//							if(!isInArray(x, y)) continue;
//							if(oreMap[x][y] != -1) {
//								counter[oreMap[x][y]]++;
//							}
//						}
//
//						for (int i = 0; i < counter.length; i++) {
//							if(counter[i] > 1 && counter[i] < 7) {
//								newTile = i;
//								break;
//							}
//						}
//					} else {
//						for (int i = 0; i < nearTiles.length; i++) {
//							int x = xx + nearTiles[i].x;
//							int y = yy + nearTiles[i].y;
//							if(!isInArray(x, y)) continue;
//							if(oreMap[x][y] == ore) {
//								same++;
//							}
//						}
//						if(same > 7) {
//							newTile = -1;
//						}
//					}
//					
//					if(newTile == ore) continue;
//					Floor floor = tile.floor();
//					if(floor == null) continue;
//					newOreMap[xx][yy] = newTile;
//					world.tile(xx, yy).setFloorNet(floor, newTile == -1 ? Blocks.air : ores[newTile]);
//				}
//			}
//			
//			oreMap = newOreMap;
//		}
	}
	
	private static final Point[] nearTiles = {
			new Point(+1, +1),
			new Point( 0, +1),
			new Point(-1, +1),
			
			new Point(+1,  0),
			new Point(-1,  0),
			
			new Point(+1, -1),
			new Point( 0, -1),
			new Point(-1, -1)
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
	private int lastWaveId = 0;
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
