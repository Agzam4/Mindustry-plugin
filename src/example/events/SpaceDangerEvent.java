package example.events;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.UnitTypes;
import mindustry.content.Weathers;
import mindustry.core.UI;
import mindustry.core.World;
import mindustry.entities.Effect;
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
import mindustry.gen.Entityc;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.gen.WeatherState;
import mindustry.gen.WeatherStatec;
import mindustry.graphics.Drawf;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.UnitType;
import mindustry.type.Weather;
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
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import arc.Events;
import arc.graphics.Color;
import arc.math.Mat;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Point2;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;

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

	private int updates = 0;

	int dFissileMatterTimer = 0;
	int fissileMatterCounter = 0;
	int targetFissileMatterCounter = 0;
	int hudLerpT = 0;

	@Override
	public void update() {

		updates++;
		if(updates % 10 == 0) {
			if(fissileMatterCounter < targetFissileMatterCounter) {
				int d = (targetFissileMatterCounter - dFissileMatterTimer)/2;
				if(d < 1) d = 1;
				fissileMatterCounter+=d;
				dFissileMatterTimer = 61;
				hudLerpT = 0;

				// /event space_danger faston
				// /sandbox on
				// /sync
				// /etrigger fall
			}

			for (int i = 0; i < targets.size(); i++) {
				targets.get(i).effect(updates);
			}
		}
		if(updates%5 == 0 && hudLerpT < 20) {
			hudLerpT += 5;
			if(hudLerpT > 20) hudLerpT = 20;
			Call.setHudText("Получено [#" +  new Color(0xffd700ff).lerp(Color.lightGray, hudLerpT/20f).toString() + "] x" + fissileMatterCounter + " [#5e988dff]нестабильная материя [white](\uf747)");
		}
		// Получено [lightgray]x1\uf747 [#5e988dff]нестабильная материя
		if(dFissileMatterTimer > 0) {
			dFissileMatterTimer--;
			if(dFissileMatterTimer == 0) {
				targetFissileMatterCounter = 0;
				fissileMatterCounter = 0;
				dFissileMatterTimer = 0;
				Call.hideHudText();
			}
		}

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


		dFissileMatterTimer = 0;
		fissileMatterCounter = 0;
		targetFissileMatterCounter = 0;
		hudLerpT = 0;
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

	static final UnitType[] pull = new UnitType[] {
			UnitTypes.renale, 	// 0.  score 1		HP: 500
			UnitTypes.risso, 	// 1.  score 1		HP: 280
			UnitTypes.retusa, 	// 2.  score 1		HP: 270
			
			UnitTypes.minke, 	// 3.  score 5		HP: 600
			UnitTypes.oxynoe, 	// 4.  score 6		HP: 560
			
			UnitTypes.bryde, 	// 5.  score 10		HP: 900
			UnitTypes.cyerce, 	// 6.  score 11		HP: 870
			
			UnitTypes.sei, 		// 7.  score 25		HP: 11 000
			UnitTypes.aegires, 	// 8.  score 30		HP: 12 000
			
			UnitTypes.omura, 	// 9.  score 45		HP: 22 000
			UnitTypes.navanax, 	// 10. score 45		HP: 20 000
			
			UnitTypes.latum, 	// 11.  score 50	HP: 20 000 + (500 x 5) = 22500 (x45 renale)
	};
	
	static final int[] pullValue = new int[] {
		1, 1, 1,
		5, 5,
		10, 11,
		25, 30,
		45, 45,
		50
	};
	
	ArrayList<Unit> dormantCystDropUnits = new ArrayList<>();

	@Override
	public void unitDestroy(UnitDestroyEvent e) {
		if(dormantCystDropUnits.contains(e.unit)) {
			if(Groups.player.size() > 0) {
				if(Groups.player.index(0).core() != null) {

					int count = 0;
					
					for (int i = 0; i < pull.length; i++) {
						if(e.unit.type() == pull[i]) {
							count = pullValue[i];
						}
					}
					
					if(count != 0) {
						Groups.player.index(0).core().items.add(Items.fissileMatter, count);
						targetFissileMatterCounter += count;
					}
					//					Call.sendMessage();
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

			if(world.tile(x, y) == null) return;
			if(world.tile(x, y).block() == null) return;
			if(world.tile(x, y).block() == Blocks.massDriver) {
				world.tile(x, y).setTeam(Team.crux);
				x = world.tile(x, y).centerX();
				y = world.tile(x, y).centerY();
			}
			
			fallTime = (int) (Math.random()*60+60)*60;
		}

		public void effect(int i) {
			double angle = i%120/60d*Math.PI;
			final double r = 5;
			float dx = (float) (r*Math.sin(angle));
			float dy = (float) (r*Math.cos(angle));
			Call.effect(Fx.chainLightning, x*tilesize, y*tilesize, 1, Color.red, new Vec2((x+dx)*tilesize, (y+dy)*tilesize));
			Call.effect(Fx.chainLightning, x*tilesize, y*tilesize, 1, Color.blue, new Vec2((x-dx)*tilesize, (y+dy)*tilesize));
		}

		private void update() {
			if(isEnded) return;
			
			if((fallTime%(30*60) == 0 || fallTime == 15*60) && fallTime > 0) {
				int sec = fallTime/60;
				int min = sec/60;
				sec -= min*60;
				Call.warningToast(0, "Метероит упадет через " + (min > 0 ? "[gold]" + min + ":" : "[red]") + (sec == 0 ? "00" : sec));
			}
			if(fallTime < 0) {
				fall();
				return;
			}
			
			if(hasTarget(x, y) 
					&& getTsunamiPower(x+3, y) > .5 && getTsunamiPower(x-3, y) > .5
					&& getTsunamiPower(x, y+3) > .5 && getTsunamiPower(x, y-3) > .5) {
				if(fallTime == 0) {
					Call.warningToast(0, "[red]Метеорит приближается!");
				}
				fallTime--;
			} else if(!isEnded) {
				Call.warningToast(0, "[red]Метеорит пролетел мимо!");
				needRemove = true;
			}
		}

		private void fall() {
			int score = meteorits*2 + 5;
			int maxUnitLevel = 0;
			
			ArrayList<Integer> ids = new ArrayList<>();
			
			while (true) {
				int maxIndex = 0;
				if(score <= 0) break;
				if(score >= 1) maxIndex = 2;
				if(score >= 5) maxIndex = 3;
				if(score >= 6) maxIndex = 4;
				if(score >= 10) maxIndex = 5;
				if(score >= 11) maxIndex = 6;
				if(score >= 25) maxIndex = 7;
				if(score >= 30) maxIndex = 8;
				if(score >= 45) maxIndex = 10;
				if(score >= 50) maxIndex = 11;
				
				int index = Mathf.random(maxIndex);
				
				ids.add(index);
				
				maxUnitLevel = Math.max(maxUnitLevel, pullValue[index]);
				score -= pullValue[index];
			}
			
			int radius = 15 + (maxUnitLevel*15/50);
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

					double rand = hypot/radius; // 0 is near center
					double irand = 1d - hypot/radius; // 0 is near bounds
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
					if(Math.random() < .05*irand) {
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
					if(tile.overlay() != Blocks.spawn) {
						tile.clearOverlay();
						if(floor != null) {
							tile.setFloorNet(floor, ore ? Blocks.oreScrap : tile.overlay());
						}
						if(floor != null && tile.block() != Blocks.air) {
							if(Math.random() > rand*rand/1.5d) tile.setNet(Blocks.air);
						}
						if(tile.block() == Blocks.air && ferricBoulder) {
							tile.setNet(Blocks.ferricBoulder);
						}else if(ferricWall) {
							tile.setNet(Blocks.ferricStoneWall);
						}
					}
				}
			}
			Call.effect(Fx.teleportActivate, x*tilesize, y*tilesize, radius*tilesize, Color.coral);

			Call.logicExplosion(Team.neoplastic, x*tilesize, y*tilesize, radius*tilesize, 10_000, true, true, false);
			Call.logicExplosion(Team.neoplastic, x*tilesize, y*tilesize, radius*tilesize + 5*8, 5_000, true, true, true);
			Call.sendMessage("[red]Метеорит упал!");
			
			for (int i = 0; i < ids.size(); i++) {
				int id = ids.get(i);
				UnitType type = pull[id];
				Unit unit = null;
				if(type == UnitTypes.latum || type == UnitTypes.renale) {
					unit = type.create(Team.crux);
				} else {
					type.flying = true;
					unit = type.create(Team.crux);
					type.flying = false;
				}
				if(unit != null) {
					float angle = Mathf.random(360);
					float dist = Mathf.random(radius/2f);
					unit.set(x*tilesize + dist*Mathf.cosDeg(angle), y*tilesize + dist*Mathf.sinDeg(angle));
					dormantCystDropUnits.add(unit);
					if(!net.client()){
						unit.add();
					}
				}
			}
			
//			if(meteorits%5 == 0 && meteorits > 0) {
//				for (int j = 0; j < meteorits; j+=5) {
//					boolean canHeal = UnitTypes.latum.canHeal;
//
//					Ability regen = null;
//					for (int i = 0; i < UnitTypes.latum.abilities.size; i++) {
//						if(UnitTypes.latum.abilities.get(i).getClass().getName().equals(RegenAbility.class.getName())) {
//							regen = UnitTypes.latum.abilities.get(i);
//							UnitTypes.latum.abilities.remove(i);
//							break;
//						}
//					}
//
//					UnitTypes.latum.setStats();
//					Unit u = UnitTypes.latum.create(Team.crux);
//					u.set(x*tilesize, y*tilesize);
//					dormantCystDropUnits.add(u);
//					u.add();
//					if(regen != null)
//						UnitTypes.latum.abilities.add(regen);
//					UnitTypes.latum.setStats();
//				}
//			}

			meteorits++;
			isEnded = true;			
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
				int cost1 = (i%5 + 1)*10;
				int cost2 = -(i%5 + 1)*10 + 1;
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
					replaseBase[i][0].spawn(unit.team(), unit.x(), unit.y());
					unit.x(0);
					unit.y(0);
					unit.kill();
					addDormantCystToCore(e.amount-cost2);
					unit.clearItem();
					Call.sendMessage("[gold]Игрок " + e.player.coloredName() + " [gold] вернул [lightgray]" + -cost2 + " [gold]дремлющих оболочек");
					return;
				}
			}
			e.player.sendMessage("[gold]Этот юнит не реагирует с данным предметом");
			addDormantCystToCore(e.amount);
			unit.clearItem();
		}
	}

	@Override
	public void trigger(Player player, String... args) {
		String name = args[0];
		if(name.equalsIgnoreCase("fall")) {
			int x = (int) (player.mouseX()/8);
			int y = (int) (player.mouseY()/8);

			Target target = new Target(x, y);
			target.fallTime = -1;
			target.update();
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
