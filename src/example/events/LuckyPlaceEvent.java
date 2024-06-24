package example.events;

import static mindustry.Vars.*;

import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Log;
import example.Emoji;
import example.GameWork;
import example.Work;
import example.events.SpaceDangerEvent.MeteoriteFallEvent;
import example.events.SpaceDangerEvent.WarmTileEvent;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.game.Team;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.DepositEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.modules.ItemModule;

public class LuckyPlaceEvent extends ServerEvent {

	public static int any = -1; // Event type for any planet
	public static int serpulo = 0; // Event type for SERPULO
	public static int erekir = 1; // Event type for EREKIR

	private static final Point2[] nearBlocks = new Point2[] {
			new Point2(0, -1),new Point2(1, -1), // Bottom
			new Point2(0, +2),new Point2(1, +2), // Top
			new Point2(-1, 0),new Point2(-1, 1), // Left
			new Point2(+2, 0),new Point2(+2, 1)  // Right
	};
	
	private static final Point2[] freeSpace = new Point2[] {
			new Point2(0, 0),
			new Point2(-1, -2),new Point2(+1, -2),
			new Point2(-1, +2),new Point2(+1, +2),
			new Point2(+2, +1),new Point2(+2, -1),
			new Point2(-2, +1),new Point2(-2, -1)
	};

	public Point2 luckySource = null;
	public boolean isLuckActivated = false;
	private WarmTileEvent warmTileEvent = new WarmTileEvent(0, 0);
	
	public LuckyPlaceEvent() {
		super("lucky_place");
		color = "yellow";
	}

	@Override
	public void init() {
		Events.on(MeteoriteFallEvent.class, e -> {
			if(!isLuckActivated) return;
			if(luckySource.dst(e.x, e.y) > e.radius) return;
			if(Mathf.random(0, 100) > e.level) {
				Call.sendMessage("[red]Метеорит принес неудачу!");
				Call.announce("[red]Метеорит принес неудачу!");
				int rays = Mathf.ceil(e.radius/2f)*2;
				int dAngle = Mathf.floor(360f/rays);
				int x = luckySource.x*tilesize;
				int y = luckySource.y*tilesize;
				boolean id = false;
				for (int angle = 0; angle < 360; angle+=dAngle) {
					id = !id;
					if(id) Vars.content.bullet(GameWork.bulletAfflictId).createNet(Team.crux, x, y, angle, 5_000_000, e.radius/200f, 25);
					else Vars.content.bullet(GameWork.bulletSmiteId).createNet(Team.crux, x, y, angle, 5_000_000, e.radius/75f, 25);
				}
			} else {
				Call.sendMessage("[yellow]Попался удачный метеорит!");
				Call.announce("[yellow]Попался удачный метеорит!");
				int count = Mathf.ceil((e.radius-10)/5f);
				boolean[] types = new boolean[count];
				for (int i = 0; i < types.length; i++) types[i] = true;
				randomMiniEvent(any, types);
			}
			createSpaceAround();
			createAirAround(); 
		});
	}
	
	@Override
	public void playerJoin(PlayerJoin e) {
		if(e.player == null) return;
		e.player.sendMessage(getInfo());
	}

	@Override
	public void announce() {
		Call.sendMessage("[yellow]Удачное событие начнется на следующей карте!");
	}
	
	private String getInfo() {
		if(isLuckActivated) return "[yellow]Заполните [lightgray]контейнер [yellow]всеми видами предметов на максимум";
		if(Vars.state.rules != null) {
			if(Vars.state.rules.hiddenBuildItems != null) {
				if(Vars.state.rules.hiddenBuildItems.contains(Items.copper)) {
					return "[yellow]Найдите место повышенной удачи, и постройте там [lightgray]холст[yellow]\nА затем окружите его 5 разными видами больших стен";
				}
			}
		}
		return "[yellow]Найдите место повышенной удачи, и постройте там [lightgray]копейщик[yellow]\nА затем окружите его 6 разными видами больших стен";
	}
	
	
	
	private int updates = 0;
	private int lastWave = 0;
	private boolean isOrePlaced = false;
	
