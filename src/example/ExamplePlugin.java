package example;

import javax.swing.plaf.nimbus.State;

import arc.*;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState;
import mindustry.game.Saves;
import mindustry.game.Team;
import mindustry.game.EventType.*;
import mindustry.game.Gamemode;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Administration;
import mindustry.net.Administration.*;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.maps.Map;
import mindustry.maps.*;
import mindustry.maps.Maps.*;

public class ExamplePlugin extends Plugin{

    private @Nullable Map nextMapOverride;
    private boolean inGameOverWait;
    private Gamemode lastMode;
    private GameState e;
    private Maps maps;
    private Administration admins = new Administration();
    
    private float lastThoriumReactorAlertX, lastThoriumReactorAlertY;
    
//    private 
    
    //called when game initializes
    @Override
    public void init() {
    	maps = new Maps();
    	maps.load();
    	
//    	Events.on(GameOverEvent.class, event -> {
//    		if(inGameOverWait) return;
//     		Map map = nextMapOverride != null ? nextMapOverride : maps.getNextMap();
//    		nextMapOverride = null;
//    	});
    	
        //listen for a block selection event
        Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildPlan() != null && event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer()){
                //player is the unit controller
                Player player = event.builder.getPlayer();

                Team team = player.team();

                float thoriumReactorX = event.tile.getX();
                float thoriumReactorY = event.tile.getY();
                
                boolean needAlert = lastThoriumReactorAlertX != thoriumReactorX || lastThoriumReactorAlertY != thoriumReactorY;

                if(needAlert) {
                	lastThoriumReactorAlertX = thoriumReactorX;
                	lastThoriumReactorAlertY = thoriumReactorY;
                }

                for (CoreBuild core : team.cores()) {
                	int hypot = (int) (Math.hypot(thoriumReactorX - core.getX(), thoriumReactorY - core.getY())/10);


                	if(hypot <= 20) {
                		if(needAlert) {
                			Call.sendMessage("[red]ALERT!!![] " + player.name + " has begun building a reactor near core at " + event.tile.x + ", " + event.tile.y + " (" + hypot + " blocks)");
                		}
                		event.builder.clearBuilding();
                		event.builder.kill();
                		return;
                	}
                }

        		if(needAlert) {
        			Call.sendMessage("[gold]WARNNING![] " + player.name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y);
        		}
                //send a message to everyone saying that this player has begun building a reactor
            }
        });

        //add a chat filter that changes the contents of all messages
        //in this case, all instances of "heck" are censored
