package example.events;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.entities.EntityGroup;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.Teams.TeamData;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Entityc;
import mindustry.gen.Groups;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import mindustry.gen.Puddle;
import mindustry.gen.Unit;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.SteamVent;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Structs;
import arc.util.noise.Simplex;
import example.GameWork;
import example.events.SpaceDangerEvent.WarmTileEvent;

import static mindustry.Vars.*;

public class NewYearEvent extends ServerEvent {

	/**
	 * TODO:
	 * Cryofluid filling
	 * cryofluid cooling
	 * 
	 * Remove rules
	 */
	
	/**
	 * Old blocks
	 */
	private Floor[][] floor, overlay;
	private Block[][] blocks;

	private byte[][] cold; // 0 - normal; 100 - hot 

	private int[][] cooldown; // it's relay cool
	
	public NewYearEvent() {
		super("New year");
		color = "sky";
	}

	@Override
	public void init() {
		Events.on(WarmTileEvent.class, e -> {
			if(cold == null) return;
			if(e.x < 0) return;
			if(e.y < 0) return;
			if(e.x >= cold.length) return;
			if(e.y >= cold[e.x].length) return;
			cold[e.x][e.y] = 0;
			if(e.block != null) blocks[e.x][e.y] = e.block;
			if(e.floor != null) floor[e.x][e.y] = (Floor) e.floor;
			if(e.overlay != null) overlay[e.x][e.y] = (Floor) e.overlay;
		});
	}
	
	int updates = 0;
	private boolean[][] puddles;
	
	int lastEnemiesCount = 0;
	int eliteId = 0;
	boolean eliteSelected = false;
	float eliteSpawnX = 0, eliteSpawnY = 0;
	int eliteLastX = 0, eliteLastY = 0;
	
	int containerX = -1, containerY = -1;
    