	@Override
	public void update() {
		if(!isGenerated) return;
		
		if(!isOrePlaced) {
			for (int i = 0; i < Groups.player .size(); i++) {
				Player player = Groups.player.index(i);
				for (int j = 0; j < player.team().cores().size; j++) {
					CoreBuild core = player.team().cores().get(j);
					if(core != null) {

						int x = core.tileX();
						int y = core.tileY();

						for (int yy = 0; yy <= 5; yy++) {
							for (int xx = 0; xx <= 5; xx++) {
								Tile tile = world.tile(x-xx, y-yy);
								if(tile == null) continue;
								tile.setFloorNet(tile.floor(), Blocks.oreScrap);
							}
						}
						isOrePlaced = true;
						return;
					}
				}
			}
		}
		
		if(updates == 0) {
			for (int i = 0; i < Groups.player .size(); i++) {
				Player player = Groups.player.index(i);
				for (int j = 0; j < player.team().cores().size; j++) {
					CoreBuild core = player.team().cores().get(j);
					if(core != null) {
						int x = core.tileX();
						int y = core.tileY();
						for (int yy = 0; yy <= 5; yy++) {
							for (int xx = 0; xx <= 5; xx++) {
								Tile tile = world.tile(x-xx, y-yy);
								if(tile == null) continue;
								tile.setFloorNet(tile.floor(), Blocks.oreScrap);
							}
						}
						return;
					}
				}
			}
		}
		updates++;
		
		if(luckySource == null) {
			Seq<Point2> points = new Seq<>();
			Team defaultTeam = Vars.state.rules.defaultTeam;
			Seq<CoreBuild> cores = defaultTeam.cores();
			Seq<Tile> spawns = Vars.spawner.getSpawns();
			float crad = Vars.state.rules.enemyCoreBuildRadius/tilesize;
			float srad = Vars.state.rules.enemyCoreBuildRadius/tilesize;
			for (int y = 1; y < world.height()-1; y++) {
				for (int x = 1; x < world.width()-1; x++) {
					boolean can = true;
					for (int i = 0; i < cores.size; i++) {
						int cx = cores.get(i).tileX();
						int cy = cores.get(i).tileY();
						
						float hypot = Mathf.len(cx-x, cy-y);
						if(hypot <= crad) {
							can = false;
							break;
						}
					}
					if(!can) continue;
					for (int i = 0; i < spawns.size; i++) {
						int cx = spawns.get(i).centerX();
						int cy = spawns.get(i).centerY();
						
						float hypot = Mathf.len(cx-x, cy-y);
						if(hypot <= srad) {
							can = false;
							break;
						}
					}
					if(!can) continue;
					if(world.tile(x, y).block() == Blocks.air && Blocks.lancer.canPlaceOn(world.tile(x, y), defaultTeam, 0)) {
						for (int i = 0; i < freeSpace.length; i++) {
							int xx = x + freeSpace[i].x;
							int yy = y + freeSpace[i].y;
							if(world.tile(xx, yy) == null) {
								can = false;
								break;
							}
							if(world.tile(xx, yy).block() != Blocks.air) {
								can = false;
								break;
							}
							if(world.tile(xx, yy).floor().isDeep()) {
								can = false;
								break;
							}
							if(!world.tile(xx, yy).floor().placeableOn) {
								can = false;
								break;
							}
							if(world.tile(xx, yy).floor().isLiquid) {
								can = false;
								break;
							}
							if(!Blocks.copperWallLarge.canPlaceOn(world.tile(xx, yy), defaultTeam, 0)) {
								can = false;
								break;
							}
						}
					} else {
						can = false;
					}
					if(!can) continue;
					points.add(new Point2(x, y));
				}
			}
			if(points.size != 0) {
				luckySource = points.random();
				Log.info("luckySource: " + luckySource.x + ";" + luckySource.y);
				createAirAround();
			}
		} else {
			if(isLuckActivated) {
				updateMiniEvent();
				if(updates%60 == 0) {
					int x = luckySource.x;
					int y = luckySource.y;
					if(world.tile(x, y).block() == Blocks.container) {
						updateContainer();
					} else {
						Call.effect(Fx.generate, 
								(float)(x*tilesize + tilesize/2f), (float)(y*tilesize + tilesize/2), 4f * 8f,
								Color.HSVtoRGB((updates/60*10f)%360, 25, 100));
						if(lastWave != state.wave) {
							lastWave = state.wave;
							world.tile(x, y).setNet(Blocks.container, Team.sharded, 0);
						}
					}
				}
			} else {
				if(needCheck && updates%60 == 0) {
					int x = luckySource.x;
					int y = luckySource.y;

					if(world.tile(x, y).block() == Blocks.lancer || world.tile(x, y).block() == Blocks.canvas) {
						Call.effect(Fx.dynamicSpikes, 
								(float)(x*tilesize + tilesize/2f), (float)(y*tilesize + tilesize/2), 4f * 8f,
								Color.HSVtoRGB((updates/60*10f)%360, 25, 100));
					} else if(updates%60 == 0){
						Call.effect(Fx.generate, 
								(float)(x*tilesize + tilesize/2f), (float)(y*tilesize + tilesize/2), 4f * 8f,
								Color.HSVtoRGB((updates/60*10f)%360, 25, 100));
						return;
					}

					boolean copperWall = false, titaniumWall = false, plastaniumWall = false,
							thoriumWall = false, phaseWall = false, surgeWall = false;
					
					boolean berylliumWall = false, tungstenWall = false, reinforcedSurgeWall = false,
							carbideWall = false, shieldedWall = false;

					for (int i = 0; i < nearBlocks.length; i++) {
						int xx = x + nearBlocks[i].x;
						int yy = y + nearBlocks[i].y;
						Block b = world.tile(xx, yy).block();
						if(b == Blocks.copperWallLarge) copperWall = true;
						if(b == Blocks.titaniumWallLarge) titaniumWall = true;
						if(b == Blocks.plastaniumWallLarge) plastaniumWall = true;
						if(b == Blocks.thoriumWallLarge) thoriumWall = true;
						if(b == Blocks.phaseWallLarge) phaseWall = true;
						if(b == Blocks.surgeWallLarge) surgeWall = true;

						if(b == Blocks.berylliumWallLarge) berylliumWall = true;
						if(b == Blocks.tungstenWallLarge) tungstenWall = true;
						if(b == Blocks.reinforcedSurgeWallLarge) reinforcedSurgeWall = true;
						if(b == Blocks.carbideWallLarge) carbideWall = true;
						if(b == Blocks.shieldedWall) shieldedWall = true;
					}

					if(copperWall && titaniumWall && plastaniumWall 
							&& thoriumWall && phaseWall && surgeWall) {
						isLuckActivated = true;
						Call.warningToast(0, "[gold]Алтарь удачи активирован!");
						createSpaceAround();
					}

					if(berylliumWall && tungstenWall && reinforcedSurgeWall 
							&& carbideWall && shieldedWall) {
						isLuckActivated = true;
						Call.warningToast(0, "[gold]Алтарь удачи активирован!");
					}
				}
			}
		}
	}
	
