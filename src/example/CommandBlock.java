package example;

import static mindustry.Vars.world;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import arc.func.Boolc;
import arc.graphics.g2d.Bloom;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.gl.Shader;
import arc.util.Log;
import arc.util.Threads;
import arc.util.noise.Simplex;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.graphics.MenuRenderer;
import mindustry.logic.LExecutor;
import mindustry.logic.LExecutor.Var;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.meta.Env;

public class CommandBlock {

	int x, y;
	public boolean needRemove;
	String autor;

	public CommandBlock(String autor, int x, int y) {
	}
	
	String lastCode = null;
	
	public void update() {
		Building building = world.build(x, y);
		if(building == null) {
			needRemove = true;
			return;
		}
		
		if(building instanceof LogicBuild) {
			LogicBuild logic = (LogicBuild) building;
			if(building.lastAccessed.equalsIgnoreCase(autor)) {
				lastCode = logic.code;
			} else {
				if(lastCode == null) {
					needRemove = true;
					return;
				}
				Log.info(building.lastAccessed + "has configurated command block");
				logic.updateCode(lastCode);
				building.lastAccessed(autor);
			}
			
			LExecutor executor = logic.executor;
			for (int i = 0; i < executor.vars.length; i++) {
				Var var = executor.vars[i];
				if(var.constant) continue;
				if(var.objval == null) continue;
				if(var.objval instanceof String) {
					String name = var.name;
					String val = var.objval.toString();
					if(val.startsWith("#")) continue;
					
					if("announce".equalsIgnoreCase(name)) {
						var.objval = announce(val);
					}
					if("message".equalsIgnoreCase(name)) {
						var.objval = sendMessage(val);
					}
					if("block".equalsIgnoreCase(name)) {
						var.objval = block(val);
					}
//					if("explosion".equalsIgnoreCase(name)) {
//						GameWork.getTeamByName(name);
//						Call.logicExplosion(null, i, i, i, i, needRemove, needRemove, needRemove);
//					}
					
				}
			}
		} else {
			needRemove = true;
			return;
		}


		//		LogicBuild build = world.tile(0).build;
		//		build.executor.vars;
	}

	long lastBlock = System.nanoTime();
	
	private String block(String data) {
		if(getElapsedMillis(lastBlock) <= 250) return data;
		String[] vals = data.split(" ");
		if(vals.length != 3) return "#ArgumentsException";
		try {
			int xx = getX(vals[0]);
			int yy = getY(vals[1]);
			if(xx == x && yy == y) return "#WrongPoint";
		} catch (NumberFormatException e) {
			return "#NumberFormatException";
		}
		
		String blockname = vals[2];
		try {
			Field field = Blocks.class.getField(blockname);
			Block block = (Block) field.get(null);
			if(world.tile(x, y) != null) {
				world.tile(x, y).setNet(block, Team.sharded, 0);
				return "ok";
			}
		} catch (NoSuchFieldException | SecurityException e) {
			return "#NotFind";
		} catch (ClassCastException e2) {
			return "#NotBlock";
		} catch (IllegalArgumentException | IllegalAccessException e) {
			return "#Illegal";
		}
		return "#IDK-Error";
	}

	private int getX(String s) {
		if(s.startsWith("+")) return x + Integer.parseInt(s);
		if(s.startsWith("-")) return x - Integer.parseInt(s);
		return Integer.parseInt(s);
	}
	
	private int getY(String s) {
		if(s.startsWith("+")) return y + Integer.parseInt(s);
		if(s.startsWith("-")) return y - Integer.parseInt(s);
		return Integer.parseInt(s);
	}

	long lastAnnounce = System.nanoTime();
	private String announce(String data) {
		if(getElapsedSeconds(lastAnnounce) <= 5) return data;
		Call.announce(data);
		lastAnnounce = System.nanoTime();
		return "ok";
	}

	long lastMessage = System.nanoTime();
	private String sendMessage(String data) {
		if(getElapsedSeconds(lastMessage) <= 5) return data;
		Call.sendMessage(data);
		lastMessage = System.nanoTime();
		return "ok";
	}

	private long getElapsedSeconds(long start) {
		return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-start);
	}

	private long getElapsedMillis(long start) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);
	}
}
