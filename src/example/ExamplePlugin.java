package example;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.plaf.nimbus.State;

import arc.*;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.*;
import arc.util.CommandHandler.Command;
import example.events.ServerEvent;
import example.events.ServerEventsManager;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.game.EventType;
import mindustry.game.EventType.*;
import mindustry.game.GameStats;
import mindustry.game.Gamemode;
import mindustry.mod.*;
import mindustry.net.Administration;
import mindustry.net.Administration.*;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.UnitType;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.maps.Map;
import mindustry.maps.*;

import static mindustry.Vars.*;

public class ExamplePlugin extends Plugin{

    private @Nullable Map nextMapOverride;
    private boolean inGameOverWait;
    private Gamemode lastMode;
    private GameState e;
    private Maps maps;
    private Administration admins = new Administration();
    
    private float lastThoriumReactorAlertX, lastThoriumReactorAlertY;
    
    private World world;
    private String worldinfo = "";
	private boolean chatFilter;
	
	private DataCollecter dataCollect;
    

    private static final char emojiCopper = 		'\uf838';
    private static final char emojiLead = 			'\uf837';
    private static final char emojiMetaglass = 		'\uf836';
    private static final char emojiGraphite = 		'\uf835';
    private static final char emojiSand = 			'\uf834';
    private static final char emojiCoal = 			'\uf833';
    private static final char emojiTitanium = 		'\uf832';
    private static final char emojiThorium = 		'\uf831';
    private static final char emojiScrap = 			'\uf830';
    private static final char emojiSilicon = 		'\uf82f';
    private static final char emojiPlastanium = 	'\uf82e';
    private static final char emojiPhaseFabric = 	'\uf82d';
    private static final char emojiSurgeAlloy = 	'\uf82c';
    private static final char emojiSporePod = 		'\uf82b';
    private static final char emojiBlastCompound = 	'\uf82a';
    private static final char emojiPyratite = 		'\uf829';

    private static final char emojiWater = 		'\uf828';
    private static final char emojiSlag = 		'\uf827';
    private static final char emojiOil = 		'\uf826';
    private static final char emojiCryofluid = 	'\uf825';
    
    private static final char emojiAlert = 		'\u26a0';
//    private static final char emojiWater = 		'\uf826';



    private static final char[] oreblocksemoji = new char[] {
    		emojiCopper,
    		emojiLead,
    		emojiScrap,
    		emojiSand,
    		emojiCoal,
    		emojiTitanium,
    		emojiThorium
    };

    private static final char[] liquidsemoji = new char[] {
    		emojiWater,
    		emojiOil,
    		emojiSlag,
    		emojiCryofluid
    };

    private ServerEventsManager eventsManager;
    private Team admin;
    
    
    private int ignoreUnitId = -1;
//    private 
    
    private int updateId = 0;
    
    
    SkipmapVoteSession[] currentlyMapSkipping = {null};
//    
    
    
    MyMenu menu;
    // TODO: bans command
    
    //called when game initializes
    @Override
    public void init() {
    	
    	menu = new MyMenu();
    	eventsManager = new ServerEventsManager();
    	eventsManager.init();
    	
    	admin = Team.all[10];
    	admin.name = "admin";
    	
//    	admin.rules().
//    	eventsManager.isEventsOn[0] = true;
//		eventsManager.startEventsLoop();
//    	eventsManager.startEventsLoop();
    	
    	maps = new Maps();
    	maps.load();
    	
    	dataCollect = new DataCollecter();
    	dataCollect.init();
    	dataCollect.collect();
    	
    	adminCommands = new ArrayList<>();
    	adminCommands.add("fillitems");
    	adminCommands.add("admin");
    	adminCommands.add("chatfilter");
    	adminCommands.add("dct");
    	adminCommands.add("event");
    	adminCommands.add("team");
    	adminCommands.add("sandbox");
    	adminCommands.add("unit");
    	adminCommands.add("bans");
    	adminCommands.add("unban");
    	adminCommands.add("m");
    	adminCommands.add("js");
    	adminCommands.add("link");
    	
    	Events.run(Trigger.update, () -> {
    		menu.update();
    		eventsManager.update();
    	});
    	
    	Events.on(GameOverEvent.class, e -> {
    		StringBuilder result = new StringBuilder(state.map.name());
    		result.append("\nСчёт: [lightgray]");
    		result.append(state.wave);
    		result.append('/');
    		result.append(state.map.getHightScore());
    		if(state.wave > state.map.getHightScore()) {
        		result.append("[gold] (Новый рекорд!)");
    		}
    		Call.sendMessage(result.toString());
    		currentlyMapSkipping[0] = null;
    		
    		state.map.setHighScore(state.wave);
    	});
    	
    	Events.on(WorldLoadEndEvent.class, e -> {
    		currentlyMapSkipping[0] = null;
    		eventsManager.worldLoadEnd(e);
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
				stars.append("[#");
				stars.append(color.toString());
				stars.append("]");
				for (int j = 0; j < startCount; j++) {
					stars.append('\ue809');
				}
				Color color2 = Color.HSVtoRGB(rate*120, 100, 33);
				stars.append("[#");
				stars.append(color2.toString());
				stars.append("]");
				for (float j = startCount+1; j < 5; j++) {
					stars.append('\ue809');
				}
				Call.sendMessage("Игрок " + e.player.name() + "[white] имеет рейтинг " + stars.toString());
			} else {
				Call.sendMessage("Игрок " + e.player.name() + "[white] в первый раз на этом сервере!");
			}
    	});
    	
//    	Blocks.
//    	Events.on(GameOverEvent.class, event -> {
//    		if(inGameOverWait) return;
//     		Map map = nextMapOverride != null ? nextMapOverride : maps.getNextMap();
//    		nextMapOverride = null;
//    	});
//    	Events.on(ServerLoadEvent.class, event -> {
//    	});
//    	
//    	Events.on(PlayerJoin.class, event -> {
//    	});
//    	Events.on(Event.class, event -> {
//    	});
    	
//    	Events.on(BlockDestroyEvent.class, event -> {
//    		event.tile.
//    	});
    	