	private void createAirAround() {
		for (int i = 0; i < freeSpace.length; i++) {
			int xx = luckySource.x + freeSpace[i].x;
			int yy = luckySource.y + freeSpace[i].y;
			world.tile(xx, yy).setNet(Blocks.air);
			world.tile(xx+1, yy).setNet(Blocks.air);
			world.tile(xx, yy+1).setNet(Blocks.air);
			world.tile(xx+1, yy+1).setNet(Blocks.air);
		}		
	}

	private void createSpaceAround() {
		for (int i = 0; i < freeSpace.length; i++) {
			int xx = luckySource.x + freeSpace[i].x;
			int yy = luckySource.y + freeSpace[i].y;
			world.tile(xx, yy).setFloorNet(Blocks.space);
			world.tile(xx+1, yy).setFloorNet(Blocks.space);
			world.tile(xx, yy+1).setFloorNet(Blocks.space);
			world.tile(xx+1, yy+1).setFloorNet(Blocks.space);
			warmTile(null, Blocks.space, null, xx, yy);
			warmTile(null, Blocks.space, null, xx+1, yy);
			warmTile(null, Blocks.space, null, xx, yy+1);
			warmTile(null, Blocks.space, null, xx+1, yy+1);
		}		
	}
	
	private void warmTile(Block block, Block floor, Block overlay, int x, int y) {
		if(isOut(x, y)) return;
		warmTileEvent.block = block;
		warmTileEvent.floor = floor;
		warmTileEvent.overlay = overlay;
		warmTileEvent.x = x;
		warmTileEvent.y = y;
		Events.fire(warmTileEvent);
	}
	
	private boolean isOut(int x, int y) {
		if(x < 0) return true;
		if(y < 0) return true;
		if(x >= world.width()) return true;
		if(y >= world.height()) return true;
		return false;
	}

	@Override
	public void deposit(DepositEvent e) {
		if(isLuckActivated) {
			if(e.tile.tileX() == luckySource.x && e.tile.tileY() == luckySource.y) {
				updateContainer();
			}
		}
	}
	
	private void updateContainer() {
		ItemModule module = world.tile(luckySource.x, luckySource.y).build.items;
		boolean isSerpuloFull = true;
		for (int i = 0; i < Items.serpuloItems.size; i++) {
			Item item = Items.serpuloItems.get(i);
			int count = module.get(item);
			
			if(count != Blocks.container.itemCapacity) {
				isSerpuloFull = false;
				break;
			}
		}

		boolean isErekirFull = true;
		for (int i = 0; i < Items.erekirItems.size; i++) {
			Item item = Items.erekirItems.get(i);
			if(item.hidden) continue;
			int count = module.get(item);
			
			if(count != Blocks.container.itemCapacity) {
				isErekirFull = false;
				break;
			}
		}
		
		if(isSerpuloFull) {
			randomMiniEvent(serpulo);
			world.tile(luckySource.x, luckySource.y).setNet(Blocks.air);
		}

		if(isErekirFull) {
			randomMiniEvent(erekir);
			world.tile(luckySource.x, luckySource.y).setNet(Blocks.air);
		}
	}
	
	int count = 0;

