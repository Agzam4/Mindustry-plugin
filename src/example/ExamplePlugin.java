package example;

import javax.swing.plaf.nimbus.State;

import arc.*;
import arc.graphics.Color;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.*;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonValue.ValueType;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.game.Team;
import mindustry.game.EventType.*;
import mindustry.game.Gamemode;
import mindustry.gen.*;
import mindustry.io.JsonIO;
import mindustry.mod.*;
import mindustry.net.Administration;
import mindustry.net.Administration.*;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.maps.Map;
import mindustry.maps.*;

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
//    private 
    
    //called when game initializes
    @Override
    public void init() {
    	maps = new Maps();
    	maps.load();
    	
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
        	if(chatFilter) {
            	text = text.replace("нуб", "про");
            	text = text.replace("Нуб", "Про");
            	text = text.replace("НУБ", "ПРО");
        	}
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
    }

    @Override
    public void registerClientCommands(CommandHandler handler){

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
    		for (int i = 0; i < lcounter.length; i++) {
    			if(lcounter[i] > 0) {
        			worldInfo.append("\n[white]");
        			worldInfo.append(liquidsemoji[i]);
        			worldInfo.append("[lightgray]: ");
        			worldInfo.append(counter[i]);
    			}
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

        /**
         *  SKIP MAP COMMANDS
         */
        
        VoteSession[] currentlyMapSkipping = {null};
        
        handler.<Player>register("skipmap", "Начать голосование за пропуск карты", (arg, player) -> {
            VoteSession session = new VoteSession(currentlyMapSkipping);
            session.vote(player, 1);
            currentlyMapSkipping[0] = session;
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
        			+ "[green] Agzam's plugin v1.3\n"
        			+  "[gray]========================================================\n"
        			+ "[white] Added [royal]skip map[white] commands\n"
        			+ "[white] Added protection from [violet]thorium reactors[white]\n"
        			+ "[white] Added map list command\n"
        			+ "[white] Added fill items by type to core command\n"
        			+ "[white] Added Map recourses statistic command\n"
        			+ "[white] and more other\n"
        			+  "[gray] Download: github.com/Agzam4/Mindustry-plugin");
        			
        });
        
        handler.<Player>register("fillitems", "[item] [count]", "Заполните ядро предметами [red]Только для администраторов", (arg, player) -> {
        	if(player.admin()) {
        		try {
            		Item item = Items.copper;
            		
            		String itemname = arg[0].toLowerCase();
            		if(itemname.equals("blastCompound") 	|| itemname.equals("взрывчатка")	|| itemname.equals("\uf82a")) 	item = Items.blastCompound;
            		else if(itemname.equals("coal") 		|| itemname.equals("уголь")			|| itemname.equals("\uf833")) 	item = Items.coal;
            		else if(itemname.equals("copper") 		|| itemname.equals("медь")			|| itemname.equals("\uf838")) 	item = Items.copper;
            		else if(itemname.equals("graphite") 	|| itemname.equals("графит")		|| itemname.equals("\uf835")) 	item = Items.graphite;
            		else if(itemname.equals("lead") 		|| itemname.equals("свинец")		|| itemname.equals("\uf837")) 	item = Items.lead;
            		else if(itemname.equals("metaglass") 	|| itemname.equals("стекло")		|| itemname.equals("\uf836")) 	item = Items.metaglass;
            		else if(itemname.equals("phaseFabric") 	|| itemname.equals("фаза")			|| itemname.equals("\uf82d")) 	item = Items.phaseFabric;
            		else if(itemname.equals("plastanium") 	|| itemname.equals("пластан")		|| itemname.equals("\uf82e")) 	item = Items.plastanium;
            		else if(itemname.equals("pyratite") 	|| itemname.equals("пиротит")		|| itemname.equals("\uf829")) 	item = Items.pyratite;
            		else if(itemname.equals("sand") 		|| itemname.equals("песок")			|| itemname.equals("\uf834")) 	item = Items.sand;
            		else if(itemname.equals("scrap") 		|| itemname.equals("железо")		|| itemname.equals("\uf830")) 	item = Items.scrap;
            		else if(itemname.equals("silicon") 		|| itemname.equals("кремень")		|| itemname.equals("\uf82f")) 	item = Items.silicon;
            		else if(itemname.equals("sporePod") 	|| itemname.equals("споры")			|| itemname.equals("\uf82b")) 	item = Items.sporePod;
            		else if(itemname.equals("surgeAlloy") 	|| itemname.equals("кинетик")		|| itemname.equals("\uf82c")) 	item = Items.surgeAlloy;
            		else if(itemname.equals("thorium") 		|| itemname.equals("торий")			|| itemname.equals("\uf831")) 	item = Items.thorium;
            		else if(itemname.equals("titanium") 	|| itemname.equals("титан")			|| itemname.equals("\uf832")) 	item = Items.titanium;
            		else {
    					player.sendMessage("Русские названия предметов: взрывчатка, уголь, медь, графит, свинец, стекло, фаза, пластан, пиротит, песок, железо, кремень, споры, кинетик, торий, титан");
            		}
            		if(arg.length == 0) {
    					player.sendMessage("Русские названия предметов: взрывчатка, уголь, медь, графит, свинец, стекло, фаза, пластан, пиротит, песок, железо, кремень, споры, кинетик, торий, титан");
            		}
            		
            		Team team = player.team();
            		
            		int count = arg.length > 1 ? Integer.parseInt(arg[1]) : 100;
            		
            		for(CoreBuild core : team.cores()){
            			core.items.add(item, count);
            		}
					player.sendMessage("Added " + "[gold]x" + count + " [orange]" + item.name);
				} catch (Exception e) {
					player.sendMessage(e.getMessage());
				}
//                  state.teams.cores(team).first().items.set(item, state.teams.cores(team).first().storageCapacity);
//              }
//              if(!state.is(State.playing)){
//              err("Not playing. Host first.");
//              return;
//          }
//
//          Team team = arg.length == 0 ? Team.sharded : Structs.find(Team.all, t -> t.name.equals(arg[0]));
//
//          if(team == null){
//              err("No team with that name found.");
//              return;
//          }
//
//          if(state.teams.cores(team).isEmpty()){
//              err("That team has no cores.");
//              return;
//          }
//
//          for(Item item : content.items()){
//              state.teams.cores(team).first().items.set(item, state.teams.cores(team).first().storageCapacity);
//          }
//
//          info("Core filled.");
        	} else {
        		admins = new Administration();
				player.sendMessage("[red] Команда только для администраторов");
        	}
        });
        
        handler.<Player>register("admin", "[add/remove] [name]", "Добавить/удалить админа [red]Только для администраторов", (arg, player) -> {
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
				player.sendMessage("[red] Команда только для администраторов");
        	}
        });
    	
        handler.<Player>register("chatfilter", "[on/off]", "Включить/выключить фильтр чата [red] Только для администраторов", (arg, player) -> {
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
    				player.sendMessage("[green]Чат фильтр выключен");
        		}else {
    				player.sendMessage("Неверный аргумент, используйте [gold]on/off");
    				return;
        		}
        	} else {
        		admins = new Administration();
				player.sendMessage("[red] Команда только для администраторов");
        	}
        });

        //register a whisper command which can be used to send other players messages
//        handler.<Player>register("whisper", "<player> <text...>", "Whisper text to another player.", (args, player) -> {
//            //find player by name
//            Player other = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
//
//            //give error message with scarlet-colored text if player isn't found
//            if(other == null){
//                player.sendMessage("[scarlet]No player by that name found!");
//                return;
//            }
//
//            //send the other player a message, using [lightgray] for gray text color and [] to reset color
//            other.sendMessage("[lightgray](whisper) " + player.name + ":[] " + args[1]);
//        });
    }
    
    class VoteSession {
    	
        float voteDuration = 3 * 60;
        
        ObjectSet<String> voted = new ObjectSet<>();
        VoteSession[] map;
        Timer.Task task;
        int votes;

        public VoteSession(VoteSession[] map){
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
                player.name, votes, votesRequired()));

            checkPass();
        }

        boolean checkPass(){
            if(votes >= votesRequired()){
                Call.sendMessage("[gold]Голосование закончилось. Карта успешно пропущена!");

                Events.fire(new GameOverEvent(Team.derelict));
//                Groups.player.each(p -> p.uuid().equals(target.uuid()), p -> p.kick(KickReason.vote, kickDuration * 1000));
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
        return (int) Math.ceil(Groups.player.size()*2d/3d);
    }
}