    	/**
    	 * killing builder, that building thoriumReactor near core
    	 */
        Events.on(BuildSelectEvent.class, event -> { 
        	Unit builder = event.builder;
            if(!event.breaking && builder != null && builder.buildPlan() != null && builder.buildPlan().block == Blocks.thoriumReactor && builder.isPlayer()){
                
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
        	
            if(!event.breaking && builder != null && builder.buildPlan() != null && builder.buildPlan().block == Blocks.thoriumReactor && builder.isPlayer()){
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
                		Call.sendMessage("[scarlet]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок [" + colorToHex(player.color()) + "]" + player.name + " []строит реактор рядом с ядром (" + hypot + " блоках от ядра)");
                		return;
                	}
                }
        		Call.sendMessage("[gold]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок [" + colorToHex(player.color()) + "]" + player.name + " []строит реактор (" + position + ")");
            }
        });
        
        // add a chat filter that changes the contents of all messages
        Vars.netServer.admins.addChatFilter((player, text) -> {

//    		char[] msg = text.toCharArray();
//    		
//    		String searchName = "";
//    		int searchNameStart = -1;
//    		boolean needSearchName = false;
//    		
//        	for (int i = 0; i < msg.length; i++) {
//        		if(msg[i] == '@') {
//        			searchName = "";
//        			searchNameStart = i;
//        			needSearchName = true;
//        		}
//        		
//        		if(needSearchName) {
//            		if(msg[i] == ' ') {
//                        ObjectSet<PlayerInfo> infos = netServer.admins.searchNames(arg[0]);
//
//                        if(infos.size > 0) {
//                            info("Players found: @", infos.size);
//
//                            int i = 0;
//                            for(PlayerInfo info : infos){
//                                info("- [@] '@' / @", i++, info.plainLastName(), info.id);
//                            }
//                        }
//                        
//            			searchName = "";
//            			needSearchName = false;
//            			searchNameStart = -1;
//            			
//            		} else {
//            			searchName += msg[i];
//            		}
//        		}
//			}
        	
        	if(chatFilter) {
        		text = "[white]" + text + "[white]";
        		char[] msg = text.toCharArray();
        		
        		StringBuilder result = new StringBuilder();

        		int noobI = -1;
        		StringBuilder noob = new StringBuilder();
        		for (int i = 0; i < msg.length; i++) {
					char c = msg[i];
					char uc = Character.toUpperCase(c);
					if(noob.length() == 0 && (uc == 'N' || uc == 'Н')) {
						noob.append(c);
						noobI = i;
					} else if(uc == 'O' || uc == 'У' || uc == 'Y') {
						noob.append(c);
					} else if(uc == 'Б' || uc == 'B') {
						noob.append(c);
					} else {
						if(noob.length() > 2) {
							boolean isUpper = Character.isUpperCase(noob.charAt(0));
							char end = Character.toUpperCase(noob.charAt(noob.length()-1));
							if(end == 'B' || end == 'Б') {
								result.delete(noobI, noobI+noob.length());
								if(end == 'B') {
									result.append(isUpper ? "Pr" : "pr");
									for (int j = 0; j < noob.length()-3; j++) {
										result.append('o');
									}
								} else {
									result.append(isUpper ? "Пр" : "пр");
									for (int j = 0; j < noob.length()-2; j++) {
										result.append('о');
									}
								}
							}
						}
						noob.delete(0, noob.length());
					}
					result.append(msg[i]);
				}
        		text = result.toString();
        	}
        	dataCollect.messageEvent(player, text);
        	return text;
        });

        //add an action filter for preventing players from doing certain things
