package agzam4;

import arc.*;
import arc.math.geom.Point2;
import arc.util.*;
import mindustry.*;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.mod.Mods.LoadedMod;
import mindustry.mod.Plugin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import agzam4.achievements.*;
import agzam4.bot.TelegramBot;
import agzam4.database.Database;
import agzam4.events.EventMap;
import agzam4.events.ServerEventsManager;
import agzam4.net.NetMenu;

import static agzam4.Emoji.*;
import static mindustry.Vars.*;

public class AgzamPlugin extends Plugin {

	public static LoadedMod plugin; 
	
	public static DataCollecter dataCollect;
	public static AchievementsManager achievementsManager;
	
	private static CommandHandler serverHandler;
    
    @Override
    public void init() {
    	plugin = Vars.mods.getMod("agzam4plugin");
    	Log.info("init");
    	try {
			Database.init(Vars.saveDirectory.absolutePath() + "/database");
		} catch (ClassNotFoundException e) {
			Log.err(e);
			Threads.sleep(10_000);
			Core.app.exit();
		}
    	Game.init();
    	try {
        	TelegramBot.init();
		} catch (Exception e) {
			Log.err(e);
		}
    	Admins.init();
    	Players.init();
    	AchievementsManager.init();
    	NetMenu.init();
    	
    	achievementsManager = new AchievementsManager();
    	CommandsManager.init();
    	CommandsManager.registerBotCommands(TelegramBot.handler);
    	CommandsManager.registerServerCommands(serverHandler);
    	Log.reset();
    	
    	ServerEventsManager.init();
    	EventMap.load();
    	
    	maps = new Maps();
    	
    	maps.load();
    	
    	
    	dataCollect = new DataCollecter();
    	dataCollect.init();
    	dataCollect.collect();

    	Events.run(Trigger.update, () -> {
//    		achievementsManager.update();
    		ServerEventsManager.update();
    		dataCollect.update();
    		
    		
//    		Vars.state.rules.defaultTeam.rules().unitBuildSpeedMultiplier *= 2;
//    		Vars.state.rules.defaultTeam.rules().unitCostMultiplier *= 2;
//    		Vars.state.rules.defaultTeam.rules().unitCrashDamageMultiplier *= 2;
//    		Vars.state.rules.defaultTeam.rules().unitDamageMultiplier *= 2;
//    		Vars.state.rules.defaultTeam.rules().unitHealthMultiplier *= 2;
//    		Vars.content.units().each(u -> {var us = Team.sharded.data().getUnits(u); for(var i = 0; i < us.size; i+=2) us.get(i).kill();});
//    		Vars.state.rules.unitCap
    	});
		
    	Events.on(ServerLoadEvent.class, e -> {
        	initColors();
    	});
    	
    	Events.on(GameOverEvent.class, e -> {
    		StringBuilder result = new StringBuilder(state.map.name());
    		result.append("\nСчёт: [lightgray]");
    		result.append(state.wave);
    		result.append('/');
    		result.append(state.map.getHightScore());
    		if(state.wave > state.map.getHightScore()) {
        		result.append("[gold] (Новый рекорд!)");
        		state.map.setHighScore(state.wave);
    		}
    		Call.sendMessage(result.toString());
    		CommandsManager.stopSkipmapVoteSession();
			TelegramBot.sendToAll("<b>Game over</b>: " + state.wave + "/" + state.map.getHightScore());
    	});

    	Events.on(WorldLoadBeginEvent.class, e -> {
    		Vars.state.rules.deconstructRefundMultiplier = .51f;
    	});
    	
    	Events.on(WorldLoadEndEvent.class, e -> {
    		CommandsManager.stopSkipmapVoteSession();
    		ServerEventsManager.worldLoadEnd(e);
    		CommandsManager.clearDoors();
            Map map = maps.getNextMap(state.rules.mode(), state.map);
			TelegramBot.sendToAll("<b>Next map is:</b> " + map.plainName());
    	});
    	
//        Events.on(BuildSelectEvent.class, event -> { 
//        	Unit builder = event.builder;
//    		if(builder == null) return;
//        	BuildPlan buildPlan = builder.buildPlan();
//    		if(buildPlan == null) return;
//            if(!event.breaking && builder.buildPlan().block == Blocks.thoriumReactor && builder.isPlayer()){
//            	Player player = builder.getPlayer();
//                Team team = player.team();
//
//                float thoriumReactorX = event.tile.getX();
//                float thoriumReactorY = event.tile.getY();
//
//                for (CoreBuild core : team.cores()) {
//                	int hypot = (int) Math.ceil(Math.hypot(thoriumReactorX - core.getX(), thoriumReactorY - core.getY())/10);
//                	if(hypot <= 20) {
//                		builder.clearBuilding();
//                		builder.kill();
//                		return;
//                	}
//                }
//            }
//        });

    	/**
    	 * Info message about builder, that building thoriumReactor
    	 */
        Events.on(BlockBuildBeginEvent.class, event -> {
        	Unit builder = event.unit;
    		if(builder == null) return;
        	BuildPlan buildPlan = builder.buildPlan();
    		if(buildPlan == null) return;
            if(!event.breaking && builder.buildPlan().block == Blocks.thoriumReactor && builder.isPlayer()) {
                Player player = builder.getPlayer();
                
                int bx = (event.tile.x * 3 / Vars.world.width()) - 1;
                int by = (event.tile.y * 3 / Vars.world.height()) - 1;

                String position = "";
                if(by == 0) {
                	position = "по центру";
                    if(bx == -1) position += " слева";
                    if(bx == 1) position += " справа";
                }
                if(bx == 0) {
                    if(by == 1) position = "вверху";
                    if(by == -1) position = "внизу";
                	position += " в центре";
                }
                if(bx != 0 && by != 0) { 
                    if(bx == -1) position = "левый";
                    if(bx == 1) position = "правый";
                    if(by == 1) position += " верхний";
                    if(by == -1) position += " нижний";
                	position += " угол карты";
                }
                if(bx == 0 && by == 0) position = "центр карты";
                
//                for (CoreBuild core : team.cores()) {
//                	int hypot = (int) Math.ceil(Math.hypot(thoriumReactorX - core.getX(), thoriumReactorY - core.getY())/10);
//                	if(hypot <= 20) {
//                		Call.sendMessage("[scarlet]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок [" + GameWork.colorToHex(player.color()) + "]" + player.name + " []строит реактор рядом с ядром (" + hypot + " блоках от ядра)");
//                		return;
//                	}
//                }
        		Call.sendMessage("[gold]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок [" + Game.colorToHex(player.color()) + "]" + player.name + " []строит реактор (" + position + ")");
            }
        });
        
    }
    
