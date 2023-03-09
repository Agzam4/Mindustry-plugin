package example.events;

import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.UnitTypes;
import mindustry.core.World;
import mindustry.entities.abilities.Ability;
import mindustry.entities.abilities.RegenAbility;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.DepositEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.UnitCreateEvent;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.WithdrawEvent;
import mindustry.game.GameStats;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.LiquidTurret;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.consumers.Consume;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.modules.LiquidModule;
import mindustry.world.modules.LiquidModule.LiquidConsumer;

import static mindustry.Vars.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;

import arc.Events;
import arc.math.geom.Point2;

public class SpaceDangerEvent extends ServerEvent {

	
	public SpaceDangerEvent() {
		super("Space danger");
		color = "magenta";
	}

	@Override
	public void init() {
		
	}///event space_danger faston

	@Override
	public void announce() {
		Call.announce("[magenta]Космичесое событие начнется на следующей карте!");
		Call.sendMessage("[magenta]Космичесое событие начнется на следующей карте!");	
	}

	boolean isLablePlaced = false;
	
	ArrayList<Target> targets = new ArrayList<>();
	
	int meteorits = 0;
	
	@Override
	public void playerJoin(PlayerJoin e) {
		if(e.player == null) return;
		e.player.sendMessage(info);
		
	}
	
	private static final String info = "[magenta]Постойте [gold]электоромагнитную катапульту[magenta]\\nОкружите ее четырмя [gold]цунами[magenta] запитанными шлаком (Не забудьте подготовить защиту)";
	