//        Vars.netServer.admins.addActionFilter(action -> {
//            //random example: prevent blast compound depositing
//            if(action.type == ActionType.depositItem && action.item == Items.blastCompound && action.tile.block() instanceof CoreBlock){
//                action.player.sendMessage("Example action filter: Prevents players from depositing blast compound into the core.");
//                return false;
//            }
//            return true;
//        });
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
        	Log.info("Statistics file: " + DataCollecter.getPathToFile());
        	Log.info("User dir: " + System.getProperty("user.dir"));
        	Log.info("Sleep time: " + dataCollect.getSleepTime());
        });
    }
    
    private ArrayList<String> adminCommands;

    @Override
    public void registerClientCommands(CommandHandler handler){
    	handler.removeCommand("help");
    	
    	handler.<Player>register("help", "[страница]", "Список всех команд", (args, player) -> {
    		boolean isAdmin = player.admin();
    		int coummandsCount = handler.getCommandList().size;
    		if(!isAdmin) coummandsCount -= adminCommands.size();
    		
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                player.sendMessage("[scarlet]\"страница\" может быть только числом.");
                return;
            }
            int commandsPerPage = 6;
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil((float)coummandsCount / commandsPerPage);

            page--;

            if(page >= pages || page < 0){
                player.sendMessage("[scarlet]\"страница\" должна быть числом между[orange] 1[] и[orange] " + pages + "[scarlet].");
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Страница команд[lightgray] @[gray]/[lightgray]@[orange] --\n\n", (page + 1), pages));
            
            for(int i = commandsPerPage * page; i < Math.min(commandsPerPage * (page + 1), handler.getCommandList().size); i++){
                Command command = handler.getCommandList().get(i);
                boolean isAdminCommand = adminCommands.indexOf(command.text) != -1;
                if(isAdminCommand && !isAdmin) continue;
                result.append("[orange] /").append(command.text).append("[white] ").append(command.paramText).append("[lightgray] - ").append(command.description + (isAdminCommand ? " [red] Только для администраторов" : "")).append("\n");
            }
            player.sendMessage(result.toString());
        });
    	
    	menu.registerCommand(handler);
    	
    	/**
    	 * List of server maps
    	 */
        handler.<Player>register("maps", "[all/custom/default]", "Показывает список доступных карт. Отображает все карты по умолчанию", (arg, player) -> {
        	boolean custom  = arg.length == 0 || (arg[0].equals("custom")  || arg[0].equals("all"));
            boolean def     = arg.length == 0 || (arg[0].equals("default") || arg[0].equals("all"));
            if(!maps.all().isEmpty()){
                Seq<Map> all = new Seq<>();

                if(custom) all.addAll(maps.customMaps());
                if(def) all.addAll(maps.defaultMaps());

                if(all.isEmpty()){
					player.sendMessage("Кастомные карт нет на этом сервере, используйте [gold]all []аргумет.");
                }else{
                    player.sendMessage("[white]Maps:");
                    for(Map map : all){
                        String mapName = Strings.stripColors(map.name()).replace(' ', '_');
                        if(map.custom){
                            player.sendMessage(" [gold]Custom [white]| " + mapName + " (" + map.width + "x" + map.height + ")");
                        }else{
                            player.sendMessage(" [gray]Default [white]| " + mapName + " (" + map.width + "x" + map.height + ")");
                        }
                    }
                }
            }else{
            	player.sendMessage("Карты не найдены");
            }
        });

        handler.<Player>register("discord", "", "\ue80d Сервера", (arg, player) -> {
        	if(discordLink == null) {
        		player.sendMessage("[red]\ue80d Ссылка отсутствует");
        	} else {
        		if(discordLink.isEmpty()) {
        			player.sendMessage("[red]\ue80d Ссылка отсутствует");
        		} else {
        			Call.openURI(player.con, discordLink);
        		}
        	}
        });
        
        /**
         * Plugin developer command (to find) emoji codes
         */
//        handler.<Player>register("emoji", "[start] [count]", "", (arg, player) -> {
//        	int count = 100;
//        	int start = 0;
//        	try {
//        		start = Integer.parseInt(arg[0]);
//			} catch (Exception e) {
//			}
//        	try {
//        		count = Integer.parseInt(arg[1]);
//			} catch (Exception e) {
//			}
//        	StringBuilder sb = new StringBuilder();
//        	for (char c = (char) ('\ue800' + start); c < '\ue800' + count + start; c++) {
//				sb.append(c);
//			}
//            player.sendMessage(sb.toString());
//        });
        
        /**
         * Map recourses statistic
         */
        handler.<Player>register("mapinfo", "", "Показывает статистику ресурсов карты", (arg, player) -> {
    		world = Vars.world;

			final Item itemDrops[] = new Item[] {
					Items.copper,
					Items.lead,
					Items.scrap,
					Items.sand,
					Items.coal,
					Items.titanium,
					Items.thorium
			};
			
			final Liquid liquidDrops[] = new Liquid[] {
					Liquids.water,
					Liquids.oil,
					Liquids.slag,
					Liquids.cryofluid
			};

    		int counter[] = new int[itemDrops.length];
    		int lcounter[] = new int[liquidDrops.length];
    		
    		int summaryCounter = 0;
    		int typesCounter = 0;
    		
    		for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                	Item floor = world.tile(x, y).floor().itemDrop;
                	Item overlay = world.tile(x, y).overlay().itemDrop;
                	Liquid lfloor = world.tile(x, y).floor().liquidDrop;
                	Liquid loverlay = world.tile(x, y).overlay().liquidDrop;

					for (int i = 0; i < counter.length; i++) {
						if(itemDrops[i] == overlay || itemDrops[i] == floor) {
							if(counter[i] == 0) {
								typesCounter++;
							}
							counter[i]++;
							summaryCounter++;
						}
					}
					
					for (int i = 0; i < liquidDrops.length; i++) {
						if(liquidDrops[i] == loverlay || liquidDrops[i] == lfloor) {
							lcounter[i]++;
						}
					}
                }
            }
    		
    		StringBuilder worldInfo = new StringBuilder();
    		
    		if(summaryCounter == 0) return;

			worldInfo.append("Информация о карте:\n");
			worldInfo.append("Название: " + Vars.state.map.name() + "\n");
			worldInfo.append("Рекорд: " + Vars.state.map.getHightScore());
			worldInfo.append("Ресурсы:\n");
    		for (int i = 0; i < counter.length; i++) {
    			float cv = ((float)counter[i])*typesCounter/summaryCounter/3f;
    			if(cv > 1/3f) cv = 1/3f;
    			int percent = (int) Math.ceil(counter[i]*100d/summaryCounter);
    			java.awt.Color c = new java.awt.Color(java.awt.Color.HSBtoRGB(cv, 1, 1));
    			worldInfo.append(oreblocksemoji[i]);
    			worldInfo.append('[');
    			worldInfo.append(String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()));
    			worldInfo.append("]: ");
    			if(counter[i] > 0) {
        			worldInfo.append(counter[i]);
        			worldInfo.append(" (");
        			worldInfo.append(percent);
        			worldInfo.append("%)");
    			} else {
        			worldInfo.append("-");
    			}
    			worldInfo.append("\n[white]");
			}

			worldInfo.append("Жидкости:");
			boolean isLFound = false;
    		for (int i = 0; i < lcounter.length; i++) {
    			if(lcounter[i] > 0) {
        			worldInfo.append("\n[white]");
        			worldInfo.append(liquidsemoji[i]);
        			worldInfo.append("[lightgray]: ");
        			worldInfo.append(counter[i]);
        			isLFound = true;
    			}
			}
    		if(!isLFound) {
    			worldInfo.append(" [red]нет");
    		}
    		worldinfo = worldInfo.toString();
            player.sendMessage(worldinfo);
        });
        
//        handler.register("nextmap", "<mapname...>", "Set the next map to be played after a game-over. Overrides shuffling.", arg -> {
//        	Core.app.
//        	Map res = maps.all().find(map -> Strings.stripColors(map.name().replace('_', ' ')).equalsIgnoreCase(Strings.stripColors(arg[0]).replace('_', ' ')));
//            if(res != null){
//                nextMapOverride = res;
//                info("Next map set to '@'.", Strings.stripColors(res.name()));
//            }else{
//                err("No map '@' found.", arg[0]);
//            }
//        });
        
//        handler.<Player>register("griefer", "[имя_грифера]", "Лучший способ наказать наказать грифера", (arg, player) -> {
//            if(Groups.player.size() < 3){ // FIXME
//                player.sendMessage("[scarlet]Для начала голосования необходимо как минимум 3 игрока.");
//                return;
//            }
//
//            if(grieferVoteSession[0] != null){
//                player.sendMessage("[scarlet]Голосование уже идет."); // FIXME
//                return;
//            }
//
//            if(arg.length == 0){
//                StringBuilder builder = new StringBuilder();
//                builder.append("[orange]Список игроков: \n");
//
//                Groups.player.each(p -> !p.admin && p.con != null && p != player, p -> {
//                    builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id()).append(")\n");
//                });
//                player.sendMessage(builder.toString());
//            }else{
//                Player found;
//                if(arg[0].length() > 1 && arg[0].startsWith("#") && Strings.canParseInt(arg[0].substring(1))){
//                    int id = Strings.parseInt(arg[0].substring(1));
//                    found = Groups.player.find(p -> p.id() == id);
//                }else{
//                    found = Groups.player.find(p -> p.name.equalsIgnoreCase(arg[0]));
//                }
//
//                if(found != null){
//                    if(found == player){
//                        player.sendMessage("[scarlet]Вы не можете проголосовать за то, чтобы наказать себя.");
//                    }else if(found.admin){
//                        player.sendMessage("[scarlet]Вы действительно думали, что сможете наказать администратора?");
//                    }else if(found.isLocal()){
//                        player.sendMessage("[scarlet]Локальные игроки не могут быть наказаны.");
//                    }else if(found.team() != player.team()){
//                        player.sendMessage("[scarlet]Только игроки Вашей команды могут быть наказаны");
//                    }else{
//                    	GrieferVoteSession session = new GrieferVoteSession(grieferVoteSession, found);
//                        grieferVoteSession[0] = session;
//                        session.vote(player, 1);
//                    }
//                }else{
//                    player.sendMessage("[scarlet]Игрок с именем [orange]'" + arg[0] + "'[scarlet] не найден.");
//                }
//            }
//        });
        