	@Override
	public void update() {
		updates++;
		
		if(needElite) {
			if(lastEnemiesCount == 0 && Vars.state.enemies > 0 && !eliteSelected) {
				Unit elite = Vars.state.rules.waveTeam.data().units.random();
				if(elite != null) {
					eliteId = elite.id;
					elite.apply(StatusEffects.overclock, 60*60*60*60);
					elite.apply(StatusEffects.overdrive, 60*60*60*60);
					eliteSpawnX = elite.x();
					eliteSpawnY = elite.y();
					
					eliteSelected = true;
					needElite = false;
				}
			}
		}
		lastEnemiesCount = Vars.state.enemies;
		
		if(updates%10 == 0) {
			if(containerX != -1 && containerY != -1) {
				Tile t = Vars.world.tile(containerX, containerY);
				boolean reset = true;
				if(t != null) {
					if(t.block() instanceof ConstructBlock) {
						t.setNet(Blocks.air);
						Call.effect(Fx.blastsmoke, t.getX(), t.getY(), 1, Color.white);
					} else if(t.block() == Blocks.container) {
						if(t.build != null) {
							if(t.build.items != null) {
								if(t.build.health < t.build.maxHealth) {
									t.build.heal(t.build.maxHealth());
								}
								if(t.build.items.any()) {
									reset = false;
								} else {
									t.setNet(Blocks.air);
									Call.effect(Fx.blastsmoke, t.getX(), t.getY(), 1, Color.white);
								}
							}
						}
					}
				} 
				if(reset) {
					containerX = containerY = -1;
				}
			}
		}
		
		if(updates%60 == 0) {
			puddles = new boolean[Vars.world.width()][Vars.world.height()];
			
			if(eliteSelected) {
				Unit elite = Groups.unit.getByID(eliteId);
				if(elite == null) {
					eliteSelected = false;
					needElite = true;
					Call.hideHudText();
					// TODO: falling block effect
					Call.logicExplosion(Vars.state.rules.defaultTeam,
							eliteLastX*Vars.tilesize, eliteLastY*Vars.tilesize,
							Vars.tilesize*10, 10e5f, true, true, true);
					if(Build.validPlace(Blocks.container, Vars.state.rules.defaultTeam, eliteLastX, eliteLastY, 0)) {
						if(containerX != -1 && containerY != -1) {
							Tile t = Vars.world.tile(containerX, containerY);
							if(t != null) {
								if(t.block() == Blocks.container) {
									t.setNet(Blocks.air);
									Call.effect(Fx.blastsmoke, t.getX(), t.getY(), 1, Color.white);
								}
							}
							containerX = containerY = -1;
						}
						Vars.world.tile(eliteLastX, eliteLastY).setNet(Blocks.container, Vars.state.rules.defaultTeam, 0);
						Building building = Vars.world.build(eliteLastX, eliteLastY);
						if(building != null) {
							if(building.items != null) {
								for (int i = 0; i < award.length; i++) {
									if(award[i] <= 0) continue;
									Call.setItem(building, Vars.content.item(i), award[i]);
//									building.items.add(Vars.content.item(i), award[i]);
								}
							}
							containerX = eliteLastX;
							containerY = eliteLastY;
							building.configure(building.config());
						} else {
							Call.sendMessage("ой");
						}
					} else {
						CoreBuild core = Vars.state.rules.defaultTeam.core();
						if(core != null) {
							StringBuilder awardMsg = new StringBuilder("[gold]Элитный враг побежден, вы получили подарок: ");
							for (int i = 0; i < award.length; i++) {
								if(award[i] <= 0) continue;
								if(core.items == null) break;
								core.items.add(Vars.content.item(i), award[i]);
								awardMsg.append("[white] " + Vars.content.item(i).emoji() + " [#" + Vars.content.item(i).color + "]" +  award[i]);
							}
							Call.sendMessage(awardMsg.toString());
						}
					}
				} else {
					if(elite.within(eliteSpawnX, eliteSpawnY, Vars.state.rules.enemyCoreBuildRadius/2f)) {
						elite.healthMultiplier(10e5f);
						elite.heal(elite.maxHealth);
						elite.shield(elite.maxHealth/2f);
						elite.apply(StatusEffects.invincible, 60*5);
						Call.setHudText(elite.type.emoji() + "[gold] Элитный враг: " + Iconc.statusShielded + "[lightgray] <неуязвим>");
					} else {
						Call.setHudText(elite.type.emoji() + "[gold] Элитный враг: [lime]" + elite.health);
						elite.healthMultiplier(2);
						elite.unapply(StatusEffects.invincible);
						elite.heal(elite.maxHealth/20f);
					}
					if(elite.hasEffect(StatusEffects.freezing)) {
						elite.heal(elite.maxHealth/10f);
					}
					elite.unapply(StatusEffects.unmoving);
					elite.unapply(StatusEffects.freezing);
					elite.unapply(StatusEffects.burning);
					elite.unapply(StatusEffects.melting);
					elite.unapply(StatusEffects.wet);
					elite.unapply(StatusEffects.tarred);
					elite.unapply(StatusEffects.sporeSlowed);
					eliteLastX = elite.tileX();
					eliteLastY = elite.tileY();
					
					Call.effect(Fx.blastsmoke, elite.x(), elite.y(), 1, Color.white);
				}
			}
		}
		
		for (int i = 0; i < 10; i++) {
			Puddle random = random(Groups.puddle);
			if(random == null) break;
			
			float t = random.liquid.temperature;
			if(t < .5f) {
				puddles[random.tileX()][random.tileY()] = true;
				int amount = (int) (100*t); // 0 to 50
				if(amount == 0) continue;
				if(updates%100/amount == 0)
				addTemperature(amount, random.tileX(), random.tileY());
			} else {
				int amount = (int) (100*(t-.5f)); // 0 to 50
				if(amount == 0) continue;
				if(updates%100/amount == 0)
				addTemperature(-amount, random.tileX(), random.tileY());
			}
		}
		
		TeamData data = Vars.state.rules.defaultTeam.data();
		
		Groups.player.each(e -> {
			if(e.buildOn() == null) {
				if(e.tileOn() == null) return;
				if(e.tileOn().build != null) updateBuilding(e.tileOn().build);
				return;
			}
			updateBuilding(e.buildOn());
		});
		
		for (int j = 0; j < 100; j++) {
			updateBuilding(data.buildings.random());
		}
		
		for (int i = 0; i < Groups.unit.size(); i++) {
			Unit unit = Groups.unit.index(i);
			if(unit.mineTile != null) {
				addTemperature(-100, unit.mineTile.x, unit.mineTile.y);
				continue;
			}
			if(!unit.isFlying()) {
				if(unit.tileOn() != null) {
					Tile tile = unit.tileOn();
					int tileX = unit.tileX();
					int tileY = unit.tileY();
					if(tileX < 0) continue;
					if(tileY < 0) continue;
					if(tileX >= world.width()) continue;
					if(tileY >= world.height()) continue;
					
					if(cold[tileX][tileY] > 50) {
						if(Math.random() < .25) {
							unit.apply(StatusEffects.freezing, 300);
						} else if(Math.random() < 1f/unit.bounds()) {
							unit.apply(StatusEffects.unmoving, 30);
						}
					}

					if(tile.floor().isLiquid || tile.overlay().isLiquid) {
						if(puddles[tileX][tileY]) {
							unit.apply(StatusEffects.freezing, 60*20);
							unit.apply(StatusEffects.unmoving, 60*10);
							puddles[tileX][tileY] = false;
						}
						addTemperature(-100, tileX, tileY);
					} else if(Math.random() > 10f/unit.bounds() && Math.random() < .05){
						if(puddles[tileX][tileY]) {
							unit.apply(StatusEffects.freezing, 60*20);
							unit.apply(StatusEffects.unmoving, 60*10);
							puddles[tileX][tileY] = false;
						}
						addTemperature(-100, tileX, tileY);
					} else {
						continue;
					}

					double randAlign = Math.random()*Math.toRadians(360);
					double randHypot = Math.random()*unit.bounds()/20f;
					int tx = (int) (unit.tileX() + randHypot*Math.cos(randAlign));
					int ty = (int) (unit.tileY() + randHypot*Math.sin(randAlign));

					if(tx < 0) continue;
					if(ty < 0) continue;
					if(tx >= world.width()) continue;
					if(ty >= world.height()) continue;

					if(Vars.world.tile(tx, ty) != null) {
						if(puddles[tileX][tileY]) {
							unit.apply(StatusEffects.freezing, 60*20);
							unit.apply(StatusEffects.unmoving, 60*10);
							puddles[tileX][tileY] = false;
						}
						addTemperature(-100, tileX, tileY);
					}
				}
			}
		}
		
		for (int i = 0; i < 5; i++) {
			int randX = Mathf.random(0, Vars.world.width()-1);
			int randY = Mathf.random(0, Vars.world.height()-1);
			Tile tile = Vars.world.tile(randX, randY);
			if(tile == null) continue;
			if(tile.build != null) continue;
			if(!checkCooldown(randX, randY)) continue;
			addTemperature(10 + getNearCold(randX, randY)/8, randX, randY);
		}
	}
	