	private final MiniEvent[] positive = new MiniEvent[] {
			new MiniEvent("solarMultiplierUp", "Cолнечная энергии увеличена на [green]0.5"),
			new MiniEvent("dropZoneRadiusDown", "Уменьшение радиуса зоны высадки в [green]1.21[] раз"),
			new MiniEvent("buildSpeedMultiplierUp", "Увеличение скорости строительства в [green]1.21[] раз"),
			new MiniEvent("unitBuildSpeedMultiplierUp", "Увеличение скорости производства юнитов в [green]1.21[] раз"),
			new MiniEvent("core", "Ядро вместо хранилища (если есть)"),
			new MiniEvent("spawnCoreUnit", "Призывает \"юнита из ядра\""),
			new MiniEvent("positiveEffects", "Накладывает вечные [green]положительные[] эффекты на случайных юнитов (включая игроков)"),
			new MiniEvent("biomeMoss", "Создает островок из мха вокруг игроков"),
			new MiniEvent("biomeSnow", "Создает островок из снега и льда вокруг игроков"),
			new MiniEvent("moreitems", "<custom info> Увеличивает количество определенных предметов в [green]2[] раза"),
			new MiniEvent("updateBlocks", "<custom info> Улучшает блоки"),
			new MiniEvent("superMine", Emoji.shockMine + " постепенно взрываются, ломая рельеф (минимум 100 мин)"),
	};
	private final MiniEvent[] negative = new MiniEvent[] {
			new MiniEvent("solarMultiplierDown", "Cолнечная энергия понижена на [red]0.25"),
			new MiniEvent("dropZoneRadiusUp", "Увеличение радиуса зоны высадки в [red]1.1[] раз"),
			new MiniEvent("buildSpeedMultiplierDown", "Уменьшение скорости строительства в [red]1.1[] раз"),
			new MiniEvent("unitBuildSpeedMultiplierDown", "Уменьшение скорости производства юнитов в [red]1.1[] раз"),
			new MiniEvent("angrySpark", "Злая искра, которая уничожает электоросеть"),
//			new MiniEvent("itemsEater", "Пожиратель предметов, уничтожающий все ресуры на блоках, уязвим к свойствам ресурсов"),
			new MiniEvent("teamUnitDestroyed", "Случайный союзный юнит уничтожен (включая игроков)"),
			new MiniEvent("itemsNegativeEffectsFromItems", "Накладывает вечные [red]негативные[] эффекты на юнитов в зависимости от переносимых предметов"),
			new MiniEvent("lessitems", "<custom info> Уменьшает количество определенных предметов в [red]1.5[] раз"),
			new MiniEvent("imposterTurrets", "[red]0-5%[] турелей становится вражескими"),
			new MiniEvent("runwave", "Запускает [red]5[] волн разом"),
//			new MiniEvent("imposterUnits", "[red]0-5%[] турелей становится вражескими"),
	};
	
	private void randomMiniEvent(int planet) {
		randomMiniEvent(planet, new boolean[] {true, true, false});
	}
	
	private void randomMiniEvent(int planet, boolean[] isPositive) {
		count++;
		StringBuilder info = new StringBuilder();
		
		MiniEvent[] events = new MiniEvent[isPositive.length];//{randomPositiveEvent(), randomPositiveEvent(), randomNegativeEvent()};
		for (int i = 0; i < isPositive.length; i++) {
			events[i] = isPositive[i] ? randomPositiveEvent() : randomNegativeEvent();
		}
		
		
		for (int i = 0; i < events.length; i++) {
			events[i].run(planet);
			if(i != 0) info.append('\n');
			info.append(isPositive[i] ? "[green]\ue800" : "[red]\ue815");
			info.append(" [white]");
			info.append(events[i].info);
		}
		createSpaceAround();
		createAirAround();
		
//		state.rules.syn
		Call.setRules(state.rules);
		
		Call.infoMessage(info.toString());
		
	}

	private MiniEvent randomPositiveEvent() {
		return positive[Mathf.random(positive.length-1)];
	}
	
	private MiniEvent randomNegativeEvent() {
		return negative[Mathf.random(negative.length-1)];
	}
	
	class MiniEvent {
		
		String name, info;
		
		public MiniEvent(String name, String info) {
			this.name = name;
			this.info = info;
		}
		
		/**
		 * TODO
		 * /event lucky_place faston
		 * /etrigger event updateBlocks
		 * 
		 * Высадка контейнеров с ресурсами
		 * Emanate
		 */
		