//        handler.<Player>register("gvote", "<y/n>", "Голосовать за наказание для игрока", (arg, player) -> {
//            if(grieferVoteSession[0] == null){
//                player.sendMessage("[scarlet]Никто ни за кого не голосует.");
//            }else{
//                if(player.isLocal()){
//                    player.sendMessage("[scarlet]Локальные игроки не могут голосовать. Вместо этого кикните игрока сами.");
//                    return;
//                }
//
//                //hosts can vote all they want
//                if((grieferVoteSession[0].voted.contains(player.uuid()) || grieferVoteSession[0].voted.contains(admins.getInfo(player.uuid()).lastIP))){
//                    player.sendMessage("[scarlet]Вы уже проголосовали.");
//                    return;
//                }
//
//                if(grieferVoteSession[0].target == player){
//                    player.sendMessage("[scarlet]Вы не можете голосовать за себя");
//                    return;
//                }
//
//                if(grieferVoteSession[0].target.team() != player.team()){
//                    player.sendMessage("[scarlet]Только игроки Вашей команды могут быть наказаны");
//                    return;
//                }
//                
//                int sign = 0;
//                String signString = arg[0].toLowerCase();
//                if(signString.equals("y") || signString.equals("yes")) sign = 1;
//                if(signString.equals("n") || signString.equals("no")) sign = -1;
//
//                if(sign == 0){
//                    player.sendMessage("[scarlet]Голосуйте либо \"y\" (да), либо \"n\" (нет).");
//                    return;
//                }
//
//                grieferVoteSession[0].vote(player, sign);
//            }
//        });
        
        /**
         *  SKIP MAP COMMANDS
         */
        
