package example;

import arc.Events;
import arc.graphics.Color;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.core.World;
import mindustry.game.EventType.*;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.modules.PowerModule;

public class ServerEventsManager {
	
	/**
	 * TODO:
	 * Water enemy can broke ice - OK
	 * Cryofluid filling
	 * cryofluid cooling
	 * 
	 * Remove rules
	 */

	public static long eventsTPS = 1_000 / 60;
	public static final String[] EVENTS_ID = {"new_year"};
	public boolean[] isEventsOn;
	
    
	public ServerEventsManager() {
    	isEventsOn = new boolean[EVENTS_ID.length];
//    	isEventsOn[0] = true;
    	
//    	Vars.world.addMapLoader(Vars.state.map, new MapLoader());
	}
	
	
	private String lastmapname = "";

//	private World defWorld;
	
	private Floor[][] floor, overlay;
	private Block[][] blocks;
	private byte[][] cold;
	
	public boolean isLoaded = true;
	
	boolean isRunning = false;
	
	public void startEventsLoop() {
		
		Events.on(WorldLoadBeginEvent.class, e -> {
			Vars.world.setGenerating(true);
        });
		
		Events.on(WorldLoadEndEvent.class, e -> {
			Log.info("Loaded!");
			updateWorld(Vars.world);
			Vars.world.setGenerating(false);
        });
		
		if(isRunning) return;
		new Thread(() -> {
			isLoaded = false;
			Call.announce("Новогднее событие начнется на следующей карте!");
			Call.sendMessage("[sky]Новогднее событие начнется на следующей карте!");
			while (isEventsOn[0]) {
				isRunning = true;
//				World world = Vars.world;
				try {
					
					if(isLoaded) {
//						if(!Vars.state.rules.lighting) {
//							Vars.state.rules.lighting = true;
//							Vars.state.rules.ambientLight = new Color(0f, 0f, 0f, .5f);
//						}
						for (int i = 0; i < Groups.unit.size(); i++) {
							Unit unit = Groups.unit.index(i);
							if(!unit.isFlying()) {
								if(unit.tileOn() != null) {
									Tile tile = unit.tileOn();
									int tileX = unit.tileX();
									int tileY = unit.tileY();
									if(tileX < 0) continue;
									if(tileY < 0) continue;
									if(tileX + 1 >= width) continue;
									if(tileY + 1 >= height) continue;
									
									if(cold[tileX][tileY] > 50) {
										if(Math.random() < .25) {
											unit.apply(StatusEffects.freezing, 300);
										}else if(Math.random() < 1f/unit.bounds()) {
											unit.apply(StatusEffects.unmoving, 30);
										}
									}
									
									if(tile.floor().isLiquid || tile.overlay().isLiquid) {
										cold[tileX][tileY] = 0;
										returnTileBack(tile, unit.tileX(), unit.tileY());
									} else if(Math.random() > 10f/unit.bounds() && Math.random() < .05){
										cold[tileX][tileY] = 0; 
										returnTileBack(tile, unit.tileX(), unit.tileY());
									} else {
										continue;
									}
									
									double randAlign = Math.random()*Math.toRadians(360);
									double randHypot = Math.random()*unit.bounds()/20f;
									int tx = (int) (unit.tileX() + randHypot*Math.cos(randAlign));
									int ty = (int) (unit.tileY() + randHypot*Math.sin(randAlign));
									
									if(tx < 0) continue;
									if(ty < 0) continue;
									if(tx + 1 >= width) continue;
									if(ty + 1 >= height) continue;

									cold[tx][ty] = 0;
									if(Vars.world.tile(tx, ty) != null) {
										returnTileBack(Vars.world.tile(tx, ty), tx, ty);
									}
								}
							}
						}
						
						World world = Vars.world;
						for (int i = 0; i < Groups.player.size(); i++) {
							Player player = Groups.player.index(i);
							
							double randAlign = Math.random()*Math.toRadians(360);
							double randHypot = Math.random()*10;
							int tileX = (int) (player.tileX() + randHypot*Math.cos(randAlign));
							int tileY = (int) (player.tileY() + randHypot*Math.sin(randAlign));

							if(tileX < 0) continue;
							if(tileY < 0) continue;
							if(tileX + 1 >= width) continue;
							if(tileY + 1 >= height) continue;
							
							if(cold[tileX][tileY] >= 50) {
								if(!player.unit().hasEffect(StatusEffects.freezing)) {
									player.unit().damage(1, true);
									player.unit().apply(StatusEffects.freezing, 180);
								}
							}
							
							Tile tileBuilding = world.tileBuilding(tileX, tileY);//.build.power.status;
							
							if(tileBuilding != null) {
								Building building = tileBuilding.build;
								if(building != null) {
//									Liquids 
//									building.liquids().get(Liquids.cryofluid);
									PowerModule powerModule = building.power();
									if(powerModule != null) {
//										Log.info(powerModule.status);
										
										int r = (int) (world.tile(tileX, tileY).block().lightRadius/10*powerModule.status);
										for (int y = tileY-r; y <= tileY+r; y++) {
											for (int x = tileX-r; x <= tileX+r; x++) {
												double hypot = Math.hypot(tileX-x, tileY-y);
												if(hypot <= r) {
													if(x < 0) continue;
													if(y < 0) continue;
													if(x + 1 >= width) continue;
													if(y + 1 >= height) continue;
													Tile tile = world.tile(x, y);
													if(tile == null) continue;
													
													if(Math.random() > hypot/r) {
														int remove = 25 - (int) (hypot*25/r);
														remove *= Math.random();
														if(remove > cold[x][y]) cold[x][y] = 0;
														else cold[x][y] -= remove;
														if(cold[x][y] <= 0) {
															returnTileBack(tile, x, y);
														}
													}
												}
											}
										}
									}
								}
							}
							// world.tile(tileX, tileY).floor().emitLight || 
//							Log.info(world.tile(tileX, tileY).block().name + ": " + world.tile(tileX, tileY).block().lightRadius);
							/*
							if((world.tile(tileX, tileY).block().emitLight && world.tile(tileX, tileY).block() != null)) {
								
							}*/
						}
					}
				} catch (Exception e) {
//					Log.err(e.getMessage());
				}
				
				try {
					Thread.sleep(eventsTPS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			isRunning = false;
		}).start();
	}
	
	private void returnTileBack(Tile tile, int x, int y) {
		tile.setFloorNet(floor[x][y], overlay[x][y]);
		cold[x][y] = 0;
		if(!tile.block().isAir() && !tile.floor().hasBuilding() && !tile.overlay().hasBuilding() && !tile.block().hasBuilding()) {
			tile.setNet(blocks[x][y]);
		}
	}
	
	int width, height;

	private void updateWorld(World world) {
		isLoaded = false;
		
//		world.isGenerating();	
		int w = world.width();
		int h = world.height();
		width = w;
		height = h;

		floor = new Floor[w][h];
		overlay = new Floor[w][h];
		blocks = new Block[w][h];
		cold = new byte[w][h];
		
		StringBuffer infotitle = new StringBuffer();
		final String infoTilteString = "Новогоднее событие!".toUpperCase();
		for (int i = 0; i < infoTilteString.length(); i++) {
			char ch = infoTilteString.charAt(i);
			if(i%2 == 0) {
				infotitle.append("[white]");
			} else {
				if(i%4 == 1) {
					infotitle.append("[red]");
				}
				if(i%4 == 3) {
					infotitle.append("[green]");
				}
			}
			infotitle.append(ch);
		}
		infotitle.append("[sky]\n");
		
        Call.sendMessage(infotitle.toString() 
        		+ "Карта покрылась снегом и льдом Некоторые руды оказались под снегом.\n"
        		+ "Найдите способ растопить снег и лед, и добраться до занесенных снегом руд\n\n"
        		+ "[lightgray](Используйте [gold]/mapinfo[lightgray] для статистики ресурсов)");
		
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				Tile tile = Vars.world.tile(x, y);
				floor[x][y] = tile.floor();
				overlay[x][y] = tile.overlay();
				blocks[x][y] = tile.block();
				cold[x][y] = 0;
				if(!tile.floor().hasBuilding() && !tile.overlay().hasBuilding() && !tile.block().hasBuilding() && !tile.block().emitLight && !tile.overlay().emitLight && !tile.floor().emitLight) { // !tile.getClass().equals(SpawnBlock.class)
					if(!tile.block().isAir()) {
						if(tile.block().name.indexOf("boulder") != -1) {
							tile.setNet(Blocks.snowBoulder);
							cold[x][y] = 100;
						} else {
							tile.setNet(Blocks.snowWall);
							cold[x][y] = 100;
						}
					}
					boolean isIce = false;
					Floor floor = tile.floor();
					Floor overlay = tile.overlay();
					
					if(tile.overlay().liquidDrop == Liquids.water) {
						overlay = (Floor) Blocks.ice;
						overlay.isLiquid = true;
						tile.setFloorNet(floor);
						tile.setOverlay(overlay);
						cold[x][y] = 100;
						isIce = true;
					}
					if(tile.floor().liquidDrop == Liquids.water) {
						floor = (Floor) Blocks.ice;
						floor.isLiquid = true;
						cold[x][y] = 100;
						tile.setFloor(floor);
						tile.setOverlay(overlay);
						isIce = true;
					}
					if(overlay != Blocks.ice) {
						if(overlay.itemDrop != null) {
							if(Math.random() > .5) {
								overlay = (Floor) Blocks.air;
							}
						}
					}
					if(!isIce) {
						if(floor != Blocks.space && overlay != Blocks.space) {
							if(floor == Blocks.sand || floor == Blocks.darksand) {
								tile.setFloor((Floor) Blocks.iceSnow);
								tile.setOverlay(overlay);
								cold[x][y] = 110;
							} else {
								tile.setFloor((Floor) Blocks.snow);
								tile.setOverlay(overlay);
								cold[x][y] = 120;
							}
						}
					}
				}
			}
		}
		
		lastmapname = Vars.state.map.name();
		isLoaded = true;
	}
}