		public void run(int planet) {
			Team defaultTeam = state.rules.defaultTeam;
			
			if(luckySource == null) {
				info = "Источник удачи не найден :(";
				return;
			}
			
			switch (name) {
			case "solarMultiplierUp": 				state.rules.solarMultiplier+=.50f; 				break;
			case "solarMultiplierDown": 			state.rules.solarMultiplier-=.25f; 				break;
			case "dropZoneRadiusUp": 				state.rules.dropZoneRadius *= 1.1f; 			break;
			case "dropZoneRadiusDown":				state.rules.dropZoneRadius /= 1.21f; 			break;
			case "buildSpeedMultiplierUp": 			state.rules.buildSpeedMultiplier *= 1.21f; 		break;
			case "buildSpeedMultiplierDown":		state.rules.buildSpeedMultiplier /= 1.1f; 		break;
			case "unitBuildSpeedMultiplierUp": 		state.rules.unitBuildSpeedMultiplier *= 1.21f; 	break;
			case "unitBuildSpeedMultiplierDown":	state.rules.unitBuildSpeedMultiplier /= 1.1; 	break;
			case "superMine":
				superMines.add(new SuperMine());
				break;
			case "updateBlocks":
				int randomReplacer = Mathf.random(2);
				info = "Заменяет # на @";
				if(randomReplacer == 0) {
					info = info.replace('#', Emoji.conveyor);
					info = info.replace('@', Emoji.titaniumConveyor);
					replacers.add(new Replacer(Blocks.conveyor, Blocks.titaniumConveyor));
				} else if(randomReplacer == 1) {
					info = info.replace('#', Emoji.titaniumConveyor);
					info = info.replace('@', Emoji.duct);
					replacers.add(new Replacer(Blocks.titaniumConveyor, Blocks.duct));
					replacers.add(new Replacer(Blocks.armoredConveyor, Blocks.armoredDuct));
				} else if(randomReplacer == 2) {
					info = info.replace('#', Emoji.plastaniumConveyor);
					info = info.replace('@', Emoji.surgeConveyor);
					replacers.add(new Replacer(Blocks.plastaniumConveyor, Blocks.surgeConveyor));
				} else if(randomReplacer == 3) {
					info = info.replace('#', Emoji.phaseWallLarge);
					info = info.replace('@', Emoji.shieldedWall);
					replacers.add(new Replacer(Blocks.phaseWallLarge, Blocks.shieldedWall));
				}
				break;
			case "runwave":
				for (int i = 0; i < 5; i++) {
					logic.runWave();
				}
				break;
			case "imposterTurrets": // /etrigger event imposterTurrets
				Seq<Building> turrets = new Seq<>();
				for (int i = 0; i < GameWork.serpyloTurrets.length; i++) {
					turrets.add(defaultTeam.data().getBuildings(GameWork.serpyloTurrets[i]));	
				}
				for (int i = 0; i < GameWork.erekirTurrets.length; i++) {
					turrets.add(defaultTeam.data().getBuildings(GameWork.erekirTurrets[i]));	
				}
				if(turrets.size == 0) break;
				int replaceCount = Mathf.ceil(turrets.size*5/100f);
				
				for (int i = 0; i < replaceCount; i++) {
					Building building = turrets.random();
					GameWork.changeBuildingTeam(building, Team.crux);
				}
				
				Team.crux.rules().cheat = true;
				Call.setRules(state.rules);
				break;
			case "moreitems":
				if(defaultTeam.core() == null) break;
				Item randomItem = planet == any || planet == serpulo ? Items.serpuloItems.random() : Items.erekirOnlyItems.random();
				int randomItemCount = defaultTeam.core().items.get(randomItem);
				defaultTeam.core().items.add(randomItem, randomItemCount);
				Work.localisateItemsNames();
				info = "Увеличивает количество [#" + randomItem.color.toString() + "]" + randomItem.localizedName + "[] в [green]2[] раза [lightgray](+" + randomItemCount + ")";
				break;
			case "lessitems":
				if(defaultTeam.core() == null) break;
				randomItem = planet == any || planet == serpulo ? Items.serpuloItems.random() : Items.erekirOnlyItems.random();
				randomItemCount = Mathf.ceil(defaultTeam.core().items.get(randomItem)/3);
				defaultTeam.core().items.remove(randomItem, randomItemCount);
				Work.localisateItemsNames();
				info = "Уменьшает количество [#" + randomItem.color.toString() + "]" + randomItem.localizedName + "[] в [red]1.5[] раз [lightgray](-" + randomItemCount + ")";
				break;
			case "biomeMoss":
				for (int i = 0; i < Groups.player.size(); i++) {
					Player player = Groups.player.index(i);
					int cx = player.tileX();
					int cy = player.tileY();
					int radius = 10;
					for (int dy = -radius; dy <= radius; dy++) {
						for (int dx = -radius; dx <= radius; dx++) {
							int x = cx + dx;
							int y = cy + dy;
							float hypot = Mathf.len(dx, dy);
							if(hypot > radius) continue;
							float k = hypot/radius; // 0 - near center
							Tile tile = world.tile(x, y);
							if(tile == null) continue;
							Block floor = tile.floor();
							Block block = null;
							if(floor == null) continue;
							boolean isCustomFloor = Mathf.random() > k;
							if(isCustomFloor) {
								if(floor.name.indexOf("water") != -1) {
									floor = Blocks.taintedWater;
									if(floor.name.indexOf("deep") != -1) {
										floor = Blocks.deepTaintedWater;
									}
								} else if(hasNearFloor(Blocks.slag, x, y)) {
									floor = Blocks.charr;
								} else if(hasNearFloor(Blocks.tar, x, y)) {
									floor = Blocks.shale;
								} else if(hasNearFloor(Blocks.cryofluid, x, y)) {
									floor = Blocks.dacite;
								} else {
									floor = Blocks.moss;
									isCustomFloor = true;
									if(Mathf.random() > k) {
										floor = Blocks.sporeMoss;
									}
								}
								if(tile.build == null) {
									if(tile.block() != Blocks.air && tile.block() != null) {
										if(tile.block().name.indexOf("boulder") != -1) {
											tile.setNet(Blocks.air);
											block = Blocks.air;
										} else {
											tile.setNet(Blocks.sporeWall);
											block = Blocks.sporeWall;
										}
									}
								}
							}
							warmTile(block, floor, tile.overlay(), x, y);
							tile.setFloorNet(floor, tile.overlay());
						}
					}
				}
				break;
			case "biomeSnow":
				for (int i = 0; i < Groups.player.size(); i++) {
					Player player = Groups.player.index(i);
					int cx = player.tileX();
					int cy = player.tileY();
					int radius = 10;
					for (int dy = -radius; dy <= radius; dy++) {
						for (int dx = -radius; dx <= radius; dx++) {
							int x = cx + dx;
							int y = cy + dy;
							float hypot = Mathf.len(dx, dy);
							if(hypot > radius) continue;
							float k = hypot/radius; // 0 - near center
							Tile tile = world.tile(x, y);
							if(tile == null) continue;
							Block floor = tile.floor();
							if(floor == null) continue;
							boolean isCustomFloor = Mathf.random() > k;
							if(isCustomFloor) {
								if(floor.name.indexOf("water") != -1) {
									floor = Blocks.ice;
								} else if(floor == Blocks.sand || floor == Blocks.darksand) {
									floor = Blocks.iceSnow;
								} else if(hasNearFloor(Blocks.slag, x, y)) {
									floor = Blocks.basalt;
								} else if(hasNearFloor(Blocks.tar, x, y)) {
									floor = Blocks.salt;
								} else if(hasNearFloor(Blocks.magmarock, x, y) || hasNearFloor(Blocks.hotrock, x, y)) {
									floor = Blocks.basalt;
								} else {
									floor = Blocks.snow;
									isCustomFloor = true;
									if(Mathf.random()/5 > k) {
										floor = Blocks.cryofluid;
									}
								}
								if(tile.build == null) {
									if(tile.block() != Blocks.air && tile.block() != null) {
										if(tile.block().name.indexOf("boulder") != -1) {
											tile.setNet(Blocks.snowBoulder);
										} else if(tile.block() == Blocks.pine || tile.block() == Blocks.sporePine) {
											tile.setNet(Blocks.snowPine);
										} else {
											tile.setNet(Blocks.snowWall);
										}
									}
								}
							}
							tile.setFloorNet(floor, tile.overlay());
						}
					}
				}
				break;
			case "angrySpark":
				Building target = state.rules.defaultTeam.data().getBuildings(Blocks.powerNode).random();
				if(target == null) target = state.rules.defaultTeam.data().getBuildings(Blocks.powerNodeLarge).random();
				if(target == null) target = state.rules.defaultTeam.data().getBuildings(Blocks.battery).random();
				if(target == null) target = state.rules.defaultTeam.data().getBuildings(Blocks.batteryLarge).random();
				if(target != null) angrySparks.add(new AngrySpark(target));
				break;
			case "core": 
				Building vault = state.rules.defaultTeam.data().getBuildings(Blocks.vault).random();
				if(vault != null) {
					vault.tile.setNet(Blocks.coreShard, defaultTeam, 0);
					state.rules.unitCap -= 24;
					Call.setRules(state.rules);
				}
				break;
			case "teamUnitDestroyed":
				Unit unit = state.rules.defaultTeam.data().units.random();
				if(unit != null) unit.kill();
//				unit.shield(1000)
				break;
				
			case "spawnCoreUnit":
				int random = Mathf.random(1, 3);
				if(random == 1) {
					UnitTypes.evoke.spawn(state.rules.defaultTeam, luckySource.x*tilesize, luckySource.y*tilesize);
					info = "Создает союзный [green]Восход";
				} else if(random == 2) {
					UnitTypes.incite.spawn(state.rules.defaultTeam, luckySource.x*tilesize, luckySource.y*tilesize);
					info = "Создает союзный [green]Призыв";
				} else if(random == 3) {
					UnitTypes.emanate.spawn(state.rules.defaultTeam, luckySource.x*tilesize, luckySource.y*tilesize);
					info = "Создает союзный [green]Исход";
				}
				break;
			case "airdrop":
				// TODO
				break;
			case "itemsNegativeEffectsFromItems":
				Seq<Unit> units = state.rules.defaultTeam.data().units;
				for (int i = 0; i < units.size; i++) {
					Unit u = units.get(i);
					if(u.hasItem()) {
						Item item = u.item();
						if(item == Items.coal) {
							u.apply(StatusEffects.tarred, Float.POSITIVE_INFINITY);
						}
						if(item == Items.titanium) {
							u.apply(StatusEffects.freezing, Float.POSITIVE_INFINITY);
						}
						if(item == Items.sporePod) {
							u.apply(StatusEffects.sporeSlowed, Float.POSITIVE_INFINITY);
						}

						float rFlammability = Mathf.random(1f);
						float rExplosiveness = Mathf.random(1f);
						float rRadioactivity = Mathf.random(1f);
						float rCharge = Mathf.random(1f);
						if(rFlammability <= item.flammability) {
							u.apply(StatusEffects.burning, Float.POSITIVE_INFINITY);
							if(item.flammability > 1f) {
								if(rFlammability <= item.flammability - 1f) {
									u.apply(StatusEffects.melting, Float.POSITIVE_INFINITY);
								}
							}
						}
						if(rExplosiveness <= item.explosiveness) {
							u.apply(StatusEffects.blasted, Float.POSITIVE_INFINITY);
							Call.logicExplosion(Team.crux, u.x, u.y, u.hitSize, u.maxHealth()*rExplosiveness/item.explosiveness, true, true, true);
						}
						if(rRadioactivity <= item.radioactivity) {
							u.apply(StatusEffects.sapped, Float.POSITIVE_INFINITY);
							if(Mathf.random(1f) <= item.radioactivity/2f) {
								u.apply(StatusEffects.unmoving, Float.POSITIVE_INFINITY);
							}
						}
						if(rCharge <= item.charge) {
							u.apply(StatusEffects.electrified, Float.POSITIVE_INFINITY);
							u.apply(StatusEffects.shocked, Float.POSITIVE_INFINITY);
						}
					}
				}
				break;
			case "positiveEffects":
				int count = Mathf.ceil(defaultTeam.data().unitCount/4f);
				for (int i = 0; i < count; i++) {
					Unit peu1 = state.rules.defaultTeam.data().units.random();
					if(peu1 != null) peu1.apply(StatusEffects.overclock, Float.POSITIVE_INFINITY);

					Unit peu2 = state.rules.defaultTeam.data().units.random();
					if(peu2 != null) peu2.apply(StatusEffects.overdrive, Float.POSITIVE_INFINITY);
				}
				break;
			default:
//				info = "[red]event not found";
				return;
			}
			
		}

	}

