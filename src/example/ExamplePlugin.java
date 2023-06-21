package example;

import arc.*;
import arc.graphics.Color;
import arc.util.*;
import mindustry.*;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.mod.Plugin;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import example.achievements.*;
import example.events.*;

import static mindustry.Vars.*;
import static example.Emoji.*;

public class ExamplePlugin extends Plugin{

	public static final String PLUGIN_NAME = "agzams-plugin";
	public static final String VERSION = "v1.9.6";
    
	public static DataCollecter dataCollect;
	public static ServerEventsManager eventsManager;
	public static AchievementsManager achievementsManager;
	public static CommandsManager commandsManager;
	public static MyMenu menu;
    
    @Override
    public void init() {
    	achievementsManager = new AchievementsManager();
    	commandsManager = new CommandsManager();
    	commandsManager.init();
    	
    	menu = new MyMenu();
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
    	});
    	
    	Events.on(WorldLoadEndEvent.class, e -> {
    		commandsManager.stopSkipmapVoteSession();
    		eventsManager.worldLoadEnd(e);
    		commandsManager.clearDoors();
    	});
    	
    	Events.on(PlayerJoin.class, e -> {
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
    	
        Events.on(BuildSelectEvent.class, event -> { 
        	Unit builder = event.builder;
    		if(builder == null) return;
        	BuildPlan buildPlan = builder.buildPlan();
    		if(buildPlan == null) return;
    		Block block = buildPlan.block;
    		
            if(!event.breaking && builder.buildPlan().block == Blocks.thoriumReactor && builder.isPlayer()){
            	Player player = builder.getPlayer();
                Team team = player.team();

                float thoriumReactorX = event.tile.getX();
                float thoriumReactorY = event.tile.getY();

                for (CoreBuild core : team.cores()) {
                	int hypot = (int) Math.ceil(Math.hypot(thoriumReactorX - core.getX(), thoriumReactorY - core.getY())/10);
                	if(hypot <= 20) {
                		builder.clearBuilding();
                		builder.kill();
                		return;
                	}
                }
            }
        });

    	/**
    	 * Info message about builder, that building thoriumReactor
    	 */
        Events.on(BlockBuildBeginEvent.class, event -> {
        	Unit builder = event.unit;
    		if(builder == null) return;
        	BuildPlan buildPlan = builder.buildPlan();
    		if(buildPlan == null) return;
    		Block block = buildPlan.block;
    		
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
                
                for (CoreBuild core : team.cores()) {
                	int hypot = (int) Math.ceil(Math.hypot(thoriumReactorX - core.getX(), thoriumReactorY - core.getY())/10);
                	if(hypot <= 20) {
                		Call.sendMessage("[scarlet]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок [" + GameWork.colorToHex(player.color()) + "]" + player.name + " []строит реактор рядом с ядром (" + hypot + " блоках от ядра)");
                		return;
                	}
                }
        		Call.sendMessage("[gold]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок [" + GameWork.colorToHex(player.color()) + "]" + player.name + " []строит реактор (" + position + ")");
            }
        });
        
    }
    
    
    public void test() {
    	Groups.puddle.each(e -> {if(e.liquid == Liquids.cryofluid) e.tile.setBlock(Blocks.cryofluid);});
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
    }
    
    @Override
    public void registerClientCommands(CommandHandler handler){
    	commandsManager.registerClientCommands(handler);
    	menu.registerCommand(handler);
    }
    
}