//        SkipmapVoteSession[] currentlyMapSkipping = {null};
        
        handler.<Player>register("skipmap", "Начать голосование за пропуск карты", (arg, player) -> {
        	if(currentlyMapSkipping[0] == null) {
            	SkipmapVoteSession session = new SkipmapVoteSession(currentlyMapSkipping);
                session.vote(player, 1);
                currentlyMapSkipping[0] = session;
        	} else {
                player.sendMessage("[scarlet]Голосование уже идет: [gold]/smvote <y/n>");
//                if(player.isLocal()){
//                    player.sendMessage("[scarlet]Локальные игроки не могут голосовать.");
//                    return;
//                }
//                
//                if((currentlyMapSkipping[0].voted.contains(player.uuid()) || currentlyMapSkipping[0].voted.contains(admins.getInfo(player.uuid()).lastIP))){
//                    player.sendMessage("[scarlet]Ты уже проголосовал. Молчи!");
//                    return;
//                }
//
//                currentlyMapSkipping[0].vote(player, 1);
        	}
        });
    	
        handler.<Player>register("smvote", "<y/n>", "Проголосовать за/протов пропуск карты", (arg, player) -> {
            if(currentlyMapSkipping[0] == null){
                player.sendMessage("[scarlet]Nobody is being voted on.");
            }else{
                if(player.isLocal()){
                    player.sendMessage("[scarlet]Локальные игроки не могут голосовать.");
                    return;
                }
                
                if((currentlyMapSkipping[0].voted.contains(player.uuid()) || currentlyMapSkipping[0].voted.contains(admins.getInfo(player.uuid()).lastIP))){
                    player.sendMessage("[scarlet]Ты уже проголосовал. Молчи!");
                    return;
                }
                
                String voteSign = arg[0].toLowerCase();
                
                int sign = 0;
                if(voteSign.equals("y")) sign = +1;
                if(voteSign.equals("n")) sign = -1;

                if(sign == 0){
                    player.sendMessage("[scarlet]Голосуйте либо \"y\" (да), либо \"n\" (нет)");
                    return;
                }

                currentlyMapSkipping[0].vote(player, sign);
            }
        });
        
        handler.<Player>register("plugininfo", "info about pluging", (arg, player) -> {
        	player.sendMessage(""
        			+ "[green] Agzam's plugin v1.8.2\n"
        			+  "[gray]========================================================\n"
        			+ "[white] Added [royal]skip map[white] commands\n"
        			+ "[white] Added protection from [violet]thorium reactors[white]\n"
        			+ "[white] Added map list command\n"
        			+ "[white] Added fill items by type to core command\n"
        			+ "[white] Added Map recourses statistic command\n"
        			+ "[white] and more other\n"
        			+  "[gray] Download: github.com/Agzam4/Mindustry-plugin");
        			
        });
        
        handler.<Player>register("fillitems", "[item] [count]", "Заполните ядро предметами", (arg, player) -> {
        	if(player.admin()) {
        		try {
        			

        			final Item serpuloItems[] = {
        					Items.scrap, Items.copper, Items.lead, Items.graphite, Items.coal, Items.titanium, Items.thorium, Items.silicon, Items.plastanium,
        					Items.phaseFabric, Items.surgeAlloy, Items.sporePod, Items.sand, Items.blastCompound, Items.pyratite, Items.metaglass
        			};
        			
        			final Item erekirOnlyItems[] = {
							/* Items.graphite, */ /* Items.thorium, */ /* Items.silicon, */ /* Items.phaseFabric, */ /*Items.surgeAlloy,*/ /*Items.sand,*/
        					Items.beryllium, Items.tungsten, Items.oxide, Items.carbide, Items.fissileMatter, Items.dormantCyst
        			};
        			
        			if(Items.copper.localizedName.equalsIgnoreCase(Items.copper.name)) {
        		    	Items.beryllium.localizedName = "бериллий";
        		    	Items.blastCompound.localizedName = "взрывчатка";
        		    	Items.carbide.localizedName = "карбид";
        		    	Items.coal.localizedName = "уголь";
        		    	Items.copper.localizedName = "медь";
        		    	Items.dormantCyst.localizedName = "оболчка";
        		    	Items.fissileMatter.localizedName = "материя";
        		    	Items.graphite.localizedName = "грфит";
        		    	Items.lead.localizedName = "свинец";
        		    	Items.metaglass.localizedName = "стекло";
        		    	Items.oxide.localizedName = "оксид";
        		    	Items.phaseFabric.localizedName = "фаза";
        		    	Items.plastanium.localizedName = "пластан";
        		    	Items.pyratite.localizedName = "пиротит";
        		    	Items.sand.localizedName = "песок";
        		    	Items.scrap.localizedName = "железо";
        		    	Items.silicon.localizedName = "кремний";
        		    	Items.sporePod.localizedName = "споры";
        		    	Items.surgeAlloy.localizedName = "кинетик";
        		    	Items.thorium.localizedName = "торий";
        		    	Items.titanium.localizedName = "титан";
        		    	Items.tungsten.localizedName = "вольфрам";
        		    	Items.fissileMatter.localizedName = "материя";
        		    	Items.dormantCyst.localizedName = "обломки";
        			}
            		if(arg.length == 0) {
            			StringBuilder ruNames = new StringBuilder("Русские названия предметов: ");
            			Log.info("Ru names");
            			for (int i = 0; i < serpuloItems.length; i++) {
                			Log.info(i + "s) " + serpuloItems[i].name);
							ruNames.append(getColoredLocalizedItemName(serpuloItems[i]));
							ruNames.append(", ");
						}
            			for (int i = 0; i < erekirOnlyItems.length; i++) {
                			Log.info(i + "e) " + serpuloItems[i].name);
							ruNames.append(getColoredLocalizedItemName(erekirOnlyItems[i]));
							if(i + 1 < erekirOnlyItems.length) {
								ruNames.append(", ");
							}
						}
    					player.sendMessage(ruNames.toString());
    					return;
            		}
            		
            		Item item = null;
            		
            		String itemname = arg[0].toLowerCase();

            		for (int i = 0; i < serpuloItems.length; i++) {
						Item si = serpuloItems[i];
						if(itemname.equalsIgnoreCase(si.name) || itemname.equalsIgnoreCase(si.localizedName)) {
							item = si;
							break;
						}
					}
            		if(item == null) {
                		for (int i = 0; i < erekirOnlyItems.length; i++) {
    						Item ei = erekirOnlyItems[i];
    						if(itemname.equalsIgnoreCase(ei.name) || itemname.equalsIgnoreCase(ei.localizedName)) {
    							item = ei;
    							break;
    						}
    					}
            		}
            		if(item == null) {
                		if(itemname.equals("\uf82a")) 	item = Items.blastCompound;
                		else if(itemname.equals("\uf833")) 	item = Items.coal;
                		else if(itemname.equals("\uf838")) 	item = Items.copper;
                		else if(itemname.equals("\uf835")) 	item = Items.graphite;
                		else if(itemname.equals("\uf837")) 	item = Items.lead;
                		else if(itemname.equals("\uf836")) 	item = Items.metaglass;
                		else if(itemname.equals("\uf82d")) 	item = Items.phaseFabric;
                		else if(itemname.equals("\uf82e")) 	item = Items.plastanium;
                		else if(itemname.equals("\uf829")) 	item = Items.pyratite;
                		else if(itemname.equals("\uf834")) 	item = Items.sand;
                		else if(itemname.equals("\uf830")) 	item = Items.scrap;
                		else if(itemname.equals("\uf82f")) 	item = Items.silicon;
                		else if(itemname.equals("\uf82b")) 	item = Items.sporePod;
                		else if(itemname.equals("\uf82c")) 	item = Items.surgeAlloy;
                		else if(itemname.equals("\uf831")) 	item = Items.thorium;
                		else if(itemname.equals("\uf832")) 	item = Items.titanium;
            		}
            		
            		if(item == null) {
            			if(itemname.equalsIgnoreCase(Items.dormantCyst.name) || itemname.equalsIgnoreCase(Items.dormantCyst.localizedName)) {
            				item = Items.dormantCyst;
            			}
            			if(itemname.equalsIgnoreCase(Items.fissileMatter.name) || itemname.equalsIgnoreCase(Items.fissileMatter.localizedName)) {
            				item = Items.fissileMatter;
            			}
            		}
            		if(item != null) {
            			Team team = player.team();

            			int count = arg.length > 1 ? Integer.parseInt(arg[1]) : 0;
            			for(CoreBuild core : team.cores()){
            				core.items.add(item, count);
            			}
            			player.sendMessage("Added " + "[gold]x" + count + " [orange]" + item.name);
            		}
				} catch (Exception e) {
					player.sendMessage(e.getMessage());
				}
        	} else {
        		admins = new Administration();
				player.sendMessage("[red]Команда только для администраторов");
        	}
        });
        
        handler.<Player>register("admin", "[add/remove] [name]", "Добавить/удалить админа", (arg, player) -> {
        	if(player.admin()) {
        		if(arg.length != 2 || !(arg[0].equals("add") || arg[0].equals("remove"))){
        			player.sendMessage("[red]Second parameter must be either 'add' or 'remove'.");
                    return;
                }

                boolean add = arg[0].equals("add");

                PlayerInfo target;
                Player playert = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[1])));
                if(playert != null){
                    target = playert.getInfo();
                }else{
                    target = Vars.netServer.admins.getInfoOptional(arg[1]);
                    playert = Groups.player.find(p -> p.getInfo() == target);
                }
                
                if(playert.uuid() == player.uuid()) {
    				player.sendMessage("[red] Вы не можете изменить свой статус");
    				return;
                }

                if(target != null){
                    if(add){
                    	Vars.netServer.admins.adminPlayer(target.id, playert == null ? target.adminUsid : playert.usid());
                    }else{
                    	Vars.netServer.admins.unAdminPlayer(target.id);
                    }
                    if(playert != null) playert.admin = add;
                    player.sendMessage("[gold]Изменен статус администратора игрока: [" + colorToHex(playert.color) + "]" + Strings.stripColors(target.lastName));
                }else{
                	player.sendMessage("[red]Игрока с таким именем или ID найти не удалось. При добавлении администратора по имени убедитесь, что он подключен к Сети; в противном случае используйте его UUID");
                }
            	Vars.netServer.admins.save();
        	} else {
        		admins = new Administration();
				player.sendMessage("[red]Команда только для администраторов");
        	}
        });

        handler.<Player>register("chatfilter", "[on/off]", "Включить/выключить фильтр чата", (arg, player) -> {
        	if(player.admin()) {
        		if(arg.length == 0) {
    				player.sendMessage("Недостаточно аргументов");
    				return;
        		}
        		if(arg[0].equals("on")) {
    				chatFilter = true;
    				player.sendMessage("[green]Чат фильтр включен");
        		}else if(arg[0].equals("off")) {
        			chatFilter = false;
    				player.sendMessage("[red]Чат фильтр выключен");
        		}else {
    				player.sendMessage("Неверный аргумент, используйте [gold]on/off");
    				return;
        		}
        	} else {
        		admins = new Administration();
				player.sendMessage("[red]Команда только для администраторов");
        	}
        });
        
        handler.<Player>register("dct", "[time]", "Установить интервал (секунд/10) обновлений данных", (arg, player) -> {
        	if(player.admin()) {
        		if(arg.length == 0) {
        			player.sendMessage("Интервал обновлений: " + dataCollect.getSleepTime() + " секунд/10");
    				return;
        		}
        		if(arg.length == 1) {
        			long count = 0;
        			try {
        				count = Long.parseLong(arg[0]);
					} catch (Exception e) {
	        			player.sendMessage("[red]Вводить можно только числа!");
					}
        			count *= 1_00;
        			
        			if(count <= 0) {
	        			player.sendMessage("[red]Интервал не может быть меньше 1!");
        			}
        			dataCollect.setSleepTime(count);
        			player.sendMessage("Установлен интервал: " + count + " ms");
    				return;
        		}
        	} else {
        		admins = new Administration();
				player.sendMessage("[red]Команда только для администраторов");
        	}
        });
        
        handler.<Player>register("event", "[id] [on/off/faston]", "Включить/выключить событие", (arg, player) -> {
        	if(player.admin()) {
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
        			player.sendMessage(msg.toString());
    				return;
        		}
        		if(arg.length == 1) {
        			for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
        				ServerEvent event = ServerEventsManager.getServerEvent(i);
        				if(arg[0].equals(event.getCommandName())) {
        					player.sendMessage("Событие [" + event.getColor() + "]" + event.getName() + " [white] имеет значение: " + event.isRunning());
            				return;
        				}
					}
        			player.sendMessage("[red]Событие не найдено, [gold]/event [red] для списка событий");
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
        				player.sendMessage("Неверный аргумент, используйте [gold]on/off[]");
        				return;
            		}
            		
            		for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
        				ServerEvent event = ServerEventsManager.getServerEvent(i);
        				if(arg[0].equals(event.getCommandName())) {
        					boolean isRunning = event.isRunning();
        					if(isRunning && isOn) {
            					player.sendMessage("[red]Событие уже запущено");
                				return;
        					}
        					if(!isRunning && !isOn) {
            					player.sendMessage("[red]Событие итак не запущено");
                				return;
        					}
        					
        					if(isOn) {
        						if(isFast) {
                					eventsManager.fastRunEvent(event.getCommandName());
                					player.sendMessage("[white]Событие резко запущено! [gold]/sync");
        						} else {
                					eventsManager.runEvent(event.getCommandName());
                					player.sendMessage("[green]Событие запущено!");
        						}
        					} else {
            					eventsManager.stopEvent(event.getCommandName());
            					player.sendMessage("[red]Событие остановлено!");
        					}
        					
            				return;
        				}
					}
            		