	private boolean hasNearFloor(Block floor, int x, int y) {
		return hasFloor(floor, x-1, y) 
				|| hasFloor(floor, x+1, y)
				|| hasFloor(floor, x, y-1)
				|| hasFloor(floor, x, y+1);
	}

	private boolean hasFloor(Block floor, int x, int y) {
		Tile tile = world.tile(x, y);
		if(tile == null) return false;
		return tile.floor() == floor;
	}
	
	private Seq<AngrySpark> angrySparks = new Seq<AngrySpark>();
	private Seq<Replacer> replacers = new Seq<>();
	private Seq<SuperMine> superMines = new Seq<>();

//	private class ItemsEater {
//		Building target;
//		public ItemsEater(Building target) {
//			this.target = target;
//		}
//		protected boolean needRemove = false;
//		int updates = 0;
//		private void update() {
//			if(needRemove) return;
//			float damage = updates/60f;
//			updates++;
//			if(target.health - damage <= 0) {
//				Building newTarget = target.getPowerConnections(new Seq<Building>()).random();
//				if(newTarget == null) {
//					needRemove = true;
//					target.damagePierce((float) damage);
//					return;
//				}
//				Call.effect(Fx.lancerLaserShoot, target.x, target.y, 60, Color.red, null);
//				Call.effect(Fx.lancerLaserShoot, target.x, target.y, 120, Color.red, null);
//				Call.effect(Fx.lancerLaserShoot, target.x, target.y, 180, Color.red, null);
//				
//				target.damagePierce((float) damage);
//				target = newTarget;
//				updates = 0;
//				return;
//			}
//			if(updates%5 == 0) {
//				Call.effect(Fx.thoriumShoot, target.x, target.y, 60, Color.red, null);
//				Call.effect(Fx.thoriumShoot, target.x, target.y, 120, Color.red, null);
//				Call.effect(Fx.thoriumShoot, target.x, target.y, 240, Color.red, null);
//			}
//			target.damagePierce((float) damage);
//		}
//	}
//	
	private class SuperMine {
		