//        Vars.netServer.admins.addChatFilter((player, text) -> text.replace("heck", "h*ck"));

        //add an action filter for preventing players from doing certain things
        Vars.netServer.admins.addActionFilter(action -> {
            //random example: prevent blast compound depositing
            if(action.type == ActionType.depositItem && action.item == Items.blastCompound && action.tile.block() instanceof CoreBlock){
                action.player.sendMessage("Example action filter: Prevents players from depositing blast compound into the core.");
                return false;
            }
            return true;
        });
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("reactors", "List all thorium reactors in the map.", args -> {
            for(int x = 0; x < Vars.world.width(); x++){
                for(int y = 0; y < Vars.world.height(); y++){
                    //loop through and log all found reactors
                    //make sure to only log reactor centers
                    if(Vars.world.tile(x, y).block() == Blocks.thoriumReactor && Vars.world.tile(x, y).isCenter()){
                        Log.info("Reactor at @, @", x, y);
                    }
                }
            }
        });
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){

        handler.<Player>register("maps", "[all/custom/default]", "Display available maps. Displays all maps by default", (arg, player) -> {
        	
        	boolean custom  = arg.length == 0 || (arg[0].equals("custom")  || arg[0].equals("all"));
            boolean def     = arg.length == 0 || (arg[0].equals("default") || arg[0].equals("all"));
            
            if(!maps.all().isEmpty()){
                Seq<Map> all = new Seq<>();

                if(custom) all.addAll(maps.customMaps());
                if(def) all.addAll(maps.defaultMaps());

                if(all.isEmpty()){
					player.sendMessage("No custom maps loaded. To display built-in maps, use the [gold]all argument.");
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
            	player.sendMessage("No maps found.");
            }
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
        
        handler.<Player>register("skipmap", "Force a game over.", (arg, player) -> {
            VoteSession session = new VoteSession(currentlyMapSkipping);
            session.vote(player, 1);
            currentlyMapSkipping[0] = session;
        });
    	
        handler.<Player>register("smvote", "<y/n>", "Vote to skip the map", (arg, player) -> {
            if(currentlyMapSkipping[0] == null){
                player.sendMessage("[scarlet]Nobody is being voted on.");
            }else{
                if(player.isLocal()){
                    player.sendMessage("[scarlet]Local players can't vote. Kick the player yourself instead.");
                    return;
                }
                
                if((currentlyMapSkipping[0].voted.contains(player.uuid()) || currentlyMapSkipping[0].voted.contains(admins.getInfo(player.uuid()).lastIP))){
                    player.sendMessage("[scarlet]You've already voted. Sit down.");
                    return;
                }
                
                String voteSign = arg[0].toLowerCase();
                
                int sign = 0;
                if(voteSign.equals("y")) sign = +1;
                if(voteSign.equals("n")) sign = -1;

                if(sign == 0){
                    player.sendMessage("[scarlet]Vote either 'y' (yes) or 'n' (no).");
                    return;
                }

                currentlyMapSkipping[0].vote(player, sign);
            }
        });
        
        handler.<Player>register("fillitems", "[item] [count]", "Fill the core with items. Admins only", (arg, player) -> {
        	if(admins.getInfo(player.uuid()).admin) {
        		try {
            		Item item = Items.copper;
            		
            		String itemname = arg[0].toLowerCase();
            		if(itemname.equals("blastCompound") || itemname.equals("взрывчатка")) item = Items.blastCompound;
            		else if(itemname.equals("coal") || itemname.equals("уголь")) item = Items.coal;
            		else if(itemname.equals("copper") || itemname.equals("медь")) item = Items.copper;
            		else if(itemname.equals("graphite") || itemname.equals("графит")) item = Items.graphite;
            		else if(itemname.equals("lead") || itemname.equals("свинец")) item = Items.lead;
            		else if(itemname.equals("metaglass") || itemname.equals("стекло")) item = Items.metaglass;
            		else if(itemname.equals("phaseFabric") || itemname.equals("фаза")) item = Items.phaseFabric;
            		else if(itemname.equals("plastanium") || itemname.equals("пластан")) item = Items.plastanium;
            		else if(itemname.equals("pyratite") || itemname.equals("пиротит")) item = Items.pyratite;
            		else if(itemname.equals("sand") || itemname.equals("песок")) item = Items.sand;
            		else if(itemname.equals("scrap") || itemname.equals("железо")) item = Items.scrap;
            		else if(itemname.equals("silicon") || itemname.equals("силикон")) item = Items.silicon;
            		else if(itemname.equals("sporePod") || itemname.equals("споры")) item = Items.sporePod;
            		else if(itemname.equals("surgeAlloy") || itemname.equals("кинетик")) item = Items.surgeAlloy;
            		else if(itemname.equals("thorium") || itemname.equals("торий")) item = Items.thorium;
            		else if(itemname.equals("titanium") || itemname.equals("титан")) item = Items.titanium;
            		else {
    					player.sendMessage("Русские названия предметов: взрывчатка, уголь, медь, графит, свинец, стекло, фаза, пластан, пиротит, песок, железо, силикон, споры, кинетик, споры, кинетик, торий, титан");
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
				player.sendMessage("[red] Admins only command");
        	}
        });
        
        handler.<Player>register("plugininfo", "info about pluging", (arg, player) -> {
        	player.sendMessage(""
        			+ "[green] Agzam's plugin v1.2\n"
        			+  "[gray]========================================================\n"
        			+ "[white] Added [royal]skip map[white] commands\n"
        			+ "[white] Added protection from [violet]thorium reactors[white]\n"
        			+ "[white] Added map list command\n"
        			+  "[gray] Download: github.com/Agzam4/Mindustry-plugin");
        			
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
    	
        float voteDuration = 0.5f * 60;
        
        ObjectSet<String> voted = new ObjectSet<>();
        VoteSession[] map;
        Timer.Task task;
        int votes;

        public VoteSession(VoteSession[] map){
            this.map = map;
            this.task = Timer.schedule(() -> {
                if(!checkPass()){
                    Call.sendMessage("[lightgray]Vote failed. Not enough votes to skip map");
                    map[0] = null;
                    task.cancel();
                }
            }, voteDuration);
        }

        void vote(Player player, int d){
            votes += d;
            voted.addAll(player.uuid(), admins.getInfo(player.uuid()).lastIP);

            Call.sendMessage(Strings.format("[lightgray]@[lightgray] has voted on map skipping [accent] (@/@)\n[lightgray]Type[orange] /smvote <y/n>[] to agree.",
                player.name, votes, votesRequired()));

            checkPass();
        }

        boolean checkPass(){
            if(votes >= votesRequired()){
                Call.sendMessage("[gold]The map was skipped!");

                Events.fire(new GameOverEvent(Team.derelict));
//                Groups.player.each(p -> p.uuid().equals(target.uuid()), p -> p.kick(KickReason.vote, kickDuration * 1000));
                map[0] = null;
                task.cancel();
                return true;
            }
            return false;
        }
    }
    

    public int votesRequired(){
        return (int) Math.ceil(Groups.player.size()*2d/3d);
    }
}