	private void initColors() {
		Log.info("init colors");
		try {
			BufferedImage colors = ImageIO.read(Game.class.getResourceAsStream("/colors.png"));
			Log.info("file: @", colors);
			TelegramBot.mapColors = new int[Vars.content.blocks().size][];
			Point2 id = new Point2(0,0);
	    	Vars.content.blocks().each(b -> {
	    		int index = id.x++;
				int size = b.size*3;
				TelegramBot.mapColors[index] = new int[size*size];
				for (int i = 0; i < size*size; i++) {
					int rgb = colors.getRGB(id.y/9, id.y%9);
					id.y++;
					if(i == size*size/2) {
						java.awt.Color col = new Color(rgb);
						b.mapColor.set(col.getRed()/255f, col.getGreen()/255f, col.getBlue()/255f);
						b.hasColor = true;
					}
					TelegramBot.mapColors[index][i] = rgb;//b.mapColor.rgb888();
				}
	    	});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
    public void registerServerCommands(CommandHandler handler) {
//    	Log.info("[registerServerCommands]");
		serverHandler = handler;
        handler.register("reactors", "List all thorium reactors in the map.", args -> {
            for(int x = 0; x < Vars.world.width(); x++){
                for(int y = 0; y < Vars.world.height(); y++){
                    if(Vars.world.tile(x, y).block() == Blocks.thoriumReactor && Vars.world.tile(x, y).isCenter()){
                        Log.info("Reactor at @, @", x, y);
                    }
                }
            }
        });
        
        handler.register("cdata", "cdata", args -> {
        	Log.info("Statistics files dir: " + DataCollecter.getPathToFile(""));
        	Log.info("User dir: " + System.getProperty("user.dir"));
        	Log.info("Sleep time: " + dataCollect.getSleepTime());
        });
        
        /** FIXME
        handler.register("event", "[id] [on/off/faston]", "Включить/выключить событие", arg -> {
        	if(arg.length == 0) {
        		StringBuilder msg = new StringBuilder("[red]Недостаточно аргументов.[white]\nID событий:");
        		msg.append(ServerEventsManager.events.toString("\n"));
//        		for (int i = 0; i < ServerEventsManager.events.size; i++) {
//        			msg.append('\n');
//        			ServerEvent event = ServerEventsManager.getServerEvent(i);
//        			msg.append('[');
//        			msg.append(event.getColor());
//        			msg.append(']');
//        			msg.appendev();
//        		}
        		Log.info(msg.toString());
        		return;
        	}
        	if(arg.length == 1) {
//        		for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
//        			ServerEvent event = ServerEventsManager.getServerEvent(i);
//        			if(arg[0].equals(event.getCommandName())) {
//        				player.sendMessage("Событие [" + event.bungle("color") + "]" + event.bungle("name") + "[white] имеет значение: " + event.isRunning());
//        				return;
//        			}
//        		}
        		Log.info("[red]Событие не найдено, [gold]/event [red] для списка событий");
        		return;
        	}
        	if(arg.length == 2) {
        		boolean isOn = false;
        		boolean isFast = false;
        		if(arg[1].equals("on")) {
        			isOn = true;
        		}else if(arg[1].equals("off")) {
        			isOn = false;
        		}else if(arg[1].equals("faston")) {
        			isOn = true;
        			isFast = true;
        		}else {
        			Log.info("Неверный аргумент, используйте [gold]on/off[]");
        			return;
        		}

        		for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
        			ServerEvent event = ServerEventsManager.getServerEvent(i);
        			if(arg[0].equals(event.getCommandName())) {
        				boolean isRunning = event.isRunning();
        				if(isRunning && isOn) {
        					Log.info("[red]Событие уже запущено");
        					return;
        				}
        				if(!isRunning && !isOn) {
        					player.sendMessage("[red]Событие итак не запущено");
        					return;
        				}

        				if(isOn) {
        					if(isFast) {
        						AgzamPlugin.eventsManager.fastRunEvent(event.getCommandName());
        						Log.info("[white]Событие резко запущено! [gold]/sync");
        					} else {
        						AgzamPlugin.eventsManager.runEvent(event.getCommandName());
        						Log.info("[green]Событие запущено!");
        					}
        				} else {
        					AgzamPlugin.eventsManager.stopEvent(event.getCommandName());
        					Log.info("[red]Событие остановлено!");
        				}

        				return;
        			}
        		}

        		Log.info("[red]Событие не найдено, [gold]/event [red] для списка событий");
        		return;
        	}
		});
        	*/
    }
    
    @Override
    public void registerClientCommands(CommandHandler handler) {
    	Log.info("[registerClientCommands]");
    	CommandsManager.registerClientCommands(handler);
    }

	public static String name() {
		return plugin.name;
	}

	public static String version() {
		return plugin.meta.version;
	}
}