		protected boolean needRemove = false;
		int updates = 0;
		
		int mines = 0;
		
		private void update() {
			if(needRemove) return;
			updates++;
			if(updates%10 == 0) {
				Seq<Building> targets = GameWork.defaultTeam().data().getBuildings(Blocks.shockMine);
				if((targets.size == 0 && mines >= 100) || mines > 100) {
					needRemove = true;
					return;
				}
				
				Building target = targets.random();
				if(target != null) {
					Call.logicExplosion(Team.crux, target.x, target.y, tilesize*3, 250, true, true, false);
					GameWork.removeEnvBlock(target.tileX()+1, target.tileY());
					GameWork.removeEnvBlock(target.tileX()-1, target.tileY());
					GameWork.removeEnvBlock(target.tileX(), target.tileY()+1);
					GameWork.removeEnvBlock(target.tileX(), target.tileY()-1);
					warmTile(Blocks.air, null, null, target.tileX()+1, target.tileY());
					warmTile(Blocks.air, null, null, target.tileX()-1, target.tileY());
					warmTile(Blocks.air, null, null, target.tileX(), target.tileY()+1);
					warmTile(Blocks.air, null, null, target.tileX(), target.tileY()-1);
					mines++;
				}
			}
		}
	}
	
	private class Replacer {
		
