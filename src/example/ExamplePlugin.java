package example;

import arc.*;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Point2;
import arc.util.*;
import mindustry.*;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.entities.abilities.Ability;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.mod.Plugin;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import example.achievements.*;
import example.bot.TelegramBot;
import example.events.*;

import static mindustry.Vars.*;

import java.io.IOException;

import static example.Emoji.*;

public class ExamplePlugin extends Plugin{

	public static final String PLUGIN_NAME = "agzams-plugin";
	public static final String VERSION = "v1.12.0";
    
	public static DataCollecter dataCollect;
	public static ServerEventsManager eventsManager;
	public static AchievementsManager achievementsManager;
	public static CommandsManager commandsManager;
	public static MyMenu menu;
    
    @Override
    public void init() {
    	TelegramBot.init();
    	Admins.init();
    	
    	achievementsManager = new AchievementsManager();
    	commandsManager = new CommandsManager();
    	commandsManager.init();
    	
    	menu = new MyMenu();
    	menu.registerCommand();
    	
    	eventsManager = new ServerEventsManager();
    	eventsManager.init();
    	
    	maps = new Maps();
    	
    	maps.load();
    	
    	
    	dataCollect = new DataCollecter();
    	dataCollect.init();
    	dataCollect.collect();

    	Events.run(Trigger.update, () -> {
    		achievementsManager.update();
    		menu.update();
    		eventsManager.update();
    		dataCollect.update();

//    		Vars.state.rules.defaultTeam.rules().unitBuildSpeedMultiplier *= 2;
//    		Vars.state.rules.defaultTeam.rules().unitCostMultiplier *= 2;
//    		Vars.state.rules.defaultTeam.rules().unitCrashDamageMultiplier *= 2;
//    		Vars.state.rules.defaultTeam.rules().unitDamageMultiplier *= 2;
//    		Vars.state.rules.defaultTeam.rules().unitHealthMultiplier *= 2;
//    		Vars.content.units().each(u -> {var us = Team.sharded.data().getUnits(u); for(var i = 0; i < us.size; i+=2) us.get(i).kill();});
//    		Vars.state.rules.unitCap
    	});

		Events.on(UnitDestroyEvent.class, e -> {
			Log.info(e.unit);
			for (var eff : Vars.content.statusEffects()) {
				if(!e.unit.hasEffect(eff)) continue;
				Log.info(eff.name);
			}
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
    		commandsManager.stopSkipmapVoteSession();
			TelegramBot.sendToAll("<b>Game over</b>: " + state.wave + "/" + state.map.getHightScore());
    	});
    	
    	Events.on(WorldLoadEndEvent.class, e -> {
    		commandsManager.stopSkipmapVoteSession();
    		eventsManager.worldLoadEnd(e);
    		commandsManager.clearDoors();
            Map map = maps.getNextMap(state.rules.mode(), state.map);
			TelegramBot.sendToAll("<b>Next map is:</b> " + map.plainName());
    	});
    	
    	Events.on(PlayerJoin.class, e -> {
			if(e.player != null) TelegramBot.sendToAll("<b>" + e.player.plainName() + "</b> has joined <i>(" + Groups.player.size() + " players)</i>");
    		eventsManager.playerJoin(e);
    		e.player.name(e.player.name().replaceAll(" ", "_"));
    		
			float rate = 1f - (e.player.getInfo().timesKicked * 5 / (float) e.player.getInfo().timesJoined);
			rate = Math.max(rate, 0);
			rate = Math.min(rate, 1f);
			
			if(e.player.getInfo().timesJoined != 1) {
				int startCount = (int) Math.ceil(rate*5);
				StringBuilder stars = new StringBuilder();
				Color color = Color.HSVtoRGB(rate*120, 100, 100);
				int index = CommandsManager.extraStarsUIDD.indexOf(e.player.uuid());
				if(index != -1) {
					color = Color.magenta;
				}
				stars.append("[#");
				stars.append(color.toString());
				stars.append("]");
				int count = 5;
				for (int j = 0; j < startCount; j++) {
					stars.append('\ue809');
					count--;
				}
				Color color2 = Color.HSVtoRGB(rate*120, 100, 33);
				stars.append("[#");
				stars.append(color2.toString());
				stars.append("]");
				for (float j = 0; j < count; j++) {
					stars.append('\ue809');
				}
				
				if(index != -1) {
					stars.append("[magenta]\ue813");
				}
				
				Call.sendMessage("Игрок " + e.player.name() + "[white] имеет рейтинг " + stars.toString());
			} else {
				Call.sendMessage("Игрок " + e.player.name() + "[white] в первый раз на этом сервере!");
			}
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
                Team team = player.team();
                
                float thoriumReactorX = event.tile.getX();
                float thoriumReactorY = event.tile.getY();
                
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
        		Call.sendMessage("[gold]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок [" + GameWork.colorToHex(player.color()) + "]" + player.name + " []строит реактор (" + position + ")");
            }
        });
        
    }
    
    private int ubyte(int b) {
    	if(b < 0) {
    		Log.info(b);
    		return Byte.MAX_VALUE-b;
    	}
    	return b;
	}
    
	private void initColors() {
		Log.info("init colors");
//		Log.info("file: @", Arrays.toString( Vars.tree.get("").list()));
		Fi colors = Vars.tree.get("assets/colors.bin");// new Fi("", FileType.classpath);
		Log.info("file: @", colors);
    	var is = colors.read();
		TelegramBot.mapColors = new int[Vars.content.blocks().size][];
		Point2 id = new Point2(0,0);
    	Vars.content.blocks().each(b -> {
    		try {
    			int index = id.x++;
    			int size = b.size*3;
    			TelegramBot.mapColors[index] = new int[size*size];
//    			Pixmap pixmap = new Pixmap(size, size);
    			for (int i = 0; i < size*size; i++) {
    				b.mapColor.r = ubyte(is.read())/255f;
    				b.mapColor.g = ubyte(is.read())/255f;
    				b.mapColor.b = ubyte(is.read())/255f;
    				b.mapColor.a = ubyte(is.read())/255f;
    				TelegramBot.mapColors[index][i] = b.mapColor.rgb888();
				}
//    			for (int py = 0; py < size; py++) {
//    				for (int px = 0; px < size; px++) {
//						pixmap.setRaw(px, py, TelegramBot.mapColors[index][px+py*size]);
//					}
//				}
//    			Fi.get("C:\\Users\\Agzam\\AppData\\Roaming\\Mindustry\\sdebug\\" + b.name + ".png").writePng(pixmap);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	});
    	try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
    public void registerServerCommands(CommandHandler handler){
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
        
        handler.register("event", "[id] [on/off/faston]", "Включить/выключить событие", arg -> {
        	if(arg.length == 0) {
        		StringBuilder msg = new StringBuilder("[red]Недостаточно аргументов.[white]\nID событий:");
        		for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
        			msg.append('\n');
        			ServerEvent event = ServerEventsManager.getServerEvent(i);
        			msg.append('[');
        			msg.append(event.getColor());
        			msg.append(']');
        			msg.append(event.getCommandName());
        		}
        		Log.info(msg.toString());
        		return;
        	}
        	if(arg.length == 1) {
        		for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
        			ServerEvent event = ServerEventsManager.getServerEvent(i);
        			if(arg[0].equals(event.getCommandName())) {
        				player.sendMessage("Событие [" + event.getColor() + "]" + event.getName() + "[white] имеет значение: " + event.isRunning());
        				return;
        			}
        		}
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
        						ExamplePlugin.eventsManager.fastRunEvent(event.getCommandName());
        						Log.info("[white]Событие резко запущено! [gold]/sync");
        					} else {
        						ExamplePlugin.eventsManager.runEvent(event.getCommandName());
        						Log.info("[green]Событие запущено!");
        					}
        				} else {
        					ExamplePlugin.eventsManager.stopEvent(event.getCommandName());
        					Log.info("[red]Событие остановлено!");
        				}

        				return;
        			}
        		}

        		Log.info("[red]Событие не найдено, [gold]/event [red] для списка событий");
        		return;
        	}
		});
    }
    
    @Override
    public void registerClientCommands(CommandHandler handler){
    	commandsManager.registerClientCommands(handler);
    }
    
}