	public int getNearCold(int x, int y) {
		return getCold(x+1, y) + getCold(x-1, y) + getCold(x, y+1) + getCold(x, y-1);
	}
	
	public byte getCold(int x, int y) {
		if(x < 0 || y < 0 || x >= Vars.world.width() || y >= Vars.world.height()) return 0;
		return cold[x][y];
	}

	private void updateBuilding(Building building) {
		if(building == null) return;
		
		Block type = building.block;
		Tile tile = building.tile();

		float temperature = 0;
		if(building.liquids != null) {
			temperature += building.liquids.get(Liquids.cryofluid) - building.liquids.get(Liquids.slag);
		}

		temperature += building.getPowerProduction()*-60;

		if(building.block.consPower != null) {
			if(building.power != null) {
				temperature += building.block.consPower.requestedPower(building) * building.power.status * -30;
			}
		}
		temperature *= building.timeScale();

		float temperatureAv = Mathf.ceil(temperature/type.size/type.size);

		int offsetx = -(type.size - 1) / 2;
		int offsety = -(type.size - 1) / 2;

		for(int dx = 0; dx < type.size; dx++){
			for(int dy = 0; dy < type.size; dy++){
				int wx = dx + offsetx + tile.x, wy = dy + offsety + tile.y;
				if(Mathf.random()*100 < Math.abs(temperatureAv)) {
					addTemperature(temperatureAv > 0 ? Mathf.ceil(temperatureAv) : Mathf.floor(temperatureAv), wx, wy);
				}
			}
		}
	        
		int range = temperature > 0 ? 
				Mathf.ceil(Mathf.sqrt(Math.abs(temperature*type.size/10))): 
					-Mathf.floor(Mathf.sqrt(Math.abs(temperature*type.size/10)));//(Mathf.sqrt(Math.abs())*type.size);
		range = Math.abs(range);
		range = Math.min(range, type.size*3);

		int d = 1 - (type.size)%2;

		int range2 = range*range;
		
		temperature /= type.size*type.size*2;
		
		temperature = Math.min(temperature, 1000);
		temperature = Math.max(temperature, -1000);

		for (int dy = -range; dy <= range+d; dy++) {
			for (int dx = -range; dx <= range+d; dx++) {
				int wx = dx + tile.x, wy = dy + tile.y;
				float len2 = Mathf.len2(dx-d/2f, dy-d/2f);
				if(len2+1-d >= range2) continue;
				if(len2 == 0) continue;

				if(wx < 0) continue;
				if(wy < 0) continue;
				if(wx >= world.width()) continue;
				if(wy >= world.height()) continue;

				float t = (temperature*(range2-len2)/range2);
				if(Mathf.random()*100 < Math.abs(t)) addTemperature(GameWork.roundMax(t/range2), wx, wy);
			}
		}
		
	}
	