		Block oldBlock, newBlock;
		public Replacer(Block oldBlock, Block newBlock) {
			this.oldBlock = oldBlock;
			this.newBlock = newBlock;
		}
		
		protected boolean needRemove = false;
		int updates = 0;
		
		private void update() {
			if(needRemove) return;
			updates++;
			if(updates%10 == 0) {
				Seq<Building> targets = GameWork.defaultTeam().data().getBuildings(oldBlock);
				if(targets.size == 0) {
					needRemove = true;
					return;
				}
				Building target = targets.random();
				GameWork.replaceBuilding(target, newBlock);
				Call.effect(Fx.rotateBlock, target.x, target.y, target.block.size, Color.white, null);
			}
		}
	}
	
	
	private class AngrySpark {
		Building target;
		public AngrySpark(Building target) {
			this.target = target;
		}
		protected boolean needRemove = false;
		int updates = 0;
		private void update() {
			if(needRemove) return;
			float damage = updates/60f;
			updates++;
			if(target == null) {
				needRemove = true;
				return;
			}
			if(target.tile().build == null) {
				needRemove = true;
				return;
			}
			if(target.health - damage <= 0) {
				Building newTarget = target.getPowerConnections(new Seq<Building>()).random();
				if(newTarget == null) {
					needRemove = true;
					Call.logicExplosion(Team.crux, target.x, target.y, tilesize, damage, true, true, false);
					return;
				}
				Call.effect(Fx.lancerLaserShoot, target.x, target.y, 60, Color.red, null);
				Call.effect(Fx.lancerLaserShoot, target.x, target.y, 120, Color.red, null);
				Call.effect(Fx.lancerLaserShoot, target.x, target.y, 180, Color.red, null);
				Call.logicExplosion(Team.crux, target.x, target.y, tilesize, damage, true, true, false);
				target = newTarget;
				updates = 0;
				return;
			}
			if(updates%5 == 0) {
				Call.effect(Fx.thoriumShoot, target.x, target.y, 60, Color.red, null);
				Call.effect(Fx.thoriumShoot, target.x, target.y, 120, Color.red, null);
				Call.effect(Fx.thoriumShoot, target.x, target.y, 240, Color.red, null);
			}

			Call.logicExplosion(Team.crux, target.x, target.y, tilesize, damage, true, true, false);
		}
	}
	
	private void updateMiniEvent() {
		if(angrySparks == null) angrySparks = new Seq<>();
		for (int i = 0; i < angrySparks.size; i++) {
			angrySparks.get(i).update();
			if(angrySparks.get(i).needRemove) {
				angrySparks.remove(i);
				break;
			}
		}
		if(replacers == null) replacers = new Seq<>();
		for (int i = 0; i < replacers.size; i++) {
			replacers.get(i).update();
			if(replacers.get(i).needRemove) {
				replacers.remove(i);
				break;
			}
		}

		if(superMines == null) superMines = new Seq<>();
		for (int i = 0; i < superMines.size; i++) {
			superMines.get(i).update();
			if(superMines.get(i).needRemove) {
				superMines.remove(i);
				break;
			}
		}
	}

	boolean needCheck = false;
	
	@Override
	protected void blockBuildEnd(BlockBuildEndEvent e) {
		needCheck = true;
	}

	@Override
	public void generateWorld() {
		isLuckActivated = false;
		luckySource = null;
		isOrePlaced = false;
		Call.sendMessage(getInfo());
	}

	@Override
	public void trigger(Player player, String... args) {
		String name = args[0];
		
		if(args.length == 2) {
			if(name.equalsIgnoreCase("event")) {
				MiniEvent event = new MiniEvent(args[1], "debug");
				event.run(-1);
				player.sendMessage("[gold]Событие запущено:[] " + event.info);
			}
			return;
		}
		if(name.equalsIgnoreCase("find")) {
			if(luckySource == null) {
				player.sendMessage("[red]Источника удачи нет :(");
			} else {
				if(player.unit() != null) player.unit().x(luckySource.x*tilesize);
				player.set(luckySource.x*tilesize, luckySource.y*tilesize);
				Call.setPosition(player.con, luckySource.x*tilesize, luckySource.y*tilesize);
			}
		}
		if(name.equalsIgnoreCase("reset")) {
			isLuckActivated = false;
			luckySource = null;
			player.sendMessage("[gold]Готово!");
		}
		if(name.equalsIgnoreCase("activate")) {
			isLuckActivated = true;
			player.sendMessage("[gold]Активирован!");
		}
		if(name.equalsIgnoreCase("event")) {
			randomMiniEvent(-1);
		}
	}
}