//        			for (int i = 0; i < ServerEventsManager.EVENTS_ID.length; i++) {
//        				if(arg[0].equals(ServerEventsManager.EVENTS_ID[i])) {
//        					eventsManager.isEventsOn[i] = isOn;
//        					
//        					if(isOn) {
//        						eventsManager.startEventsLoop();
//            					player.sendMessage("Событие [gold]" + ServerEventsManager.EVENTS_ID[i] + " [green]запущено!");
//            					if(isFast) {
//            						eventsManager.fastStart();
//            					}
//        					}else {
//            					player.sendMessage("Событие [gold]" + ServerEventsManager.EVENTS_ID[i] + " [red]выключено!");
//        					}
//        					
//            				return;
//        				}
//					}
        			player.sendMessage("[red]Событие не найдено, [gold]/event [red] для списка событий");
    				return;
        		}
        	} else {
        		admins = new Administration();
				player.sendMessage("[red]Команда только для администраторов");
        	}
        });

        handler.<Player>register("team", "[player] [team]", "Установить команду для игрока", (arg, player) -> {
        	if(player.admin()) {
        		if(arg.length < 1) {
        			StringBuilder teams = new StringBuilder();
    				for (int i = 0; i < Team.baseTeams.length; i++) {
    					teams.append(Team.baseTeams[i].name);
    					teams.append(", ");
					}
    				for (int i = 0; i < Team.all.length; i++) {
    					teams.append(Team.all[i].name);
    					if(i != Team.all.length - 1) teams.append(", ");
					}
    				player.sendMessage("Команды:\n" + teams.toString());
        		}
        		if(arg.length == 1) {
                    Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[0])));
                    if(targetPlayer == null) {
    					player.sendMessage("[red]Игрок не найден");
                    	return;
                    }
    				player.sendMessage("Игрок состоить в команде: " +  targetPlayer.team().name);
    				return;
        		}
        		if(arg.length == 2) {
                    Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[0])));
                    if(targetPlayer == null) {
    					player.sendMessage("[red]Игрок не найден");
                    	return;
                    }
    				player.sendMessage("Игрок состоить в команде: " +  targetPlayer.team().name);
    				
    				Team team = null;
    				String targetTeam = arg[1].toLowerCase();
    				for (int i = 0; i < Team.baseTeams.length; i++) {
						if(Team.baseTeams[i].name.equals(targetTeam.toLowerCase())) {
							team = Team.baseTeams[i];
						}
					}
    				for (int i = 0; i < Team.all.length; i++) {
						if(Team.all[i].name.equals(targetTeam.toLowerCase())) {
							team = Team.all[i];
						}
					}
    				if(team == null) {
    					player.sendMessage("[red]Команда не найдена");
    				} else {
    					targetPlayer.team(team);
    					if(team.name.equals(Team.crux.name)) {
    						Log.info("crux");
    						targetPlayer.unit().healTime(.01f);
    						targetPlayer.unit().healthMultiplier(100);
    						targetPlayer.unit().maxHealth(1000f);
    						targetPlayer.unit().apply(StatusEffects.invincible, Float.MAX_VALUE);
    					}
    					if(team.name.equals(admin.name)) {
    						targetPlayer.unit().healTime(.01f);
    						targetPlayer.unit().healthMultiplier(100);
    						targetPlayer.unit().maxHealth(1000f);
    						targetPlayer.unit().hitSize(0);
    						targetPlayer.unit().apply(StatusEffects.invincible, Float.MAX_VALUE);
    						
    				    	admin.rules().infiniteResources = true;
    				    	admin.rules().cheat = true;
    				    	admin.rules().infiniteAmmo = true;
    				    	admin.rules().blockDamageMultiplier = Float.MAX_VALUE;
    				    	admin.rules().blockHealthMultiplier = Float.MAX_VALUE;
    				    	admin.rules().buildSpeedMultiplier = 100;
    				    	admin.rules().unitDamageMultiplier = Float.MAX_VALUE;
    					}
    					player.sendMessage("Игрок " + targetPlayer.name() + " отправлен в команду [#" + team.color + "]" + team.name);
    					targetPlayer.sendMessage("Вы отправлены в команду [#" + team.color + "]" + team.name);
    				}
    				
    				return;
        		}
        	} else {
        		admins = new Administration();
				player.sendMessage("[red]Команда только для администраторов");
        	}
        });

        handler.<Player>register("config", "[name] [set/add] [value...]", "Конфикурация сервера", (arg, player) -> {
        	if(player.admin()) {
        		if(arg.length == 0){
        			player.sendMessage("All config values:");
        			for(Config c : Config.all){
        				player.sendMessage("[gold]" + c.name + "[lightgray](" + c.description + ")[white]:\n> " + c.get() + "\n");
        			}
        			return;
        		}

        		Config c = Config.all.find(conf -> conf.name.equalsIgnoreCase(arg[0]));

        		if(c != null){
        			if(arg.length == 1) {
        				player.sendMessage(c.name + " is currently " + c.get());
        			}else if(arg.length > 2) {
        				if(arg[2].equals("default")){
        					c.set(c.defaultValue);
        				}else if(c.isBool()){
        					c.set(arg[2].equals("on") || arg[2].equals("true"));
        				}else if(c.isNum()){
        					try{
        						c.set(Integer.parseInt(arg[2]));
        					}catch(NumberFormatException e){
        						player.sendMessage("[red]Not a valid number: " + arg[2]);
        						return;
        					}
        				}else if(c.isString()) {
        					if(arg.length > 2) {
        						if(arg[1].equals("add")) {
                					c.set(c.get().toString() + arg[2].replace("\\n", "\n"));
        						} else if(arg[1].equals("set")) {
                					c.set(arg[2].replace("\\n", "\n"));
        						} else {
        	        				player.sendMessage("[red]Only [gold]add/set");
        	        				return;
        						}
        					} else {
    	        				player.sendMessage("[red]Add [gold]add/set [red]attribute");
        					}
        				}

        				player.sendMessage("[gold]" + c.name + "[gray] set to [white]" + c.get());
        				Core.settings.forceSave();
        			} else {
        				player.sendMessage("[red]Need more attributes");
        			}
        		}else{
        			player.sendMessage("[red]Unknown config: '" + arg[0] + "'. Run the command with no arguments to get a list of valid configs.");
        		}
        	} else {
        		admins = new Administration();
        		player.sendMessage("[red]Команда только для администраторов");
        	}
        });
        
        handler.<Player>register("sandbox", "[on/off] [team]", "Бесконечные ресурсы", (arg, player) -> {
        	if(player.admin()) {
        		if(arg.length == 0) {
            		player.sendMessage("[gold]infiniteResources: [gray]" + Vars.state.rules.infiniteResources);
        			
        		} else {
        			Team team = null;
        			if(arg.length == 2) {
            			String targetTeam = arg[1].toLowerCase();
            			for (int i = 0; i < Team.baseTeams.length; i++) {
            				if(Team.baseTeams[i].name.equals(targetTeam.toLowerCase())) {
            					team = Team.baseTeams[i];
            				}
            			}
            			for (int i = 0; i < Team.all.length; i++) {
            				if(Team.all[i].name.equals(targetTeam.toLowerCase())) {
            					team = Team.all[i];
            				}
            			}
        			}
        			
        			if(arg[0].equals("on")) {
                		if(team == null) {
                    		Vars.state.rules.infiniteResources = true;
                    		player.sendMessage("[green]Включено!");
                		} else {
                			team.rules().infiniteResources = true;
                    		player.sendMessage("[green]Включено для команды [#" + team.color + "]" + team.name);
                		}
        			}else if(arg[0].equals("off")) {
                		if(team == null) {
                    		Vars.state.rules.infiniteResources = false;
                    		player.sendMessage("[red]Выключено!");
                		} else {
                			team.rules().infiniteResources = false;
                    		player.sendMessage("[red]Выключено для команды [#" + team.color + "]" + team.name);
                		}
        			} else {
                		player.sendMessage("[red]Только on/off");
        			}
        		}
        	} else {
        		admins = new Administration();
        		player.sendMessage("[red]Команда только для администраторов");
        	}
        });
        
        handler.<Player>register("unit", "[type] [inPlayer]", "Создает юнита, list для списка", (arg, player) -> {
        	if(player.admin()) {
        		String unitType = UnitTypes.gamma.name;
    			Field[] fields = UnitTypes.class.getFields();
        		
        		if(arg.length > 0) {
        			if(arg[0].equals("list")) {
    					StringBuilder unitTypes = new StringBuilder("Типы юнитов");
            			for (int i = 0; i < fields.length; i++) {
            				String name = fields[i].getName();
        					if(name.equals(UnitTypes.block.name)) continue;
        					
            				unitTypes.append(fields[i].getName());
            				if(i+1 != fields.length) {
                				unitTypes.append(", ");
            				}
    					}
            			player.sendMessage(unitTypes.toString());
            			return;
        			}
        			unitType = arg[0];
        		}

        		try {
        			for (int i = 0; i < fields.length; i++) {
        				if(fields[i].getName().equals(unitType)) {
        					UnitType ut = (UnitType) fields[i].get(UnitTypes.class);
        					if(ut == null) continue;
        					if(ut.name.equals(UnitTypes.block.name)) {
        						continue;
        					}
        					Unit u = ut.spawn(player.team(), player.x, player.y);
        					ignoreUnitId = u.id;
        					if(arg.length > 1) {
        						if(arg[1].equals("true") || arg[1].equals("y") || arg[1].equals("t") || arg[1].equals("yes")) {
        							player.unit(u);
        						}
        					}
        					player.sendMessage("Готово!"); 

        					if(!net.client()){
        						u.add();
        					}
        					return;
        				}
        			}
            		player.sendMessage("[red]Юнит не найден [gold]/unit list");
        		} catch (IllegalArgumentException | IllegalAccessException e) {
        			e.printStackTrace();
            		player.sendMessage(e.getLocalizedMessage());
        		}
        	        
        	} else {
        		admins = new Administration();
        		player.sendMessage("[red]Команда только для администраторов");
        	}
        });
        
        handler.<Player>register("unban", "<ip/ID/all>", "Completely unban a person by IP or ID.", (arg, player) -> {
        	if(player.admin()) {
        		if(arg[0].equals("all")) {
        			int count = 0;
        			while (true) {
						if(netServer.admins.bannedIPs.size > 0) {
	        				netServer.admins.unbanPlayerIP(netServer.admins.bannedIPs.get(0));
	        				count++;
						} else {
							break;
						}
					}
            		player.sendMessage("[gold]Снято банов: [lightgray]" + count);
        		} else {
            		if(netServer.admins.unbanPlayerIP(arg[0]) || netServer.admins.unbanPlayerID(arg[0])){
            			player.sendMessage("[gold]Unbanned player: [white]" + arg[0]);
            		}else{
            			player.sendMessage("[red]That IP/ID is not banned!");
            		}
        		}
        	} else {
        		player.sendMessage("[red]Команда только для администраторов");
        	}
        });
        
        handler.<Player>register("bans", "List all banned IPs and IDs.", (arg, player) -> {
        	if(player.admin()) {
        		Seq<PlayerInfo> bans = netServer.admins.getBanned();

        		if(bans.size == 0){
        			player.sendMessage("No ID-banned players have been found.");
        		}else{
        			player.sendMessage("Banned players [ID]:");
        			for(PlayerInfo info : bans){
        				player.sendMessage(" " + info.id + " / Last known name: [gold]" + info.plainLastName());
        			}
        		}

        		Seq<String> ipbans = netServer.admins.getBannedIPs();

        		if(ipbans.size == 0){
        			player.sendMessage("No IP-banned players have been found.");
        		}else{
        			player.sendMessage("Banned players [IP]:");
        			for(String string : ipbans){
        				PlayerInfo info = netServer.admins.findByIP(string);
        				if(info != null){
        					player.sendMessage(" " + string + "   / Last known name: [gold]" + info.plainLastName() +"[] / ID: " + info.id);
        				}else{
        					player.sendMessage(" " + string + "   (No known name or info)");
        				}
        			}
        		}
        	} else {
        		player.sendMessage("[red]Команда только для администраторов");
        	}
        });
        

        handler.<Player>register("reloadmaps", "Перезагрузить карты", (arg, player) -> {
        	if(player.admin()) {
        		int beforeMaps = maps.all().size;
        		maps.reload();
        		if(maps.all().size > beforeMaps) {
        			player.sendMessage("[gold]" + (maps.all().size - beforeMaps) + " новых карт было найдено");
        		}else if(maps.all().size < beforeMaps) {
        			player.sendMessage("[gold]" + (beforeMaps - maps.all().size) + " карт было удалено");
        		}else{
        			player.sendMessage("[gold]Карты перезагружены");
        		}
        	} else {
        		player.sendMessage("[red]Команда только для администраторов");
        	}
        });

        handler.<Player>register("js", "<script...>", "Запустить JS", (arg, player) -> {
        	if(player.admin()) {
        		player.sendMessage("[gold]" + mods.getScripts().runConsole(arg[0]));
        	} else {
        		player.sendMessage("[red]Команда только для администраторов");
        	}
        });

        handler.<Player>register("link", "<link> [player]", "Отправить ссылку всем/игроку", (arg, player) -> {
        	if(player.admin()) {
        		if(arg.length == 1) {
            		Call.openURI(arg[0]);
        		} else if(arg.length == 2) {
                    Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[1])));
            		if(targetPlayer != null) {
            			Call.openURI(targetPlayer.con, arg[0]);
                		player.sendMessage("[gold]Готово!");
            		} else {
                		player.sendMessage("[red]Игрок не найден");
            		}
        		}
        	} else {
        		player.sendMessage("[red]Команда только для администраторов");
        	}
        });

        handler.<Player>register("setdiscord", "[link]", "\ue80d Сервера", (arg, player) -> {
        	if(player.admin()) {
        		if(arg.length == 1) {
        			discordLink = arg[0];
            		player.sendMessage("[gold]\ue80d Готово!");
        		}
        	} else {
        		player.sendMessage("[red]Команда только для администраторов");
        	}
        });
    }
    
    String discordLink = "";
    
    private String getColoredLocalizedItemName(Item item) {
		return "[#" + item.color.toString() + "]" + item.localizedName;
	}

	private void fillitemsCheck() {

	}
    
    class GrieferVoteSession {
    	
    	Player target;
        float voteDuration = 1f * 60;
        ObjectSet<String> voted = new ObjectSet<>();
        private GrieferVoteSession[] gmap;
        Timer.Task task;
        int votes;

        public GrieferVoteSession(GrieferVoteSession[] gmap, Player target){
        	this.target = target;
            Log.info("SMap: " + gmap[0]);
            this.gmap = gmap;
            this.task = Timer.schedule(() -> {
                if(!checkPass()){
                    Call.sendMessage("[lightgray]Голосование закончилось. Недостаточно голосов");
                    gmap[0] = null;
                    task.cancel();
                }
            }, voteDuration);
        }

        void vote(Player player, int d){
            votes += d;
            voted.addAll(player.uuid(), admins.getInfo(player.uuid()).lastIP);
            Call.sendMessage(Strings.format("[" + colorToHex(player.color) + "]@[lightgray] проголосовал " + (d > 0 ? "[green]за" : "[red]против") + "[white] наказания для @ [accent] (@/@)\n[lightgray]Напишите[orange] /gvote <y/n>[white], чтобы проголосовать [green]за[white]/[red]против",
                player.name, ("[" + colorToHex(target.color) + "]" + target.name()), votes, votesRequired()));
            checkPass();
        }

        boolean checkPass(){
            if(votes >= votesRequired()){
            	
            	target.team(Team.crux);
            	target.unit().apply(StatusEffects.invincible, Float.MAX_VALUE);
            	target.sendMessage("[gold]Вы были наказаны, теперь вы в команде врага. Вы можете управлять вражескими единицами, но после этого вы не сможете вернуться в прежнюю форму");

            	Call.sendMessage(Strings.format("[gold]Голосование закончилось. [scarlet] @[orange] наказан", target.name));
            	gmap[0] = null;
                task.cancel();
                return true;
            }
            return false;
        }
    }

    class SkipmapVoteSession {
    	
        float voteDuration = 3 * 60;
        ObjectSet<String> voted = new ObjectSet<>();
        SkipmapVoteSession[] map;
        Timer.Task task;
        int votes;

        public SkipmapVoteSession(SkipmapVoteSession[] map){
            this.map = map;
            this.task = Timer.schedule(() -> {
                if(!checkPass()){
                    Call.sendMessage("[lightgray]Голосование закончилось. Недостаточно голосов, чтобы пропустить карту");
                    map[0] = null;
                    task.cancel();
                }
            }, voteDuration);
        }

        void vote(Player player, int d){
            votes += d;
            voted.addAll(player.uuid(), admins.getInfo(player.uuid()).lastIP);
            Call.sendMessage(Strings.format("[" + colorToHex(player.color) + "]@[lightgray] проголосовал " + (d > 0 ? "[green]за" : "[red]против") + "[] пропуска карты[accent] (@/@)\n[lightgray]Напишите[orange] /smvote <y/n>[], чтобы проголосовать [green]за[]/[red]против",
                player.name, votes, votesRequiredSkipmap()));
            checkPass();
        }

        boolean checkPass(){
            if(votes >= votesRequiredSkipmap()){
            	Call.sendMessage("[gold]Голосование закончилось. Карта успешно пропущена!");
            	Events.fire(new GameOverEvent(Team.derelict));
            	map[0] = null;
            	task.cancel();
                return true;
            }
            return false;
        }
    }
    
    private String colorToHex(Color color) {
    	return String.format("#%02x%02x%02x", (int)(color.r*255), (int)(color.g*255), (int)(color.b*255));
	}

    public int votesRequired(){
    	if(Groups.player.size() == 2) return 1;
        return 2 + (Groups.player.size() > 4 ? 1 : 0);
    }
    
    public int votesRequiredSkipmap(){
        return (int) Math.ceil(Groups.player.size()*2d/3d);
    }
    
    
}