	@Override
	public void update() {
		if(!isLablePlaced) {
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
						isLablePlaced = true;
						return;
					}
				}
			}
		}
		
		for (int i = 0; i < targets.size(); i++) {
			targets.get(i).update();
		}

		for (int i = 0; i < targets.size(); i++) {
			if(targets.get(i).needRemove || targets.get(i).isEnded) {
				targets.remove(i);
				i = 0;
			}
		}
	}
	
	// /event space_danger faston

	@Override
	public void generateWorld() {
		meteorits = 0;
		targets.clear();
		dormantCystDropUnits.clear();
		Call.sendMessage(info);
	}

	@Override
	protected void blockBuildEnd(BlockBuildEndEvent e) {
		if(e.tile.block().name.equals(Blocks.massDriver.name)) {
			int x = e.tile.centerX();
			int y = e.tile.centerY();
			
			if(hasTarget(x, y)) {
				targets.add(new Target(x, y));
			}
		}
		if(e.tile.block().name.equals(Blocks.tsunami.name)) {
			int x = e.tile.centerX();
			int y = e.tile.centerY();

			if(hasMassDriver(x+3, y)) {
				if(hasTarget(x+3, y)) {
					targets.add(new Target(x+3, y));
				}
			}
			if(hasMassDriver(x-3, y)) {
				if(hasTarget(x-3, y)) {
					targets.add(new Target(x-3, y));
				}
			}
			
			if(hasMassDriver(x, y+3)) {
				if(hasTarget(x, y+3)) {
					targets.add(new Target(x, y+3));
				}
			}
			if(hasMassDriver(x, y-3)) {
				if(hasTarget(x, y-3)) {
					targets.add(new Target(x, y-3));
				}
			}
		}
	}
	
	ArrayList<Unit> dormantCystDropUnits = new ArrayList<>();
	
	@Override
	public void unitDestroy(UnitDestroyEvent e) {
		if(dormantCystDropUnits.contains(e.unit)) {
			if(Groups.player.size() > 0) {
				if(Groups.player.index(0).core() != null) {
					
					int count = 1;
					
					if(e.unit.type() == UnitTypes.latum) {
						count = 5;
					}
					
					Groups.player.index(0).core().items.add(Items.fissileMatter, count);
					Call.sendMessage("Получено [lightgray] x" + count + "[gold] нестабильная материя");
				}
			}
			dormantCystDropUnits.remove(e.unit);
		}
	}

	// /event space_danger faston
	
	private boolean hasTarget(int xx, int yy) {
		if(isOut(xx, yy)) return false;
		if(world.tile(xx, yy).block().name.equals(Blocks.massDriver.name)) {
			int x = world.tile(xx, yy).build.tile.centerX();
			int y = world.tile(xx, yy).build.tile.centerY();
			return hasTsunami(x+3, y) && hasTsunami(x-3, y)
				&& hasTsunami(x, y+3) && hasTsunami(x, y-3);
		}
		return false;
	}
	
	private boolean hasTsunami(int x, int y) {
		if(isOut(x, y)) return false;
		if(world.tile(x, y).block().name.equals(Blocks.tsunami.name)) {
			return true;
		}
		return false;
	}
	
	private boolean hasMassDriver(int x, int y) {
		if(isOut(x, y)) return false;
		
		if(world.tile(x, y).block().name.equals(Blocks.massDriver.name)) {
			return true;
		}
		return false;
	}
	
	private boolean isOut(int x, int y) {
		if(x < 0) return true;
		if(y < 0) return true;
		if(x >= world.width()) return true;
		if(y >= world.height()) return true;
		return false;
	}
	
	private class Target {
		
		private int x, y;
		private boolean needRemove = false;
		private int shootTime = 0;

		private boolean isActivated = false;
		private boolean isEnded = false;
		private int fallTime;
		
		public Target(int x, int y) {
			for (int i = 0; i < targets.size(); i++) {
				if(targets.get(i).x == x && targets.get(i).y == y) {
					needRemove = true;
					break;
				}
			}
			
			this.x = x;
			this.y = y;
			
			if(world.tile(x, y).block().name.equals(Blocks.massDriver.name)) {
				world.tile(x, y).setTeam(Team.crux);
			}
		}
		
		private void update() {
			if(isEnded) return;
			if(isActivated) {
				fallTime--;
				if(fallTime < 0) {
					int radius = 15 + ((meteorits%5 == 0 && meteorits > 0) ? 15 : 0);
					for (int dy = -radius; dy <= radius; dy++) {
						for (int dx = -radius; dx <= radius; dx++) {
							int x = this.x + dx;
							int y = this.y + dy;
							if(isOut(x, y)) continue;
							double hypot = Math.hypot(dx, dy);
							if(hypot > radius) continue;

							boolean ferricStone = false;
							boolean ferricCraters = false;
							boolean ferricBoulder = false;
							boolean ferricWall = false;
							
							boolean slag = false;
							boolean magmarock = false;
							boolean hotrock = false;
							boolean basalt = false;

							boolean ore = false;
							
							double k = hypot/radius;
							
							ferricStone = Math.random() > (hypot-5)/radius;
							
							if(ferricStone) {
								if(Math.random() < .2) {
									ferricStone = false;
									ferricCraters = true;
								}
							}
							if(Math.random() < .3) {
								ore = true;
							}
							if(Math.random() < .15) {
								ferricBoulder = true;
							}
							if(Math.random() < .025) {
								ferricWall = true;
								ferricStone = true;
							}

							if(ferricStone) {
								if(Math.random() < .4) {
									basalt = true;
								}
								if(Math.random() < .2) {
									magmarock = true;
								}
								if(Math.random() < .1) {
									magmarock = true;
								}
								if(Math.random() < .05) {
									slag = true;
								}
							}
							
							Block floor = null;
							if(ferricStone) floor = Blocks.ferricStone;
							if(ferricCraters) floor = Blocks.ferricCraters;
							if(slag) floor = Blocks.slag;
							if(magmarock) floor = Blocks.magmarock;
							if(hotrock) floor = Blocks.hotrock;
							if(basalt) floor = Blocks.basalt;
							
							Tile tile = world.tile(x, y);
							tile.clearOverlay();
							if(floor != null) {
								tile.setFloorNet(floor, ore ? Blocks.oreScrap : tile.overlay());
							}
							if(tile.block() == Blocks.air && ferricBoulder) {
								tile.setNet(Blocks.ferricBoulder);
							}else if(ferricWall) {
								if(tile.block() == Blocks.air) {
									tile.setNet(Blocks.ferricStoneWall);
								} else {
									if(Math.random() < k) tile.setNet(Blocks.ferricStoneWall);
									else tile.setNet(Blocks.air);
								}
							}
						}
					}
					Call.logicExplosion(Team.neoplastic, x*tilesize, y*tilesize, radius*2, 25_000, true, true, false);
					Call.logicExplosion(Team.neoplastic, x*tilesize, y*tilesize, radius*2 + 10, 10_000, true, true, true);
					Call.sendMessage("[red]Метерорит упал!");
					
					for (int i = 0; i < 5 + meteorits; i++) {
						if(Groups.player.size() == 0) break;
						if(Groups.player.index(0).unit() == null) break;

						Unit u = UnitTypes.renale.create(Team.crux);
						u.set(x*tilesize, y*tilesize);
						dormantCystDropUnits.add(u);
						if(!net.client()){
							u.add();
						}
					}
					
					if(meteorits%5 == 0 && meteorits > 0) {
						for (int j = 0; j < meteorits; j+=5) {
							boolean canHeal = UnitTypes.latum.canHeal;
							
							Ability regen = null;
							for (int i = 0; i < UnitTypes.latum.abilities.size; i++) {
								if(UnitTypes.latum.abilities.get(i).getClass().getName().equals(RegenAbility.class.getName())) {
									regen = UnitTypes.latum.abilities.get(i);
									UnitTypes.latum.abilities.remove(i);
									break;
								}
							}
							
							UnitTypes.latum.setStats();
							Unit u = UnitTypes.latum.create(Team.crux);
							u.set(x*tilesize, y*tilesize);
							dormantCystDropUnits.add(u);
							u.add();
							if(regen != null)
							UnitTypes.latum.abilities.add(regen);
							UnitTypes.latum.setStats();
						}
					}
					
					meteorits++;
					isEnded = true;
				}
			} else {
				if(hasTarget(x, y)) {
					if(getTsunamiPower(x+3, y) > .5 && getTsunamiPower(x-3, y) > .5
					&& getTsunamiPower(x, y+3) > .5 && getTsunamiPower(x, y-3) > .5) {
						if(shootTime%60 == 0) {
				            Fx.spawnShockwave.at(x, y, World.conv(15));
						}
						if(shootTime == 0) {
							Call.sendMessage("[red]Метерорит приближается!");
						}
						shootTime++;
						if(shootTime > 60*30) {
							fallTime = (int) (Math.random() * 60*60);
							isActivated = true;
						}
					} else {
						shootTime = 0;
					}
							
				} else {
					Call.sendMessage("[red]Метерорит пролетел мимо!");
					needRemove = true;
				}
			}
		}
		private LiquidTurret getTsunami(int x, int y) {
			if(isOut(x, y)) return null;
			if(world.tile(x, y).block().name.equals(Blocks.tsunami.name)) {
				return (LiquidTurret) world.tile(x, y).block();
			}
			return null;
		}
		private float getTsunamiPower(int x, int y) {
			if(isOut(x, y)) return 0;
			
			if(world.tile(x, y).block().name.equals(Blocks.tsunami.name)) {
				Building build = world.tile(x, y).build;
				if(build == null) return 0;
				LiquidModule liquids = build.liquids();
				if(liquids == null) return 0;
				return liquids.get(Liquids.slag);
			}
			return 0;
		}
	}
	
	
	private static final UnitType[][] replaseBase = {
			// 1
			{UnitTypes.dagger, UnitTypes.stell},
			{UnitTypes.mace, UnitTypes.locus},
			{UnitTypes.fortress, UnitTypes.precept},
			{UnitTypes.scepter, UnitTypes.vanquish},
			{UnitTypes.reign, UnitTypes.conquer},
			// 2
			{UnitTypes.crawler, UnitTypes.merui},
			{UnitTypes.atrax, UnitTypes.cleroi},
			{UnitTypes.spiroct, UnitTypes.anthicus},
			{UnitTypes.arkyid, UnitTypes.tecta},
			{UnitTypes.toxopid, UnitTypes.collaris},
			// 3
			{UnitTypes.flare, UnitTypes.elude},
			{UnitTypes.poly, UnitTypes.avert},
			{UnitTypes.zenith, UnitTypes.obviate},
			{UnitTypes.antumbra, UnitTypes.quell},
			{UnitTypes.eclipse, UnitTypes.disrupt},
	};
	// /event space_danger faston
	
	@Override
	public void withdraw(WithdrawEvent e) {
		if(e.player == null) return;
		Unit unit = e.player.unit();
		if(unit == null) return;
		
		if(e.item == Items.fissileMatter) {
			UnitType unitType = unit.type();
			if(unitType == UnitTypes.alpha || unitType == UnitTypes.beta || unitType == UnitTypes.gamma) {
				e.player.sendMessage("[gold]Вселитесь в юнита и возмите им этот предмет, чтобы улучшить его");
				addDormantCystToCore(e.amount);
				unit.clearItem();
				return;
			}
			for (int i = 0; i < replaseBase.length; i++) {
				int cost1 = (i%5 + 1)*5;
				int cost2 = (i%5 + 1);
				if(unitType == replaseBase[i][0]) {
					if(e.amount >= cost1) {
						replaseBase[i][1].spawn(unit.team(), unit.x(), unit.y());
						unit.x(0);
						unit.y(0);
						unit.kill();
						addDormantCystToCore(e.amount-cost1);
						unit.clearItem();
						Call.sendMessage("[gold]Игрок " + e.player.coloredName() + " [gold]потратил [lightgray]" + cost1 + " [gold]дремлющих оболочек");
					} else {
						e.player.sendMessage("[gold]Недостаточно предметов, требуется " + cost1);
						addDormantCystToCore(e.amount);
						unit.clearItem();
					}
					return;
				}
				if(unitType == replaseBase[i][1]) {
					if(e.amount >= cost2) {
						replaseBase[i][0].spawn(unit.team(), unit.x(), unit.y());
						unit.x(0);
						unit.y(0);
						unit.kill();
						addDormantCystToCore(e.amount-cost2);
						unit.clearItem();
						Call.sendMessage("[gold]Игрок " + e.player.coloredName() + " [gold] потратил [lightgray]" + cost2 + " [gold]дремлющих оболочек");
					} else {
						e.player.sendMessage("[gold]Недостаточно предметов, требуется " + cost2);
						addDormantCystToCore(e.amount);
						unit.clearItem();
					}
					return;
				}
			}
			e.player.sendMessage("[gold]Этот юнит не реагирует с данным предметом");
			addDormantCystToCore(e.amount);
			unit.clearItem();
		}
	}
	
	private boolean addDormantCystToCore(int count) {
		if(Groups.player.size() > 0) {
			if(Groups.player.index(0).core() != null) {
				Groups.player.index(0).core().items.add(Items.fissileMatter, count);
				return true;
			}
		}
		return false;
	}
}