	private boolean checkCooldown(int x, int y) {
		return updates - cooldown[x][y] >= 60*5; // 5 second
	}
	
	private <T extends Entityc> T random(EntityGroup<T> group) {
		if(group.size() == 0) return null;
		return group.index(Mathf.random(group.size() - 1));
	}

	@Override
	public void generateWorld() {
		for (int i = 0; i < kills.length; i++) kills[i] = 0;
		for (int i = 0; i < award.length; i++) award[i] = 0;
		needElite = false;
		eliteSelected = false;

		Floor ice = (Floor) Blocks.ice;
		ice.isLiquid = true;
		ice.liquidDrop = Liquids.water;
		
		int w = world.width();
		int h = world.height();

		floor = new Floor[w][h];
		overlay = new Floor[w][h];
		blocks = new Block[w][h];
		cold = new byte[w][h];
		cooldown = new int[w][h];
		puddles = new boolean[w][h];
		
		StringBuffer infotitle = new StringBuffer();
		final String infoTilteString = "Зимнее событие!".toUpperCase();
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

        Block[] selected = Structs.random(
//        new Block[]{Blocks.sand, Blocks.sandWall},
        new Block[]{Blocks.grass, Blocks.shrubs},
        new Block[]{Blocks.shale, Blocks.shaleWall},
//        new Block[]{Blocks.ice, Blocks.iceWall},
//        new Block[]{Blocks.sand, Blocks.sandWall},
        new Block[]{Blocks.shale, Blocks.shaleWall},
//        new Block[]{Blocks.ice, Blocks.iceWall},
        new Block[]{Blocks.moss, Blocks.sporePine},
        new Block[]{Blocks.dirt, Blocks.dirtWall},
        new Block[]{Blocks.dacite, Blocks.daciteWall}
        );
        Block[] selected2 = Structs.random(
        new Block[]{Blocks.basalt, Blocks.duneWall},
        new Block[]{Blocks.basalt, Blocks.duneWall},
        new Block[]{Blocks.stone, Blocks.stoneWall},
        new Block[]{Blocks.stone, Blocks.stoneWall},
        new Block[]{Blocks.moss, Blocks.sporeWall},
        new Block[]{Blocks.salt, Blocks.saltWall}
        );

        int offset = Mathf.random(100000);
        int s1 = offset;
//      int	s2 = offset + 1, 
        int s3 = offset + 2;
//        double tr1 = Mathf.random(0.65f, 0.85f);
//        double tr2 = Mathf.random(0.65f, 0.85f);
        boolean tendrils = Mathf.chance(0.25);
        boolean tech = Mathf.chance(0.25);
        int secSize = 10;
        
        Block floord = selected[0], walld = selected[1];
        Block floord2 = selected2[0], walld2 = selected2[1];
        
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				Tile tile = Vars.world.tile(x, y);
				floor[x][y] = tile.floor();
				overlay[x][y] = tile.overlay();
				blocks[x][y] = tile.build == null ? tile.block() : null;
				cold[x][y] = 100;
				if(tile.build == null && !(tile.floor() instanceof SteamVent)) {
					freezeTile(tile, false);
				}

				Floor oldFloor = floor[x][y];
				Block oldBlock = blocks[x][y];
				boolean defFloor = false, defBlock = false;
				if(floor[x][y] == Blocks.ice) {
					if(tendrils) {
						floor[x][y] = (Floor) Blocks.darksandWater;
					} else {
						floor[x][y] = (Floor) Blocks.sandWater;
					}
					defFloor = true;
				}
				if(floor[x][y] == Blocks.iceSnow) {
					floor[x][y] = (Floor) Blocks.sand;
					defFloor = true;
				}
				float noiseS3 = Simplex.noise2d(s3, 3, 0.5, 1/20.0, x, y);
				if(floor[x][y] == Blocks.snow) {
					floor[x][y] = (Floor) (noiseS3 > .5f ? floord : floord2);
	                if(tech){
	                    int mx = x % secSize, my = y % secSize;
	                    int sclx = x / secSize, scly = y / secSize;
	                    if(Simplex.noise2d(s1, 2, 1f / 10f, 0.5f, sclx, scly) > 0.4f && (mx == 0 || my == 0 || mx == secSize - 1 || my == secSize - 1)){
	                    	floor[x][y] = (Floor) Blocks.darkPanel3;
	                        if(Mathf.dst(mx, my, secSize/2, secSize/2) > secSize/2f + 1){
	                        	floor[x][y] = (Floor) Blocks.darkPanel4;
	                        }
	                    }
	                }
					defFloor = true;
				}
				if(blocks[x][y] == Blocks.snowWall || blocks[x][y] == Blocks.iceWall) {
					blocks[x][y] = (noiseS3 > .5f ? walld : walld2);
	                if(tech && Mathf.chance(0.7)){
	                	blocks[x][y] = Blocks.darkMetal;
	                }
					defBlock = true;
				}
				if(blocks[x][y] == Blocks.snowBoulder || blocks[x][y] == Blocks.snowPine) {
					blocks[x][y] = Blocks.air;
					defBlock = true;
				}
				if(defFloor) {
					tile.setFloorUnder(oldFloor);
				}
				if(defBlock) {
					if(oldBlock != null) {
						tile.setBlock(oldBlock);
					}
				}
			}
		}
	}

	@Override
	public void announce() {
		Call.announce("[royal]Новогднее событие начнется на следующей карте!");
		Call.sendMessage("[sky]Новогднее событие начнется на следующей карте!");		
	}

	private void addTemperature(int temperature, int x, int y) {
		if(x < 0 || y < 0) return;
		if(x >= Vars.world.width() || y >= Vars.world.height()) return;
		int value = cold[x][y];
		Tile tile = Vars.world.tile(x, y);
		
		if(value + temperature >= 100) {
			if(cold[x][y] < 100) {
				if(!checkCooldown(x, y)) {
					cold[x][y] = 99;
					return;
				}
				freezeTile(tile, true);
			}
			cold[x][y] = 100;
			return;
		}
		
		if(value + temperature <= 0) {
			if(cold[x][y] > 0) {
				unfreezeTile(tile, true);
				return;
			}
			cooldown[x][y] = updates;
			cold[x][y] = 0;
			return;
		}
		cold[x][y] += temperature;
	}

	public void setBlock(Tile tile, Block block, boolean net) {
		if(block == null) return;
		if(net) tile.setNet(block);
		else tile.setBlock(block);
	}

	public void setFloor(Tile tile, Block floor, Floor overlay, boolean net) {
		if(overlay == null) {
			if(net) tile.setFloorNet(floor);
			else tile.setFloor((Floor) floor);
		} else {
			if(net) tile.setFloorNet(floor, overlay);
			else {
				tile.setFloor((Floor) floor);
				tile.setOverlay(overlay); 
			}
		}
	}

	private void unfreezeTile(Tile tile, boolean net) {
		int x = tile.x;
		int y = tile.y;

		cold[x][y] = 0;
		if(floor[x][y] != tile.floor() && floor[x][y].liquidDrop == Liquids.water) {
			Building building = tile.build;
			if(!(building instanceof CoreBuild)) {
				if(building != null) {
					building.damage(10);
					if(building.health() > 0) {
						cold[x][y] = 1;
						return;
					}
				}
			}
		}
		tile.setFloorNet(floor[x][y], overlay[x][y]);
		
		if(tile.build == null) {
			if(blocks[x][y] != null) {
				tile.setNet(blocks[x][y]);
			}
		}

		cooldown[x][y] = updates;
		if(net) Call.effect(Fx.reactorsmoke, x*Vars.tilesize, y*Vars.tilesize, 1, Color.white);
	}
	
	private void freezeTile(Tile tile, boolean net) {
		int x = tile.x;
		int y = tile.y;

		if(tile.block().emitLight) return;
		if(tile.floor().emitLight) return;
		if(tile.overlay().emitLight) return;
		if(tile.floor() == Blocks.air) return;
		if(tile.floor() == Blocks.empty) return;
		if(tile.floor() == Blocks.space) return;

		if(!tile.block().isAir() && tile.build == null) {
			if(tile.block().name.indexOf("boulder") != -1) {
				setBlock(tile, Blocks.snowBoulder, net);
				cold[x][y] = 100;
			} else {
				setBlock(tile, Blocks.snowWall, net);
				cold[x][y] = 100;
			}
		}
		
		Floor floor = tile.floor();
		Floor overlay = tile.overlay();

		if(floor.liquidDrop == Liquids.water) {
			floor.supportsOverlay = false;
			floor.speedMultiplier = 2f;
			floor.needsSurface = true;
			
			setFloor(tile, Blocks.ice, overlay, net);
			
			cold[x][y] = 100;
			if(net) Call.effect(Fx.pulverize, x*Vars.tilesize, y*Vars.tilesize, 1, Color.white);
			return;
		}
		
		if(overlay.itemDrop != null) {
			if(Math.random() > .5) {
				overlay = (Floor) Blocks.air;
			}
		}
		if(floor == Blocks.sand || floor == Blocks.darksand) {
			setFloor(tile, Blocks.iceSnow, overlay, net);
		} else {
			setFloor(tile, Blocks.snow, overlay, net);
		}
		cold[x][y] = 100;
		if(net) Call.effect(Fx.pulverize, x*Vars.tilesize, y*Vars.tilesize, 1, Color.white);
	}
	
	@Deprecated
	protected void $freezeTile(Tile tile) {
		int x = tile.x;
		int y = tile.y;

		if(tile.build != null) return;
		if(tile.block().emitLight) return;
		if(tile.floor().emitLight) return;
		if(tile.overlay().emitLight) return;
		
		if(!tile.block().isAir()) {
			if(tile.block().name.indexOf("boulder") != -1) {
				tile.setBlock(Blocks.snowBoulder);
				cold[x][y] = 100;
			} else {
				tile.setBlock(Blocks.snowWall);
				cold[x][y] = 100;
			}
		}
		
		boolean isIce = false;
		Floor floor = tile.floor();
		Floor overlay = tile.overlay();

		if(tile.overlay().liquidDrop == Liquids.water) {
			overlay = (Floor) Blocks.ice;
			overlay.isLiquid = true;
			overlay.liquidDrop = Liquids.water;
			floor.supportsOverlay = false;
			floor.speedMultiplier = 2f;
			floor.needsSurface = true;
			tile.setFloor(floor);
			tile.setOverlay(overlay);
			cold[x][y] = 100;
			isIce = true;
		}
		if(tile.floor().liquidDrop == Liquids.water) {
			floor = (Floor) Blocks.ice;
			floor.isLiquid = true;
			floor.liquidDrop = Liquids.water;
			floor.supportsOverlay = false;
			floor.speedMultiplier = 2f;
			floor.needsSurface = true;
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

	int[] kills = new int[Vars.content.units().size];
	int[] award = new int[Vars.content.items().size];
	boolean needElite = false;
	
	@Override
	public void unitDestroy(UnitDestroyEvent e) {
		if(e.unit == null) return;
		if(e.unit.team == Vars.state.rules.defaultTeam) return;
		
		int id = e.unit.type.id;
		this.kills[id]++;
		int kills = this.kills[id];
		if(kills <= 0) return;
		
		if(Mathf.random()*kills < 10f) return;
		
		ItemStack requirements = Structs.random(e.unit.type.getTotalRequirements());
		if(requirements == null) return;
		
		int iid = requirements.item.id;
		award[iid] += Mathf.random(requirements.amount);
//		Call.sendMessage("<buffer> " + award[iid] + " " + requirements.item.emoji());
		if(award[iid] >= 300 || award[iid] >= Vars.state.wave) {
			needElite = true;
		}
		this.kills[id] = 0;
	}
	
	@Override
	public void trigger(Player player, String... args) {
		if(args.length == 1) {
			if(args[0].equalsIgnoreCase("elite")) {
				award[Vars.content.items().random().id] = 300;
				needElite = true;
				player.sendMessage("[gold]Готово");
			}
		}
	}
}
