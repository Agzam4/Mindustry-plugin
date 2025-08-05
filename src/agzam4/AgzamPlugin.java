package agzam4;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.mod.Mods.LoadedMod;
import mindustry.mod.Plugin;

import agzam4.achievements.*;
import agzam4.admins.Admins;
import agzam4.bot.Bots;
import agzam4.bot.Bots.NotifyTag;
import agzam4.bot.TelegramBot;
import agzam4.database.Database;
import agzam4.events.EventMap;
import agzam4.events.ServerEventsManager;
import agzam4.net.NetMenu;
import agzam4.utils.Log;

import static agzam4.Emoji.*;
import static mindustry.Vars.*;

public class AgzamPlugin extends Plugin {

	public static LoadedMod plugin; 
	
	public static DataCollecter dataCollect;
	public static AchievementsManager achievementsManager;

	public static CommandHandler serverHandler;
	public static CommandHandler clientHandler;
    
    @Override
    public void init() {
    	plugin = Vars.mods.getMod("agzam4plugin");
    	Log.init();
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
    	Log.reset();
    	
    	ServerEventsManager.init();
    	EventMap.load();

    	CommandsManager.flushBotCommands();
    	
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
        	Images.init();
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
			Bots.notify(NotifyTag.round, "<b>Game over</b>: " + state.wave + "/" + state.map.getHightScore());
    	});

    	Events.on(WorldLoadBeginEvent.class, e -> {
    		Vars.state.rules.deconstructRefundMultiplier = .51f;
    	});
    	
    	Events.on(WorldLoadEndEvent.class, e -> {
    		CommandsManager.stopSkipmapVoteSession();
    		ServerEventsManager.worldLoadEnd(e);
    		CommandsManager.clearDoors();
//            Map map = maps.getNextMap(state.rules.mode(), state.map);
    		Timer.schedule(() -> {
    			Bots.notify(NotifyTag.round, Strings.format("<b>Next map is:</b> <code>@</code>", TelegramBot.strip(state.map.plainName())));
    		}, 1f);
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
    
	@Override
    public void registerServerCommands(CommandHandler handler) {
		serverHandler = handler;
    	CommandsManager.flushServerCommands();
    }
    
    @Override
    public void registerClientCommands(CommandHandler handler) {
    	clientHandler = handler;
    	CommandsManager.flushClientCommands();
    }

	public static String name() {
		return plugin.name;
	}

	public static String version() {
		return plugin.meta.version;
	}
}
