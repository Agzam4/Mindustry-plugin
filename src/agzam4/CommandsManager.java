package agzam4;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import agzam4.achievements.AchievementsManager;
import agzam4.achievements.AchievementsManager.Achievement;
import agzam4.bot.Bots;
import agzam4.bot.Bots.NotifyTag;
import agzam4.bot.TChat;
import agzam4.bot.TSender;
import agzam4.bot.TUser;
import agzam4.bot.TUser.MessageData;
import agzam4.bot.TelegramBot;
import agzam4.database.Database;
import agzam4.database.Database.PlayerEntity;
import agzam4.events.*;
import agzam4.net.NetMenu;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.*;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Bresenham2;
import arc.math.geom.Point2;
import arc.struct.*;
import arc.util.CommandHandler;
import arc.util.CommandHandler.CommandRunner;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Structs;
import arc.util.Threads;
import arc.util.Time;
import arc.util.Timekeeper;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.net.Administration.*;
import mindustry.net.Packets.KickReason;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.storage.CoreBlock;

import static agzam4.Emoji.*;

public class CommandsManager {

	private CommandsManager() {}
	
	private static ObjectMap<String, Integer> lastkickTime = new ObjectMap<>();
	
	public static SkipmapVoteSession[] currentlyMapSkipping = {null};
    public static @Nullable VoteSession currentlyKicking = null;
    
    public static Seq<String> extraStarsUIDD;
	
	public static boolean chatFilter;		
	public static String discordLink = "";	// Link on discord "/setdiscord" to change
	public static int doorsCup = 100; 		// Max doors limit "/doorscup" to change

	private static ArrayList<Integer> doorsCoordinates;
	private static Player lastThoriumReactorPlayer;
	
//	public static final Seq<Sound> sounds = new Seq<Sound>();

	public static ObjectMap<String, PlayerEntity> joined = new ObjectMap<>(); // UUID, PlayerEntity

	public static void init() {
		discordLink = Core.settings.getString(AgzamPlugin.name() + "-discord-link", null);
		doorsCup = Core.settings.getInt(AgzamPlugin.name() + "-doors-cup", Integer.MAX_VALUE);
		discordLink = Core.settings.getString(AgzamPlugin.name() + "-discord-link", null);
		chatFilter = Core.settings.getBool(AgzamPlugin.name() + "-chat-filter", false);
		extraStarsUIDD = new Seq<>();
		
		doorsCoordinates = new ArrayList<>();
		
		registerBotCommands();
		registerPlayersCommands();
		registerAdminCommands();
		
    	Events.run(Trigger.update, () -> {
    		Brush.brushes.each(Brush::update);
    	});

		Events.on(GameOverEvent.class, e -> {
			Brush.brushes.clear();
			lastkickTime.clear();
		});
		
		Events.on(PlayerLeave.class, e -> {
			if(e.player == null) return;
			Brush.brushes.removeAll(b -> b.owner == e.player);

    		/**
    		 * Saving PlayerEntity
    		 */
			@Nullable PlayerEntity playerEntity = joined.remove(e.player.uuid());
			if(playerEntity != null) {
				playerEntity.playtime += (int) TimeUnit.MILLISECONDS.toMinutes(Time.millis() - playerEntity.joinTime);
				Database.players.put(playerEntity);
			}
			Bots.notify(NotifyTag.playerConnection, 
					Strings.format("<b>@</b> has left <i>(@ players)</i>", TelegramBot.strip(e.player.name), joined.size)
			);
			
			PlayerData data = Players.getData(e.player.uuid());
			if(data == null || data.disconnectedMessage == null) Call.sendMessage(e.player.coloredName() + "[accent] отключился");
			else Call.sendMessage("[accent]" + data.disconnectedMessage.replaceAll("@name", e.player.coloredName() + "[accent]"));
			
		});

    	Events.on(PlayerJoin.class, e -> {
    		/**
    		 * Loading PlayerEntity
    		 */
			PlayerEntity playerEntity = Database.player(e.player);
			playerEntity.joinTime = Time.millis();
    		joined.put(e.player.uuid(), playerEntity);
    		
    		Bots.notify(NotifyTag.playerConnection,
    				Strings.format("<b>@</b>@ has joined <i>(@ players)</i>", TelegramBot.strip(e.player.name), (e.player.admin() ? " (admin)":""), joined.size),
    				Strings.format("<b>@</b>@ has joined <i>(@ players)</i> <code>@</code>", TelegramBot.strip(e.player.name), (e.player.admin() ? " (admin)":""), joined.size, e.player.uuid())
    		);
			
    		PlayerData data = Players.getData(e.player.uuid());

    		if(data != null && data.name != null)  e.player.name(data.name);
    		
    		if(data == null || data.connectMessage == null) Call.sendMessage(e.player.coloredName() + "[accent] подключился");
    		else Call.sendMessage("[accent]" + data.connectMessage.replaceAll("@name", e.player.coloredName() + "[accent]"));
    		
    		ServerEventsManager.playerJoin(e);
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
			
			if(block == Blocks.door || block == Blocks.doorLarge || block == Blocks.blastDoor) {

				Tile door = event.tile;
				if(door == null) return;
				int center = Point2.pack(door.centerX(), door.centerY());
				for (int i = 0; i < doorsCoordinates.size(); i++) {
					int pos = doorsCoordinates.get(i);
					if(center == pos) {
						doorsCoordinates.remove(i);
						break;
					}
				}
				if(!event.breaking) {
					doorsCoordinates.add(Point2.pack(event.tile.centerX(), event.tile.centerY()));
				}
				if(event.tile != null)
					updateDoors(event.tile);
			}
		});

		/**
		 * Destroy (by explosion, or enemy and etc.)
		 */
    	Events.on(BlockDestroyEvent.class, event -> {
    		if(event.tile == null) return;
    		if(event.tile.block() == null) return;
    		Block block = event.tile.block();
    		
    		if(block == Blocks.door || block == Blocks.doorLarge || block == Blocks.blastDoor) {
    			updateDoors(event.tile);
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
			
			if(!event.breaking) {
				if(block == Blocks.door || block == Blocks.doorLarge || block == Blocks.blastDoor) {
					if(doorsCoordinates.size() >= doorsCup) {
						event.tile.setNet(Blocks.air);
						Player player = builder.getPlayer();
						if(player != null) {
							builder.clearBuilding();
							if(builder.plans != null) builder.plans.clear();
							player.clearUnit();
							Log.info(player.plainName() + " exceeded the limit of doors");
							player.sendMessage("[red]Достигнут максимальный лимит дверей!");
							return;
						}
						return;
					}
				}
			}
		});
		
		Events.on(BlockBuildEndEvent.class, event -> {
			Unit builder = event.unit;
			if(builder == null) return;
			BuildPlan buildPlan = builder.buildPlan();
			if(buildPlan == null) return;
//			if(builder.isPlayer()) {
//				Player player = builder.getPlayer();
//				if(player == null) return;
//				if(player.admin()) {
//					if(player != brushOwner) return;
//					if(adminSandbox) {
//						if(brushBlock != null || brushFloor != null || brushOverlay != null) {
//							Tile tile = event.tile;
//							if(brushBlock != null) {
//								tile.setNet(brushBlock, player.team(), 0);
//							}
//							if(brushFloor != null) {
//								if(brushOverlay != null) {
//									tile.setFloorNet(brushFloor, brushOverlay);
//								} else {
//									tile.setFloorNet(brushFloor);
//								}
//							} else {
//								if(brushOverlay != null) {
//									tile.setFloorNet(tile.floor(), brushOverlay);
//								}
//							}
//						}
//					}
//				}
//			}
		});

		// "/brush" command
		Events.on(TapEvent.class, event -> {
			Player player = event.player;
			if(player == null) return;
			if(event.tile == null) return;
			if(!player.admin()) return;
			Brush brush = Brush.get(player);
			
			Vars.mods.getScripts().runConsole("var ttile = Vars.world.tile(" + event.tile.pos() + ")");
			if(brush.info && event.tile.build != null) player.sendMessage("[white]lastAccessed: [gold]" + event.tile.build.lastAccessed);
		});
		
		Vars.netServer.admins.addActionFilter(action -> {
			if(action.type == ActionType.pickupBlock) {
				if(action.tile == null) return true;
				if(action.tile.block() == Blocks.thoriumReactor || action.tile.block() == Blocks.impactReactor) return false;
			}
			if(action.type == ActionType.placeBlock) {
				if(action.tile == null) return true;
				if(action.block == Blocks.thoriumReactor || action.block == Blocks.impactReactor) {
					if(action.player == null) return false;
	                var cores = action.player.team().cores();
	                for (int i = 0; i < cores.size; i++) {
	                	if(cores.get(i).dst2(action.tile) <= 500*Vars.tilesize*Vars.tilesize) {
	                		if(lastThoriumReactorPlayer != action.player) Call.sendMessage("[scarlet]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок [" + Game.colorToHex(action.player.color()) + "]" + action.player.name + " []строит реактор рядом с ядром (" + (int)(World.toTile(cores.get(i).dst(action.tile))) + " блоках от ядра)");
	                		lastThoriumReactorPlayer = action.player;
	                		return false;
	                	}
	                }
				}

//				if(action.block.category == Category.defense || action.block.category == Category.turret) {
					if(Vars.state.hasSpawns() && action.tile.block() != Blocks.thoriumReactor){
						for(Tile spawn : Vars.spawner.getSpawns()) {

							int trad = (int)(Vars.state.rules.dropZoneRadius / Vars.tilesize);
							
							
							Tile tile = action.tile;//world.tile(Math.round(x / tilesize) + dx, Math.round(y / tilesize) + dy);
							if(tile != null && action.block != null) {
								int minx = tile.x + action.block.sizeOffset;
								int miny = tile.y + action.block.sizeOffset;
								int cx = spawn.x;
								int cy = spawn.y;
								if(cx < minx) cx = minx;
								if(cx > minx + action.block.size-1) cx = minx + action.block.size-1;

								if(cy < miny) cy = miny;
								if(cy > miny + action.block.size-1) cy = miny + action.block.size-1;

//								Log.info("tile: @, @ at @ @", action.tile, action.block, cx, cy);
								int dx = spawn.x - cx;
								int dy = spawn.y - cy;
								if(dx*dx + dy*dy <= trad*trad) return false;
//								Log.info("Game.effect(Fx.rotateBlock, @, @, 1, Color.red)", cx*Vars.tilesize, cy*Vars.tilesize);
//								int ctilex = tile.build.tileX() + tile.build.block.sizeOffset;
//								if(tile.build.tileX() > spawn.x ? tile.x;
								
//								Block block = tile.build.block;
//								float o = block.offset;
//								SolidPump
							}
//							if(tile != null && tile.build != null && dx*dx + dy*dy <= trad*trad){
//								tile.build.damage(team, damage);
//							}
//							int dx = 
							
//							if(spawn.within(action.tile, state.rules.dropZoneRadius + action.block.size*Vars.tilesize*Mathf.sqrt2/2f)) return false;
						}
					}
			}
			return true;
		});
		// "/chatfilter" command
		Vars.netServer.admins.addChatFilter((player, text) -> {
			if(player != null && text != null) Bots.notify(NotifyTag.chatMessage, "<u><b>" + TelegramBot.strip(player.name) + "</b></u>: " + TelegramBot.strip(text));
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
			AgzamPlugin.dataCollect.messageEvent(player, text);
			return text;
		});
	}

	private static void updateDoors(Tile tile) {
		for (int i = 0; i < doorsCoordinates.size(); i++) {
			int pos = doorsCoordinates.get(i);
			Tile door = Vars.world.tile(pos);
			if(door == tile) continue;
			if((door.block() != Blocks.door && door.block() != Blocks.doorLarge && door.block() != Blocks.blastDoor) || door == null) {
				doorsCoordinates.remove(i);
				i--;
				if(i < 0) i = 0;
			} else {
				int center = Point2.pack(door.centerX(), door.centerY());
				if(center != pos) {
					doorsCoordinates.remove(i);
					i--;
					if(i < 0) i = 0;
				}
			}
		}		
	}

	private static Seq<PlayerCommand> playerCommands = new Seq<PlayerCommand>();
	private static Seq<BaseCommand> serverCommands = new Seq<BaseCommand>();
	private static Seq<BotCommand> botCommands = new Seq<BotCommand>();
	
	enum ReceiverType {
		
		player,
		server,
		bot,
		any;

		String bungle(String name) {
			return Game.bungleDef("command.response." + name + "." + name(), Game.bungle("command.response." + name));
		}
		
		String format(String name, Object... args) {
			return Strings.format(bungle(name), args);
//			return Game.bungle("command.response." + name + "." + name(), args);
		}

		String err(String name) {
			if(this == player) return "[red]" + Game.bungle("command.response." + name);
			return Game.bungle("command.response." + name);
		}
		
	}

	public static void playerCommand(String text, String parms, String desc, CommandRunner<Player> run) {
		playerCommands.add(new PlayerCommand(text, parms, desc, run));
	}

	public static void playerCommand(String text, String desc, CommandRunner<Player> run) {
		playerCommands.add(new PlayerCommand(text, "", desc, run));
	}

	public static void adminCommand(String text, String parms, String desc, CommandRunner<Player> run) {
		playerCommands.add(new PlayerCommand(text, parms, desc, run).admin(true));
	}

	public static void adminCommand(String text, String desc, CommandRunner<Player> run) {
		playerCommands.add(new PlayerCommand(text, "", desc, run).admin(true));
	}
	
	public static void serverCommand(String text, String desc, Cons4<String[], CommandReceiver, Object, ReceiverType> run) {
		playerCommands.add(new PlayerCommand(text, "", desc, (arg, player) -> run.get(arg, player::sendMessage, player, ReceiverType.player)).admin(true));
		serverCommands.add(new BaseCommand(text, "", desc, (arg) -> run.get(arg, Log::info, null, ReceiverType.server)));
		botCommands.add(new BotCommand(text, "", desc, (arg, chat) -> run.get(arg, m -> chat.chat.message(m), chat, ReceiverType.bot)));
	}

	public static void serverCommand(String text, String parms, String desc, Cons4<String[], CommandReceiver, Object, ReceiverType> run) {
		playerCommands.add(new PlayerCommand(text, parms, desc, (arg, player) -> run.get(arg, player::sendMessage, player, ReceiverType.player)).admin(true));
		serverCommands.add(new BaseCommand(text, parms, desc, (arg) -> run.get(arg, Log::info, null, ReceiverType.server)));
		botCommands.add(new BotCommand(text, parms, desc, (arg, chat) -> run.get(arg, m -> chat.chat.message(m), chat, ReceiverType.bot)));
	}

	public static void botCommand(String text, String desc, Cons2<String[], MessageData> run) {
		botCommands.add(new BotCommand(text, "", desc, (arg, chat) -> run.get(arg, chat)));
	}

	public static void botCommand(String text, String parms, String desc, Cons2<String[], MessageData> run) {
		botCommands.add(new BotCommand(text, parms, desc, (arg, chat) -> run.get(arg, chat)));
	}
	
	public static interface CommandReceiver {
		void sendMessage(String message);
	}
	
	static class BotCommand {
		
		private final String text, parms, desc;
		private CommandRunner<MessageData> run;

		public BotCommand(String text, String parms, String desc, CommandRunner<MessageData> run) {
			this.text = text;
			this.parms = parms;
			this.desc = desc;
			this.run = run;
		}
		

		public CommandRunner<MessageData> run() {
			return (args, sender) -> {
				if(!sender.hasPermissions(text)) {
					sender.noAccess(text);
					return;
				}
				run.accept(args, sender);
			};
		}

//		private String build(String[] args) {
//			StringBuilder command = new StringBuilder("&#47;");
//			command.append(text);
//			for (int i = 0; i < args.length; i++) {
//				command.append(' ');
//				command.append(args[i] == null ? "null" : args[i]);
//			}
//			return command.toString();
//		}
	}

	static class BaseCommand {

		private final String text, parms, desc;
		Cons<String[]> run;
		
		public BaseCommand(String text, String parms, String desc, Cons<String[]> run) {
			this.text = text;
			this.parms = parms;
			this.desc = desc;
			this.run = run;
		}

		public Cons<String[]> run() {
			return run;
		}
		
	}
	
	static class PlayerCommand {
		
		private final String text, parms, desc;
		private boolean admin = false;
		private CommandRunner<Player> run;

		public PlayerCommand(String text, String parms, String desc, CommandRunner<Player> run) {
			this.text = text;
			this.parms = parms;
			this.desc = desc;
			this.run = run;
		}

		public PlayerCommand admin(boolean admin) {
			this.admin = admin;
			return this;
		}
		
		public boolean check(Player player) {
			if(!admin) return true;
			if(player.admin()) return true;
			var data = Admins.adminData(player.getInfo());
			if(data == null) return false;
			if(!data.usid.equals(player.usid())) return false;
			return data.has(text);
		}
		
		public CommandRunner<Player> run() {
			if(!admin) return (args, player) -> {
				Bots.notify(NotifyTag.playerCommand, "<b><u>" + TelegramBot.strip(player.plainName()) + "</u></b>: <code>" + build(args) + "</code>");
				run.accept(args, player);
			};
			return (args, player) -> {
				if(!check(player)) return;
				Bots.notify(NotifyTag.adminCommand, 
						player.admin() ? null : Strings.format("#helper <b><u>@</u></b>: <code>@</code>", TelegramBot.strip(player.name), build(args)),
						Strings.format("@ <b><u>@</u></b>: <code>@</code>", player.admin() ? "#admin" : "#helper", TelegramBot.strip(player.name), build(args))
				);
				
				run.accept(args, player);
			};
		}

		private String build(String[] args) {
			StringBuilder command = new StringBuilder("&#47;");
			command.append(text);
			for (int i = 0; i < args.length; i++) {
				command.append(' ');
				command.append(args[i] == null ? "null" : args[i]);
			}
			return command.toString();
		}
	}
	
//	int commandVotekick

	public static void registerClientCommands(CommandHandler handler) {
		handler.removeCommand("a");
		handler.removeCommand("help");
		handler.removeCommand("votekick");
		handler.removeCommand("vote");
//		handler.getCommandList().forEach(command -> {
//			if(command.text.equals("votekick")) {
//				command.description
//			}
//		});
		
		playerCommands.each(c -> handler.register(c.text, c.parms, c.desc, c.run()));

//		registerPlayersCommands(handler);
//		registerAdminCommands(handler);
	}
	
	public static void registerServerCommands(CommandHandler handler) {
		serverCommands.each(c -> handler.register(c.text, c.parms, c.desc, c.run()));
	}

	public static void registerBotCommands(CommandHandler handler) {
		botCommands.each(c -> handler.register(c.text, c.parms, c.desc, c.run()));
	}


	public static void registerAdminCommands() {
		adminCommand("admin", "<add/remove> <name>", "Добавить/удалить админа", (arg, player) -> {
			if(require(arg.length != 2 || !(arg[0].equals("add") || arg[0].equals("remove")), player, "[red]Second parameter must be either 'add' or 'remove'.")) return;
			boolean add = arg[0].equals("add");
			PlayerInfo target;
			Player playert = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[1])));
			if(playert != null) {
				target = playert.getInfo();
			} else {
				target = Vars.netServer.admins.getInfoOptional(arg[1]);
				playert = Groups.player.find(p -> p.getInfo() == target);
			}
			if(require(!add && playert.uuid() == player.uuid(), player, "[red] Вы не можете снять свой статус")) return;
			if(target != null){
				if(add) Vars.netServer.admins.adminPlayer(target.id, playert == null ? target.adminUsid : playert.usid());
				else Vars.netServer.admins.unAdminPlayer(target.id);
				if(playert != null) playert.admin(add);
				player.sendMessage("[gold]Изменен статус администратора игрока: [" + Game.colorToHex(playert.color) + "]" + Strings.stripColors(target.lastName));
			} else {
				player.sendMessage("[red]Игрока с таким именем или ID найти не удалось. При добавлении администратора по имени убедитесь, что он подключен к Сети; в противном случае используйте его UUID");
			}
			Vars.netServer.admins.save();
		});

		adminCommand("m", "", "Открыть меню", (args, admin) -> {
			 var players = new NetMenu("[white]" + Config.serverName.get().toString());

			 for (int i = 0; i < Groups.player.size(); i++) {
				 Player player = Groups.player.index(i);
				 if(admin == null) continue;
				 players.button(player.coloredName(), () -> {
					 var playerControl = new NetMenu(player.coloredName());
					 playerControl.build(() -> {
						 if(Admins.has(admin, "team")) {
							 for (var team : Team.baseTeams) {
								 playerControl.button(team.emoji.isEmpty() ? Strings.format("[#@]@", team.color.toString(), Iconc.logic) : team.emoji, () -> {
									 player.team(team);
								 });
							 }
							 playerControl.row();
						 }
						 playerControl.button("[green]\ue80f Вылечить", () -> {
							 if(player.unit() == null) return;
							 player.unit().heal();
						 });
						 playerControl.button("[red]\uue815 Уничтожить", () -> {
							 if(player.unit() == null) return;
							 player.unit().kill();
						 });
						 playerControl.row();

						 if(player.unit() != null) {
							 Cons2<StatusEffect, String> createEffect = (e,name) -> {
								 if(player.unit().hasEffect(e)) {
									 playerControl.button(Strings.format("[scarlet]@[] @", Iconc.cancel, name), () -> {
										 if(player.unit() == null) return;
										 player.unit().unapply(e);
									 });
									 return;
								 }
								 playerControl.button(Strings.format("[lime]@[] @", Iconc.add, name), () -> {
									 if(player.unit() == null) return;
									 player.unit().apply(e, Float.MAX_VALUE);
								 });
							 };
							 createEffect.get(StatusEffects.invincible, "Неуязвимость");
							 createEffect.get(StatusEffects.fast, "Скорость");
						 }

						 playerControl.row();
						 playerControl.button("[gold]\ue86d Сброс юнита", () -> {
							 if(player.unit() != null) {
								 Game.clearUnit(player);
							 }
						 });
					 });
					 playerControl.show(admin);
				 }).row();
			 }
			 players.show(admin);
    	});
		
		adminCommand("restart", "[code] [args...]", "Перезагрузить сервер", (args, player) -> {
			if(args.length == 0) {
				generateStopCode();
				player.sendMessage("Код остановки сервера: [red]" + stopcode);
				return;
			}
			Log.info("args @ @ @", Arrays.toString(args), args.length, args.length >= 1);
			if(args.length >= 1) {
				Log.info("args[0].equals(stopcode): @ @/@", args[0].equals(stopcode), args[0], stopcode);
				if(require(!args[0].equals(stopcode), player, "[red]Неверный код остановки сервера")) return;
				Log.info("Sucsess code");
				
				Call.sendMessage("[scarlet]Рестарт сервера");
				Call.setHudText("[scarlet]Рестарт сервера");
				Groups.player.each(p -> {
					p.kick(KickReason.serverRestarting);
				});
				Log.info("Restarting");
				String command = "java -jar \"" + Fi.get("").absolutePath() + "/server-release.jar\"" + ((args.length <= 1) ? "" : " " + args[1]);
				Bots.notify(NotifyTag.serverInfo, null, "Restarting server: <code>" + command + "</code>");
				Threads.daemon(() -> {
					Core.app.exit();
					try {
						Runtime.getRuntime().exec(command, new String[] {}, new File("").getParentFile());
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.exit(0);
				});
				Core.app.exit();
				return;
			}
			player.sendMessage("[red]Неверные аргументы");
		});
		
		serverCommand("nextmap", "<название...>", "Устанавливает следущую карту", (arg, sender, receiver, type) -> {
            Map res = Vars.maps.all().find(map -> map.plainName().replace('_', ' ').equalsIgnoreCase(Game.strip(arg[0]).replace('_', ' ')));
            boolean canEventmaps = receiver instanceof Player player ? Admins.has(player, "eventmaps") : true;
            if(arg[0].startsWith("$") && canEventmaps) {
				try {
					EventMap em = EventMap.maps.get(Integer.parseInt(arg[0].substring(1))-1);
					em.setNextMapOverride();
					sender.sendMessage(type.format("nextmap.set-event", em.map().plainName()));
				} catch (Exception e) {
					sender.sendMessage(e.getMessage());
				}
            	return;
            }
            if(res == null && arg[0].startsWith("#")) {
				try {
					res = Vars.maps.all().get(Integer.parseInt(arg[0].substring(1))-1);
				} catch (Exception e) {
					sender.sendMessage(e.getMessage());
				}
			}
            if(res != null){
            	Vars.maps.setNextMapOverride(res);
            	ServerEventsManager.setNextMapEvents(null);
                sender.sendMessage(type.format("nextmap.set", res.plainName()));
            }else{
            	sender.sendMessage(type.err("nextmap.not-found"));
            }
        });

		serverCommand("runwave", "Запускает волну", (arg, sender, receiver, type) -> {
			boolean force = receiver instanceof Player player ? Admins.has(player, "force-runwave") : true;
			if(require(!force && Vars.state.enemies > 0, sender, type.bungle("runwave.enemies"))) return;
			Vars.logic.runWave();
			sender.sendMessage(type.bungle("runwave.ready"));
        });
		
		serverCommand("fillitems", "[item] [count]", "Заполните ядро предметами", (arg, sender, receiver, type) -> {
			try {
				if(arg.length == 0) {
					StringBuilder names = new StringBuilder();
					Vars.content.items().each(i -> {
						if(names.length() != 0) names.append(", ");
						if(type == ReceiverType.player) names.append("[white]" + i.emoji() + " " + Game.getColoredLocalizedItemName(i));
						if(type == ReceiverType.bot) names.append("<code>" + Game.contentName(i) + "</code>");
						if(type == ReceiverType.server) names.append(Game.contentName(i));
					});
					sender.sendMessage(type.format("fillitems.names", names));
					return;
				}
				int count = arg.length > 1 ? Integer.parseInt(arg[1]) : 0;
				
				
				String itemname = arg[0].toLowerCase();
				

				if(itemname.equals("all")) {
					Team team = receiver instanceof Player p ? p.team() : Vars.state.rules.defaultTeam;
					if(require(team.cores().size == 0, sender, type.err("fillitems.no-core"))) return;
					Vars.content.items().each(i -> team.cores().get(0).items.set(i, 9999999));
					return;
				}
				
				Item item = Vars.content.items().find(i -> itemname.equalsIgnoreCase(i.name) || itemname.equalsIgnoreCase(Game.contentName(i)));
				if(require(item == null, sender, type.err("fillitems.no-item"))) return;
				Team team = receiver instanceof Player p ? p.team() : Vars.state.rules.defaultTeam;
				if(require(team.cores().size == 0, sender, type.err("fillitems.no-core"))) return;

				team.cores().get(0).items.add(item, count);
				sender.sendMessage(type.format("fillitems.added", count, item.name));
			} catch (Exception e) {
				sender.sendMessage(e.getMessage());
			}
		});
		
		serverCommand("chatfilter", "<on/off>", "Включить/выключить фильтр чата", (arg, sender, receiver, type) -> {
			if(require(arg.length == 0, sender, "[red]Недостаточно аргументов")) return;
			if(arg[0].equals("on")) {
				chatFilter = true;
				Core.settings.put(AgzamPlugin.name() + "-chat-filter", chatFilter);
				sender.sendMessage("[green]Чат фильтр включен");
			}else if(arg[0].equals("off")) {
				chatFilter = false;
				Core.settings.put(AgzamPlugin.name() + "-chat-filter", chatFilter);
				sender.sendMessage("[red]Чат фильтр выключен");
			}else {
				sender.sendMessage("Неверный аргумент, используйте [gold]on/off");
				return;
			}
		});

		serverCommand("dct", "[time]", "Установить интервал (секунд/10) обновлений данных", (arg, sender, receiver, type) -> {
			if(require(arg.length == 0, sender, "Интервал обновлений: " + AgzamPlugin.dataCollect.getSleepTime() + " секунд/10")) return;
			if(arg.length == 1) {
				long count = 0;
				try {
					count = Long.parseLong(arg[0]);
				} catch (Exception e) {
					sender.sendMessage("[red]Вводить можно только числа!");
				}
				count *= 1_00;

				if(count <= 0) {
					sender.sendMessage("[red]Интервал не может быть меньше 1!");
				}
				AgzamPlugin.dataCollect.setSleepTime(count);
				sender.sendMessage("Установлен интервал: " + count + " ms");
				return;
			}
		});
		
		serverCommand("event", "[id] [on/off/faston]", "Включить/выключить событие", (arg, sender, receiver, type) -> {
			if(arg.length == 0) {
				StringBuilder msg = new StringBuilder();
				for (int i = 0; i < ServerEventsManager.events.size; i++) {
					if(i != 0) msg.append('\n');
					ServerEvent event = ServerEventsManager.events.get(i);
					if(type == ReceiverType.player) {
						msg.append(event.isRunning() ? "[lime]" + Iconc.ok + " " : "[scarlet] " + Iconc.cancel + " ");
						msg.append(event.name);
						msg.append(' ');
					} else if(type == ReceiverType.bot) {
						msg.append(event.isRunning() ? "\u2714 " : "\u274c ");
						msg.append("<code>");
						msg.append(event.name);
						msg.append("</code>");
					} else {
						msg.append(event.isRunning() ? "V " : "X ");
						msg.append("");
						msg.append(event.name);
						msg.append("");
					}
				}
				sender.sendMessage(msg.toString());
				return;
			}
			if(arg.length == 1) {
				ServerEvent event = ServerEventsManager.events.find(e -> arg[0].equals(e.name));
				if(require(event == null, sender, "[red]Событие не найдено, [gold]/event [red] для списка событий")) return;
				sender.sendMessage("Событие " + event.name + "[white] имеет значение: " + event.isRunning());
				return;
			}
			if(arg.length == 2) {
				boolean isOn = false;
				boolean isFast = false;
				if(arg[1].equals("on")) {
					isOn = true;
				} else if(arg[1].equals("off")) {
					isOn = false;
				} else if(arg[1].equals("faston")) {
					isOn = true;
					isFast = true;
				} else {
					sender.sendMessage("Неверный аргумент, используйте [gold]on/off[]");
					return;
				}
				ServerEvent event = ServerEventsManager.events.find(e -> arg[0].equals(e.name));
				if(require(event == null, sender, "[red]Событие не найдено, [gold]/event [red] для списка событий")) return;

				boolean running = event.isRunning();
				boolean await = ServerEventsManager.activeEvents.contains(event);
				
				if(require(running && isOn, sender, "[red]Событие уже запущено")) return;
				if(require(await && isOn, sender, "[red]Событие начнется на следующей карте")) return;
				if(require(!await && !isOn, sender, "[red]Событие итак не запущено")) return;

				if(isOn) {
					if(isFast) {
						ServerEventsManager.fastRunEvent(event.name);
						sender.sendMessage("[white]Событие резко запущено!");
					} else {
						ServerEventsManager.runEvent(event.name);
						sender.sendMessage("[green]Событие запущено!");
					}
				} else {
					ServerEventsManager.stopEvent(event.name);
					sender.sendMessage("[red]Событие остановлено!");
				}

				return;
			}
		});

		serverCommand("team", "[player] [team]", "Установить команду для игрока", (arg, sender, receiver, type) -> {
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
				sender.sendMessage("Команды:\n" + teams.toString());
			}
			if(arg.length == 1) {
				Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[0])));
				if(targetPlayer == null) {
					sender.sendMessage("[red]Игрок не найден");
					return;
				}
				sender.sendMessage("Игрок состоить в команде: " +  targetPlayer.team().name);
				return;
			}
			if(arg.length == 2) {
				Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[0])));
				if(targetPlayer == null) {
					sender.sendMessage("[red]Игрок не найден");
					return;
				}
				sender.sendMessage("Игрок состоить в команде: " +  targetPlayer.team().name);

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
					sender.sendMessage("[red]Команда не найдена");
				} else {
					targetPlayer.team(team);
					if(team.name.equals(Team.crux.name)) {
						Log.info("crux");
						targetPlayer.unit().healTime(.01f);
						targetPlayer.unit().healthMultiplier(100);
						targetPlayer.unit().maxHealth(1000f);
						targetPlayer.unit().apply(StatusEffects.invincible, Float.MAX_VALUE);
					}
					sender.sendMessage("Игрок " + targetPlayer.name() + " отправлен в команду [#" + team.color + "]" + team.name);
					targetPlayer.sendMessage("Вы отправлены в команду [#" + team.color + "]" + team.name);
				}
				return;
			}
		});

		serverCommand("config", "[name] [set/add] [value...]", "Конфигурация сервера", (arg, sender, receiver, type) -> {
			if(arg.length == 0){
				sender.sendMessage("All config values:");
				for(Config c : Config.all){
					sender.sendMessage("[gold]" + c.name + "[lightgray](" + c.description + ")[white]:\n> " + c.get() + "\n");
				}
				return;
			}
			Config c = Config.all.find(conf -> conf.name.equalsIgnoreCase(arg[0]));
			if(c != null){
				if(arg.length == 1) {
					sender.sendMessage(c.name + " is currently " + c.get());
				}else if(arg.length > 2) {
					if(arg[2].equals("default")){
						c.set(c.defaultValue);
					}else if(c.isBool()){
						c.set(arg[2].equals("on") || arg[2].equals("true"));
					}else if(c.isNum()){
						try{
							c.set(Integer.parseInt(arg[2]));
						}catch(NumberFormatException e){
							sender.sendMessage("[red]Not a valid number: " + arg[2]);
							return;
						}
					}else if(c.isString()) {
						if(arg.length > 2) {
							if(arg[1].equals("add")) {
								c.set(c.get().toString() + arg[2].replace("\\n", "\n"));
							} else if(arg[1].equals("set")) {
								c.set(arg[2].replace("\\n", "\n"));
							} else {
								sender.sendMessage("[red]Only [gold]add/set");
								return;
							}
						} else {
							sender.sendMessage("[red]Add [gold]add/set [red]attribute");
						}
					}
					sender.sendMessage("[gold]" + c.name + "[gray] set to [white]" + c.get());
					Core.settings.forceSave();
				} else {
					sender.sendMessage("[red]Need more attributes");
				}
			}else{
				sender.sendMessage("[red]Unknown config: '" + arg[0] + "'. Run the command with no arguments to get a list of valid configs.");
			}
		});

		adminCommand("sandbox", "[on/off] [team]", "Бесконечные ресурсы", (arg, player) -> {
			if(require(arg.length == 0, player, "[gold]infiniteResources: [gray]" + Vars.state.rules.infiniteResources)) return;
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
				Call.setRules(player.con, Vars.state.rules);
			}else if(arg[0].equals("off")) {
				if(team == null) {
					Vars.state.rules.infiniteResources = false;
					player.sendMessage("[red]Выключено!");
				} else {
					team.rules().infiniteResources = false;
					player.sendMessage("[red]Выключено для команды [#" + team.color + "]" + team.name);
				}
				Call.setRules(player.con, Vars.state.rules);
			} else {
				player.sendMessage("[red]Только on/off");
			}
		});

		adminCommand("unit", "[type] [t/c]", "Создает юнита, list для списка", (arg, player) -> {
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
						Unit u = ut.spawn(player.team(), player.mouseX, player.mouseY);
						if(arg.length > 1) {
							if(arg[1].equals("true") || arg[1].equals("y") || arg[1].equals("t") || arg[1].equals("yes")) {
								player.unit(u);
							}
							if(arg[1].equals("c") || arg[1].equals("core")) {
								player.unit(u);
								u.spawnedByCore(true);
							}
						}
						player.sendMessage("Готово!"); 

						if(!Vars.net.client()){
							u.add();
						}
						return;
					}
				}
				player.sendMessage("[red]Юнит не найден [gold]/unit list");
			} catch (IllegalArgumentException | IllegalAccessException e) {
				player.sendMessage(e.getLocalizedMessage());
			}
		});

		serverCommand("bans", "List all banned IPs and IDs", (arg, sender, receiver, type) -> {
			sender.sendMessage("Banned players [ID]:");
			
			Vars.netServer.admins.playerInfo.each((key, info) -> {
				if(info == null) return;
				if(info.banned) {
					sender.sendMessage("> " + key + " - banned");
				}
				if(Time.millis() < info.lastKicked) {
					sender.sendMessage("> " + key + " - kicked [lightgray](" + (info.lastKicked - Time.millis())/1000/60 + " minutes)");
				}
			});
			sender.sendMessage("Banned players [IP]:");
			Vars.netServer.admins.kickedIPs.each((key, time) -> {
				if(Time.millis() < time) {
					sender.sendMessage("> " + key + " - kicked [lightgray](" + (time - Time.millis())/1000/60 + " minutes)");
				}
			});
			sender.sendMessage("Dos players [IP]:");
			Vars.netServer.admins.dosBlacklist.each((value) -> {
				sender.sendMessage("> " + value + " - banned");
			});
		});

		serverCommand("unban", "<ip/ID/all>", "Completely unban a person by IP or ID.", (arg, sender, receiver, type) -> {
			if(require(arg.length == 0, sender, "[red]<ip/ID/all> is missed")) return;
			if(arg[0].equalsIgnoreCase("all")) {
				sender.sendMessage("Unbanned players [ID]:");
				Vars.netServer.admins.playerInfo.each((key, info) -> {
					if(info == null) return;
					info.banned = false;
					if(Time.millis() < info.lastKicked) {
						info.lastKicked = Time.millis();
					}
				});
				sender.sendMessage("Unbanned all IPs");
				Vars.netServer.admins.kickedIPs.clear();
				return;
			}
			PlayerInfo info = Vars.netServer.admins.playerInfo.get(arg[0]);
			Long ip = Vars.netServer.admins.kickedIPs.remove(arg[0]);
			if(require(info == null && ip == null, sender, "player/ip not found")) return;
			if(info != null) {
				info.banned = false;
				if(Time.millis() < info.lastKicked) {
					info.lastKicked = Time.millis();
				}
				sender.sendMessage("Unbanned player: " + info.lastName);
			}
			if(ip != null) sender.sendMessage("Unbanned IP: " + arg[0]);
		});

		serverCommand("reloadmaps", "Перезагрузить карты", (arg, sender, receiver, type) -> {
			int beforeMaps = Vars.maps.all().size;
			Vars.maps.reload();
			if (Vars.maps.all().size > beforeMaps) {
				sender.sendMessage("[gold]" + (Vars.maps.all().size - beforeMaps) + " новых карт было найдено");
			} else if (Vars.maps.all().size < beforeMaps) {
				sender.sendMessage("[gold]" + (beforeMaps - Vars.maps.all().size) + " карт было удалено");
			} else {
				sender.sendMessage("[gold]Карты перезагружены");
			}
			beforeMaps = EventMap.maps.size;
			EventMap.reload();
			if (EventMap.maps.size > beforeMaps) {
				sender.sendMessage("[gold]" + (EventMap.maps.size - beforeMaps) + " ивентных новых карт было найдено");
			} else if (EventMap.maps.size < beforeMaps) {
				sender.sendMessage("[gold]" + (beforeMaps - EventMap.maps.size) + " ивентных карт было удалено");
			} else {
				sender.sendMessage("[gold]Ивентные карты перезагружены");
			}
			AchievementsManager.updateMaps();
		});

		serverCommand("js", "<script...>", "Запустить JS", (arg, sender, receiver, type) -> {
			if(type == ReceiverType.bot) {
				Core.app.post(() -> {
					sender.sendMessage(type.format("js", Vars.mods.getScripts().runConsole(arg[0])));
				});
			} else {
				sender.sendMessage(type.format("js", Vars.mods.getScripts().runConsole(arg[0])));
			}
		});

		serverCommand("link", "<link> [player]", "Отправить ссылку всем/игроку", (arg, sender, receiver, type) -> {
			if(arg.length == 1) {
				Call.openURI(arg[0]);
			} else if(arg.length == 2) {
				Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[1])));
				if(targetPlayer != null) {
					Call.openURI(targetPlayer.con, arg[0]);
					sender.sendMessage("[gold]Готово!");
				} else {
					sender.sendMessage("[red]Игрок не найден");
				}
			}
		});

		serverCommand("setdiscord", "<link>", "\ue80d Сервера", (arg, sender, receiver, type) -> {
			if(arg.length != 1) return;
			discordLink = arg[0];
			Core.settings.put(AgzamPlugin.name() + "-discord-link", discordLink);
			sender.sendMessage(type.bungle("setdiscord"));
		});

//		adminCommand("pardon", "<ID> [index]", "Прощает выбор игрока по ID и позволяет ему присоединиться снова.", (arg, player) -> {
//			int index = 0;
//			if(arg.length >= 2) {
//				try {
//					index = Integer.parseInt(arg[1]);
//				} catch (Exception e) {
//					player.sendMessage("[red]" + e.getMessage());
//					return;
//				}
//			}
//			Seq<PlayerInfo> infos = Vars.netServer.admins.findByName(arg[0]).toSeq();
//			if(index < 0) index = 0;
//			if(index >= infos.size) index = infos.size-1;
//			if(index < 0) {
//				player.sendMessage("[red]No ids");
//				return;
//			}
//			PlayerInfo info = infos.get(index);
//
//			if(info != null){
//				info.lastKicked = 0;
//				Vars.netServer.admins.kickedIPs.remove(info.lastIP);
//				player.sendMessage("Pardoned player: " + info.plainLastName() + " [lightgray](of " + infos.size + " find)");
//			}else{
//				player.sendMessage("[red]That ID can't be found");
//			}
//		});

		serverCommand("doorscup", "[count]", "Устанавливает лимит дверей", (arg, sender, receiver, type) -> {
			if(require(arg.length == 0, sender, type.format("doorscup.doors", doorsCoordinates.size(), doorsCup))) return;
			try {
				int lastDoorsCup = doorsCup;
				doorsCup = Integer.parseInt(arg[0]);
				Core.settings.put(AgzamPlugin.name() + "-doors-cup", doorsCup);
				sender.sendMessage(type.format("doorscup.set", lastDoorsCup, doorsCup));
			} catch (Exception e) {
				sender.sendMessage(e.getMessage());
			}
		});

		adminCommand("brush", "[none/block/floor/overlay/info] [block/none]", "Устанваливает кисточку", (arg, player) -> {
			Brush brush = Brush.get(player);
			if(arg.length == 0) {
				player.sendMessage(Strings.format("Кисточка: [@,@,@]", brush.floor, brush.overlay, brush.block));
			} else if(arg.length == 1) {
				if(arg[0].equalsIgnoreCase("none")) {
					brush.block = null;
					brush.floor = null;
					brush.overlay = null;
					brush.info = false;
					player.sendMessage("[gold]Кисть отчищена");
				} else if(arg[0].equalsIgnoreCase("block")) {
					if(brush.block == null) player.sendMessage("[gold]К кисти не привязан блок");
					else player.sendMessage("[gold]К кисти привязан блок: " + brush.block.emoji() + " [lightgray]" + brush.block);
				} else if(arg[0].equalsIgnoreCase("floor")) {
					if(brush.floor == null) player.sendMessage("[gold]К кисти не привязана поверхность");
					else player.sendMessage("[gold]К кисти привязан блок: " + brush.floor.emoji() + " [lightgray]" + brush.floor);
				} else if(arg[0].equalsIgnoreCase("overlay")) {
					if(brush.overlay == null) player.sendMessage("[gold]К кисти не привязано покрытие");
					else player.sendMessage("[gold]К кисти привязан блок: " + brush.overlay.emoji() + " [lightgray]" + brush.overlay);
				} else if(arg[0].equalsIgnoreCase("info")) {
					brush.info  = true;
					player.sendMessage("[gold]готово!");
				} else {
					player.sendMessage("[red]На первом месте только аргумены [lightgray]none/block/floor/overlay");
				}
			} else if(arg.length == 2) {
				String blockname = arg[1];
				if(blockname.equals("core1")) blockname = "coreShard";
				if(blockname.equals("core2")) blockname = "coreFoundation";
				if(blockname.equals("core3")) blockname = "coreNucleus";
				if(blockname.equals("core4")) blockname = "coreBastion";
				if(blockname.equals("core5")) blockname = "coreCitadel";
				if(blockname.equals("core6")) blockname = "coreAcropolis";
				if(blockname.equals("power+")) blockname = "powerSource";
				if(blockname.equals("power-")) blockname = "powerVoid";
				if(blockname.equals("item+")) blockname = "itemSource";
				if(blockname.equals("item-")) blockname = "itemVoid";
				if(blockname.equals("liq+")) blockname = "liquidSource";
				if(blockname.equals("liq-")) blockname = "liquidVoid";
				if(blockname.equals("s")) blockname = "shieldProjector";
				if(blockname.equals("ls")) blockname = "largeShieldProjector";
				
				if(arg[0].equalsIgnoreCase("block") || arg[0].equalsIgnoreCase("b")) {
					if(arg[1].equals("none")) {
						brush.block = null;
						player.sendMessage("[gold]Блок отвязан");
						return;
					}
					@Nullable Block find = Game.findBlock(blockname);
					if(require(find == null, player, "[red]Поверхность не найдена")) return;
					brush.block = find;
					player.sendMessage("[gold]Поверхность привязана!");
				} else if(arg[0].equalsIgnoreCase("floor") || arg[0].equalsIgnoreCase("f")) {
					if(arg[1].equals("none")) {
						brush.floor = null;
						player.sendMessage("[gold]Поверхность отвязана");
						return;
					}
					@Nullable Block find = Game.findBlock(blockname);
					if(require(find == null, player, "[red]Поверхность не найдена")) return;
					if(require(!(find instanceof Floor), player, "[red]Это не поверхность")) return;
					brush.floor = find;
					player.sendMessage("[gold]Поверхность привязана!");
				} else if(arg[0].equalsIgnoreCase("overlay") || arg[0].equalsIgnoreCase("o")) {
					if(arg[1].equals("none")) {
						brush.overlay = null;
						player.sendMessage("[gold]Покрытие отвязано");
						return;
					}
					@Nullable Block find = Game.findBlock(blockname);
					if(require(find == null, player, "[red]Поверхность не найдена")) return;
					if(require(!(find instanceof OverlayFloor) && find != Blocks.air, player, "[red]Это не поверхность")) return;
					brush.overlay = find;
					player.sendMessage("[gold]Поверхность привязана!");
				}
			}
		});

		adminCommand("etrigger", "<trigger> [args...]", "Устанваливает кисточку", (args, player) -> {
			ServerEventsManager.trigger(player, args);
		});

		adminCommand("extrastar", "[add/remove] [uidd/name]", "", (args, player) -> {
			if(args.length == 0) {
				if(extraStarsUIDD.isEmpty()) {
					player.sendMessage("[gold]Нет игроков");
				} else {
					StringBuilder sb = new StringBuilder("[gold]Игроки с дополнительными звездами:[white]");
					for (int i = 0; i < extraStarsUIDD.size; i++) {
						String uidd = extraStarsUIDD.get(i);
						String name = Vars.netServer.admins.getInfo(uidd).lastName;
						sb.append("\n" + uidd + " (" + name + ")");
					}
					player.sendMessage(sb.toString());
				}
			} else if(args.length == 2) {
				Player playert = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(args[1])));
				if(playert != null) args[1] = playert.uuid();

				if(args[0].equalsIgnoreCase("add")) {
					if(!extraStarsUIDD.contains(args[1])) {
						PlayerInfo info = Vars.netServer.admins.getInfo(args[1]);
						if(info == null) {
							player.sendMessage("[red]Игрок не найден");
						} else {
							extraStarsUIDD.add(args[1]);
							player.sendMessage("[gold]Игрок []" + info.lastName + " [gold]добавлен");
						}
					} else {
						player.sendMessage("[red]Игрок уже есть");
					}
				} else if(args[0].equalsIgnoreCase("remove")) {
					if(extraStarsUIDD.contains(args[1])) {
						PlayerInfo info = Vars.netServer.admins.getInfo(args[1]);
						if(info == null) {
							player.sendMessage("[red]Игрок не найден");
						} else {
							extraStarsUIDD.remove(args[1]);
							player.sendMessage("[gold]Игрок []" + info.lastName + " [gold]убран");
						}
					} else {
						player.sendMessage("[red]UIDD не найден");
					}
				} else {
					player.sendMessage("[red]Только add/remove");
				}
			} else {
				player.sendMessage("[red]Неверные аргументы");
			}
		});

		adminCommand("bot", "[add/remove/list/start/stop/t/p] [id/name] [token...]", "Привязать/отвязать телеграм аккаунт", (arg, player) -> {
			if(require(arg.length < 1, player, "Мало аргументов")) return;
			try {
				arg[0] = arg[0].toLowerCase();

				if(arg[0].equalsIgnoreCase("list")) {
					Cons2<LongMap<? extends TSender>, String> show = (m, name) -> {
						if(m.size == 0) player.sendMessage(Strings.format("@: <empty>", name));
						else {
							player.sendMessage(Strings.format("@:", name));
							m.eachValue(e -> {
								player.sendMessage("> " + e);
							});
						}
					};
					show.get(TelegramBot.users, "Пользователи");
					show.get(TelegramBot.chats, "Чаты");
				}
				
				if(arg[0].equals("add") || arg[0].equals("remove") || arg[0].equals("t") || arg[0].equals("p")) {
					if(require(arg.length < 2, player, "Должно быть 2 аргумента: " + arg[0] + " <id>")) return;

					boolean user = arg[1].startsWith("u-");
					boolean chat = arg[1].startsWith("c-");
					
					LongMap<? extends TSender> senders = null;
					if(chat) senders = TelegramBot.chats;
					if(user) senders = TelegramBot.users;
					
					if(require(senders == null, player, "Неверный Id")) return;
					if(require(!user && !chat, player, "Неверный Id")) return;

					long id = Long.parseUnsignedLong(arg[1].substring(2), Character.MAX_RADIX);

					if(arg[0].equals("add")) {
						if(user) {
							if(TelegramBot.users.containsKey(id)) {
								player.sendMessage("Пользователь [gold]" + id + "[] уже был добавлен!");
							} else {
								TelegramBot.users.put(id, new TUser(id));
								TelegramBot.save();
								player.sendMessage("Пользователь [gold]" + id + "[] добавлен!");
							}
							return;
						}
						if(chat) {
							if(TelegramBot.chats.containsKey(id)) {
								player.sendMessage("Чат [gold]" + id + "[] уже был добавлен!");
							} else {
								TelegramBot.chats.put(id, new TChat(id));
								TelegramBot.save();
								player.sendMessage("Чат [gold]" + id + "[] добавлен!");
							}
							return;
						}
						return;
					}
					if(arg[0].equals("remove")) {
						if(senders.remove(id) == null) player.sendMessage("[lightgray]" + id + "[red] не найден!");
						else player.sendMessage("[lightgray]" + id + "[gold] убран!");
						TelegramBot.save();
						return;
					}

					boolean t = arg[0].equals("t");
					boolean p = arg[0].equals("p");
					
					if(t || p) {
						var sender = senders.get(id);
						if(require(sender == null, player, "Id не найден")) return;
						if(require(arg.length != 3, player, "Должно быть 3 аргумента!")) return;
						Log.info("args: [blue]@[]", Arrays.toString(arg));
						for (var a : arg[2].split(" ")) {
							boolean add = true;
							if(a.startsWith("+")) a = a.substring(1);
							if(a.startsWith("-")) {
								add = false;
								a = a.substring(1);
							}
							Log.info("arg: [blue]@[]",  a);
							if(t) {
								if(add) sender.addTag(a);
								else sender.removeTag(a);
							}
							if(p) {
								if(add) sender.addPermission(a);
								else sender.removePermission(a);
							}
						}
						player.sendMessage("Объект " + id + ":");
						player.sendMessage("Разрешения: " + sender.permissionsString(" "));
						player.sendMessage("Теги: " + sender.tagsString(" "));
						TelegramBot.save();
						return;
					}
					
					return;
				}
				if(arg[0].equals("start")) {
					if(require(arg.length != 3, player, "Должно быть 3 аргумента: start <name> <token>")) return;
					TelegramBot.run(arg[1], arg[2]);
					player.sendMessage("[gold]Бот запущен! [gray](" + arg[1] + " " + arg[2] + ")");
					return;
				}
				if(arg[0].equals("stop")) {
					TelegramBot.stop();
					player.sendMessage("Бот остановлен!");
					return;
				}
			} catch (Exception e) {
				player.sendMessage("[red]" + e.getMessage());
			}
		});
		

		serverCommand("helper", "<add/remove/refresh> [args...]", "Добавить помошника / разрешения", (args, sender, receiver, type) -> {
			int code = 0;
			if(args[0].equalsIgnoreCase("add")) code = 1;
			else if(args[0].equalsIgnoreCase("remove")) code = -1;
			else if(args[0].equalsIgnoreCase("refresh")) code = 2;
			if(code == 0) {
				Player found = Groups.player.find(p -> p.uuid().equals(args[0]));
	            if(found == null) found = Groups.player.find(p -> p.name.equals(args[0]));
				if(require(found == null, sender, "[red]UIID не найден")) return;
	            
				AdminData data = Admins.adminData(found.getInfo());
				if(require(data == null, sender, "[red]Игрок не помошник")) return;
				if(args.length == 1) {
					if(data.permissionsCount() == 0) sender.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [lightgray]<empty>", found.plainName()));
					else sender.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [gold]@", found.plainName(), data.permissionsAsString(' ')));
					return;
				}
				String[] keys = args[1].split(" ");
				for (int i = 0; i < keys.length; i++) {
					String arg = keys[i];
					if(arg.length() < 2) continue;
					Log.info("Argumet: \"@\" with char \"@\" and value \"@\"", arg, arg.charAt(0), arg.substring(1));
					if(arg.charAt(0) == '+') data.add(arg.substring(1));
					else if(arg.charAt(0) == '-') data.remove(arg.substring(1));
				}
				if(data.permissionsCount() == 0) sender.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [lightgray]<empty>", found.plainName()));
				else sender.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [gold]@", found.plainName(), data.permissionsAsString(' ')));
				Admins.save();
				return;
			} else {
				if(require(args.length < 2, sender, "[red]Слишком мало аргументов")) return;

	            Player found = Groups.player.find(p -> p.uuid().equals(args[1]));
	            if(found == null) found = Groups.player.find(p -> p.name.equals(args[1]));
				if(require(found == null, sender, "[red]Игрок не найден")) return;
				if(code == 2) {
					if(Admins.refresh(found)) sender.sendMessage("Игрок [gold]" + found.plainName() + "[] успешно обновлен!");
					else sender.sendMessage("[red]Игрок [gold]" + found.plainName() + "[] уже обновлен!");
				} if(code == 1) {
					if(Admins.add(found)) sender.sendMessage("Игрок [gold]" + found.plainName() + "[] успешно добавлен!");
					else sender.sendMessage("[red]Игрок [gold]" + found.plainName() + "[] уже добавлен!");
				} else if(code == -1) {
					if(Admins.remove(found)) sender.sendMessage("Игрок [gold]" + found.plainName() + "[] успешно удален!");
					else sender.sendMessage("[red]Игрок [gold]" + found.plainName() + "[] не найден!");
				}
				Admins.save();
			}
		});

		adminCommand("nick", "[ник...]", "Установить никнейм на сервере", (args, player) -> {
			String name = args.length == 0 ? "" : args[0];
			name = name.replaceAll(" ", "_");
			if(!Admins.has(player, "longname")) {
				if(name.length() > 100) name = name.substring(0, 100);
			}
			Players.data(player.uuid()).name = name.isEmpty() ? null : name;
			Players.save();
			if(!name.isEmpty()) player.name(name);
			player.sendMessage("[gold]Установлено имя: []" + player.coloredName());
		});

		serverCommand("setnick", "<player> [ник...]", "Установить никнейм на сервере", (args, sender, receiver, type) -> {
			if(args.length == 0) return;
			Player p = Game.findPlayer(args[0]);
			if(require(p == null, sender, "[red]Игрок не найден")) return;

			String name = args.length == 1 ? "" : args[1];
			name = name.replaceAll(" ", "_");
			boolean longname = receiver instanceof Player player ? Admins.has(player, "longname") : true;
			if(!longname && name.length() > 100) name = name.substring(0, 100);
			Players.data(p.uuid()).name = name.isEmpty() ? null : name;
			Players.save();
			sender.sendMessage("[gold]Установлено имя: []" + p.coloredName() + " [gray]->[] " + name);
			if(!name.isEmpty()) p.name(name);
		});

		adminCommand("custom", "<join/leave> [сообщение...]", "Установить сообщение подключения/отключения [lightgray]([coral]@name[] - для имени)", (args, player) -> {
			if(args.length == 0) return;
			boolean join = args[0].equalsIgnoreCase("join");
			boolean leave = args[0].equalsIgnoreCase("leave");
			if(require(!join && !leave, player, "[red]Доступно только join/leave")) return;
			
			String message = args.length == 1 ? "" : args[1];
			if(!Admins.has(player, "longname")) {
				if(message.length() > 200) message = message.substring(0, 200);
			}
			if(join) Players.data(player.uuid()).connectMessage = message.isEmpty() ? null : message;
			if(leave) Players.data(player.uuid()).disconnectedMessage = message.isEmpty() ? null : message;
			Players.save();
			player.sendMessage("[gold]Установлено " + args[0] + ": []" + message);
		});

		serverCommand("setcustom", "<player> <join/leave> [сообщение...]", "Установить сообщение подключения/отключения [lightgray]([coral]@name[] - для имени)", (args, sender, receiver, type) -> {
			if(args.length == 0) return;
			Player p = Game.findPlayer(args[0]);
			if(require(p == null, sender, "[red]Игрок не найден")) return;
			
			boolean join = args[1].equalsIgnoreCase("join");
			boolean leave = args[1].equalsIgnoreCase("leave");
			if(require(!join && !leave, sender, "[red]Доступно только join/leave")) return;
			
			String message = args.length == 2 ? "" : args[2];
			boolean longname = receiver instanceof Player player ? Admins.has(player, "longname") : true;
			if(!longname && message.length() > 200) message = message.substring(0, 200);
			if(join) Players.data(p.uuid()).connectMessage = message.isEmpty() ? null : message;
			if(leave) Players.data(p.uuid()).disconnectedMessage = message.isEmpty() ? null : message;
			Players.save();
			sender.sendMessage("[gold]Установлено " + args[0] + ": []" + message);
		});
		

		serverCommand("threads", "[filter]", "Конфикурация сервера", (args, sender, receiver, type) -> {
			StringBuilder message = new StringBuilder("Threads");
			Func<String, Byte> filter = (keyword) -> {
				if(Structs.contains(args, keyword) || Structs.contains(args, "+" + keyword)) return 1;
				if(Structs.contains(args, "-" + keyword)) return -1;
				return 0;
			};
			byte daemon = filter.get("daemon");
			
			Thread.getAllStackTraces().keySet().forEach(t -> {
				if(daemon == 1 & !t.isDaemon()) return;
				if(daemon == -1 & t.isDaemon()) return;
				
				message.append(Strings.format("\n@: (@) @", t.getName(), t.isDaemon() ? "Daemon" : "", t.getState()));
			});
			sender.sendMessage(message.toString());
		});
	}
	
	static String stopcode = generateStopCode();
	
	private static String generateStopCode() {
		char[] code = new char[10];
		for (int i = 0; i < code.length; i++) {
			code[i] = (char) ('0' + Mathf.random(9));
		}
		stopcode = new String(code);
		return stopcode;
	}

	public static void registerBotCommands() {
		botCommand("help", "Список всех команд", (args, receiver) -> {
			StringBuilder result = new StringBuilder("Команды:\n");
			for (BotCommand command : botCommands) {
				if(!receiver.hasPermissions(command.text)) continue;
				result.append("/").append(command.text).append(" ").append(TelegramBot.escapeHtml(command.parms)).append("<i> - ").append(command.desc).append("</i>\n");
			}
			receiver.sendMessage(result.toString());
		});
		botCommand("players", "Список игроков", (args, receiver) -> {
			if(require(Groups.player.size() == 0, receiver, "Нет игроков")) return;
			try {
				StringBuilder msg = new StringBuilder("Players:");
				Groups.player.each(p -> 
					msg.append("\nName: <code>").append(TelegramBot.strip(p.plainName()))
					.append("</code>\nID: <code>#").append(p.id)
					.append("</code>\nIP: <code>").append(p.ip())
					.append("</code>\nUSID: <code>").append(p.usid())
					.append("</code>\nUUID: <code>").append(p.uuid())
					.append("</code>\n***")
				);
				receiver.sendMessage(msg.toString());
			} catch (Exception e) {
				receiver.sendMessage("<code>" + e.getMessage() + "</code>");
			}
		});

		botCommand("player", "<uuid>" ,"Информация об игроке", (args, receiver) -> {
			if(require(args.length < 1, receiver, "arguments err")) return;
			
			String uuid = args[0];
			
			PlayerEntity entity = joined.get(uuid);
			boolean online = true;
			if(entity == null) {
				online = false;
				entity = Database.player(uuid);
				
			}
			
			@Nullable PlayerInfo info = Vars.netServer.admins.playerInfo.get(uuid);
			
			StringBuilder msg = new StringBuilder("Player <code>").append(uuid).append("</code>:\n");
			msg.append(online ? "(online)\n" : "(offline)\n");
			
			
			if(info != null) {
				msg.append("<b><u>Names:</u></b>\n");
				info.names.each(name -> msg.append("- <code>").append(TelegramBot.strip(name)).append("</code>\n"));

				msg.append("<b><u>IPs:</u></b>\n");
				info.ips.each(ip -> msg.append("- <code>").append(TelegramBot.strip(ip)).append("</code>\n"));
			}
			
			if(entity != null) {
				long playtimeMinutes = entity.playtime;
				long playtimeHours = playtimeMinutes/60;
				long playtimeDays = playtimeHours/24;
				playtimeHours %= 24;
				playtimeMinutes %= 60;

				msg.append("<b><u>Playtime:</u></b> ");
				if(playtimeDays > 0) msg.append("<b>").append(playtimeDays).append("</b> days ");
				if(playtimeHours > 0) msg.append("<b>").append(playtimeHours).append("</b>:");
				msg.append("<b>").append(playtimeMinutes < 10 ? ("0" + playtimeMinutes) : playtimeMinutes).append("</b>");

				msg.append("\n<b><u>Achivments:</u></b> ");
				entity.eachAchievements(a -> {
					msg.append(Strings.format("\n@ @ on @", Achievement.values()[a.type], a.tier, AchievementsManager.mapsIds.findKey(a.map, false)));
				});
				
				
			}
			
			receiver.sendMessage(msg.toString());
			
//			try {
//				StringBuilder msg = new StringBuilder("Players:");
//				Groups.player.each(p -> 
//					msg.append("\nName: <code>").append(TelegramBot.strip(p.plainName()))
//					.append("</code>\nID: <code>#").append(p.id)
//					.append("</code>\nIP: <code>").append(p.ip())
//					.append("</code>\nUSID: <code>").append(p.usid())
//					.append("</code>\nUUID: <code>").append(p.uuid())
//					.append("</code>\n***")
//				);
//				receiver.sendMessage(msg.toString());
//			} catch (Exception e) {
//				receiver.sendMessage("<code>" + e.getMessage() + "</code>");
//			}
		});

//		botCommand("mmap", "Миникарта", (args, receiver) -> {
//			 TODO: move code from bot to here
//		});
//		botCommand("mapm", "Картинка карты", (args, receiver) -> {
//			// TODO: move code from bot to here
//		});
		
		botCommand("kick", "[игрок] [причина...]", "Проголосовать, чтобы кикнуть игрока по уважительной причине", (args, receiver) -> {
			try {
	            if(args.length == 0) {
	                StringBuilder builder = new StringBuilder();
	                builder.append("[orange]Игроки для кика: \n");
	                Groups.player.each(p -> !p.admin && p.con != null, p -> {
	                    builder.append("<code>").append(TelegramBot.strip(p.name)).append("</code> (<code>#").append(p.id()).append("</code>)\n");
	                });
	                receiver.sendMessage(builder.toString());
	            } else {
	            	String reason = args.length < 2 ? "гриф" : args[1];

	            	if(reason.equalsIgnoreCase("g") || reason.equalsIgnoreCase("г")) reason = "гриф";
	            	if(reason.equalsIgnoreCase("f") || reason.equalsIgnoreCase("ф")) reason = "фрикик";
	            	
	                Player found;
	                if (args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))){
	                    int id = Strings.parseInt(args[0].substring(1));
	                    found = Groups.player.find(p -> p.id() == id);
	                } else {
	                    found = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
	                    if(found == null) found = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
	                    if(found == null) found = Groups.player.find(p -> Strings.stripGlyphs(p.name).equalsIgnoreCase(Strings.stripGlyphs(args[0])));
	                    if(found == null) found = Groups.player.find(p -> Strings.stripColors(p.name).equalsIgnoreCase(Strings.stripColors(args[0])));
	                    if(found == null) found = Groups.player.find(p -> Strings.stripColors(Strings.stripGlyphs(p.name)).equalsIgnoreCase(Strings.stripColors(Strings.stripGlyphs(args[0]))));
	                }
	                if(found != null) {
                		kick(found, "сервер", reason);
	                } else {
	                	receiver.sendMessage("[red]Игрок[orange]'" + args[0] + "'[red] не найден.");
	                }
	            }
			} catch (Exception e) {
				receiver.sendMessage("[red]" + e.getLocalizedMessage());
			}
		});
		

		botCommand("this", "информация", (args, receiver) -> {
			StringBuilder result = new StringBuilder();
			result.append(Strings.format("<b><u>User</u></b>:\nID: <code>u-@</code>\nTags: <code>@</code>\nPermissions: <code>@</code>", 
					receiver.user.uid(), receiver.user.tagsString("</code> <code>"), receiver.user.permissionsString("</code> <code>")));
			if(receiver.user != receiver.chat) result.append(Strings.format("\n<b><u>Chat</u></b>:\nID: <code>c-@</code>\nTags: <code>@</code>\nPermissions: <code>@</code>", 
					receiver.chat.uid(), receiver.chat.tagsString("</code> <code>"), receiver.chat.permissionsString("</code> <code>")));
			receiver.sendMessage(result.toString());
		});
	}

	public static void registerPlayersCommands() {
		playerCommand("help", "[страница]", "Список всех команд", (args, player) -> {
			if(require(args.length > 0 && !Strings.canParseInt(args[0]), player, "[red]\"страница\" может быть только числом.")) return;
			final int commandsPerPage = 6;
			int count = 0;
			for (int i = 0; i < playerCommands.size; i++) {
				if(playerCommands.get(i).check(player)) count++;
			}
			int pages = Mathf.ceil((float) count / commandsPerPage);
			int page = (args.length > 0 ? Strings.parseInt(args[0]) : 1)-1;
			if(require(page >= pages || page < 0, player, "[red]\"страница\" должна быть числом между[orange] 1[] и[orange] " + pages + "[red]")) return;
			StringBuilder result = new StringBuilder();
			result.append(Strings.format("[orange]-- Страница команд[lightgray] @[gray]/[lightgray]@[orange] --\n\n", (page + 1), pages));
			int skip = commandsPerPage*page;
			for (int i = 0; i < playerCommands.size; i++) {
				if(!playerCommands.get(i).check(player)) continue;
				if(--skip >= 0) continue;
				if(-skip > commandsPerPage) break;
				PlayerCommand command = playerCommands.get(i);
				result.append("[orange] /").append(command.text).append("[white] ").append(command.parms).append("[lightgray] - ").append(command.desc + (command.admin ? " [red] Только для администраторов" : "")).append("\n");
			}
			player.sendMessage(result.toString());
		});
		
        ObjectMap<String, Timekeeper> cooldowns = new ObjectMap<>();
		playerCommand("votekick", "[игрок] [причина...]", "Проголосовать, чтобы кикнуть игрока по уважительной причине", (args, player) -> {
			try {
				
	            if(require(!Config.enableVotekick.bool(), player, "[red]Голосование на этом сервере отключено")) return;
//	            if(require(Groups.player.size() < 3, player, "[red]Для участия в голосовании требуется как минимум 3 игрока.")) return;
	            if(require(player.isLocal(), player, "[red]Просто кикни их сам, если ты хост")) return;
	            boolean permission = Admins.has(player, "votekick");
	            if(require(currentlyKicking != null && !(permission && !player.admin), player, "[red]Голосование уже идет")) return;

	            if(args.length == 0){
	                StringBuilder builder = new StringBuilder();
	                builder.append("[orange]Игроки для кика: \n");

	                Groups.player.each(p -> !p.admin && p.con != null && p != player, p -> {
	                    builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id()).append(")\n");
	                });
	                player.sendMessage(builder.toString());
	            }else if(args.length == 1){
	                player.sendMessage("[orange]Для кика игрока вам нужна веская причина. Укажите причину после имени игрока");
	            }else{
	            	String reason = args[1];
	            	if(reason.equalsIgnoreCase("g") || reason.equalsIgnoreCase("г")) reason = "гриф";
	            	if(reason.equalsIgnoreCase("f") || reason.equalsIgnoreCase("ф")) reason = "фрикик";
	            	
	                Player found;
	                if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))){
	                    int id = Strings.parseInt(args[0].substring(1));
	                    found = Groups.player.find(p -> p.id() == id);
	                }else{
	                    found = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
	                    if(found == null) found = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
	                    if(found == null) found = Groups.player.find(p -> Strings.stripGlyphs(p.name).equalsIgnoreCase(Strings.stripGlyphs(args[0])));
	                    if(found == null) found = Groups.player.find(p -> Strings.stripColors(p.name).equalsIgnoreCase(Strings.stripColors(args[0])));
	                    if(found == null) found = Groups.player.find(p -> Strings.stripColors(Strings.stripGlyphs(p.name)).equalsIgnoreCase(Strings.stripColors(Strings.stripGlyphs(args[0]))));
	                }
	                if(found != null){
	                    if(found == player){
	                        player.sendMessage("[red]Ты не можешь голосовать за то, чтобы кикнуть себя");
	                    }else if(found.admin){
	                        player.sendMessage("[red]Хо-хо-хо, ты действительно ожидал, что сможешь выгнать администратора?");
	                    }else if(Admins.has(found, "votekick")){
	                        player.sendMessage("[red]Этот игрок защищен пластаном");
	                    }else if(Admins.has(found, "whitelist")){
	                        player.sendMessage("[red]Этот игрок защищен метастеклом");
	                    }else if(found.isLocal()){
	                        player.sendMessage("[red]Локальные игроки не могут быть выгнаны");
	                    }else if(found.team() != player.team()){
	                        player.sendMessage("[red]Кикать можно только игроков из вашей команды");
	                    }else{
	                    	if(permission) {
	                    		kick(found, player.plainName(), reason);
	                    	} else {
	                            Timekeeper vtime = cooldowns.get(player.uuid(), () -> new Timekeeper(NetServer.voteCooldown));
	                            if(!vtime.get()){
	                                player.sendMessage("[red]Вы должны подождать " + NetServer.voteCooldown/60 + " минут между голосованиями");
	                                return;
	                            }
	                            VoteSession session = new VoteSession(found, reason);
	                            Bots.notify(NotifyTag.votekick, Strings.format("<b><u>@</u></b> started voting for kicking <b><u>@</b></u>", TelegramBot.strip(player.name), TelegramBot.strip(found.name)));
	                            Bots.notify(NotifyTag.votekick, Images.screenshot(found));
	                            
	                            session.vote(player, 1);
	                            Call.sendMessage(Strings.format("[lightgray]Причина:[orange] @[lightgray].", reason));
	                            vtime.reset();
	                            currentlyKicking = session;
	                    	}
	                    }
	                }else{
	                    player.sendMessage("[red]Игрок[orange]'" + args[0] + "'[red] не найден.");
	                }
	            }
			} catch (Exception e) {
				Log.err(e);
				player.sendMessage("[red]" + e.getLocalizedMessage());
			}
		});
		
		playerCommand("vote", "<y/n/c>", "Проголосуйте, чтобы выгнать текущего игрока", (arg, player) -> {
			if(require(currentlyKicking == null, player, "[red]Ни за кого не голосуют")) return;
            boolean permission = Admins.has(player, "votekick");
			if(permission && arg[0].equalsIgnoreCase("c")){
				Call.sendMessage(Strings.format("[lightgray]Голосование отменено администратором[orange] @[lightgray].", player.name));
				currentlyKicking.task.cancel();
				currentlyKicking = null;
				return;
			}
			
			if(require(player.isLocal(), player, "[red]Локальные игроки не могут голосовать. Вместо этого кикните игрока сами")) return;

			int sign = switch(arg[0].toLowerCase()){
			case "y", "yes" -> 1;
			case "n", "no" -> -1;
			default -> 0;
			};
			
			if(permission && sign > 0) {
				currentlyKicking.task.cancel();
        		kick(currentlyKicking.target, player.plainName(), currentlyKicking.reason);
				currentlyKicking = null;
        		return;
			}
			
			//hosts can vote all they want
			if((currentlyKicking.voted.get(player.uuid(), 2) == sign || currentlyKicking.voted.get(Vars.netServer.admins.getInfo(player.uuid()).lastIP, 2) == sign)){
				player.sendMessage(Strings.format("[red]Вы уже проголосовали за @", arg[0].toLowerCase()));
				return;
			}
			if(require(currentlyKicking.target == player, player, "[red]Ты не можешь голосовать на за себя")) return;
			if(require(currentlyKicking.target.team() != player.team(), player, "[red]Ты не можешь голосовать на за другие команды")) return;
			if(require(sign == 0, player, "[red]Голосуйте либо \"y\" (да), либо \"n\" (нет)")) return;
			currentlyKicking.vote(player, sign);
        });
		
		playerCommand("maps", "[all/custom/default/event]", "Показывает список доступных карт. Отображает все карты по умолчанию", (arg, player) -> {
			String types = "all";
			if(arg.length == 0) types = Vars.maps.getShuffleMode().name();
			else types = arg[0];
			if(types.startsWith("event")) {
				int id = 0;
				for(EventMap map : EventMap.maps){
					id++;
					String mapName = Strings.stripColors(map.map().name());
					player.sendMessage(Strings.format("[gold]$@ @ [white]| @ [white](@x@, рекорд: @)", 
							id, map.events(), mapName, map.map().width, map.map().height, map.map().getHightScore()));
				}
				return;
			}
			boolean custom  = types.equals("custom") || types.equals("c") || types.equals("all");
			boolean def     = types.equals("default") || types.equals("all");
			if(!Vars.maps.all().isEmpty()) {
				Seq<Map> all = new Seq<>();
				if(custom) all.addAll(Vars.maps.customMaps());
				if(def) all.addAll(Vars.maps.defaultMaps());
				if(all.isEmpty()){
					player.sendMessage("Кастомные карт нет на этом сервере, используйте [gold]all []аргумет.");
				}else{
					player.sendMessage("[white]Maps:");
					int id = 0;
					for(Map map : Vars.maps.all()){
						id++;
						if((def && !map.custom) || (custom && map.custom)) {
							String mapName = Strings.stripColors(map.name());
							player.sendMessage(Strings.format("[gold]#@ @ [white]| @ [white](@x@, рекорд: @)", 
									id, map.custom ? "Кастомная" : "Дефолтная", mapName, map.width, map.height, map.getHightScore()));
						}
					}
				}
			} else {
				player.sendMessage("Карты не найдены");
			}
		});

		playerCommand("a", "<сообщение...>", "Сообщение администраторам", (args, player) -> {
            String raw = "[#" + Color.scarlet.toString() + "]" + Iconc.admin + " " + player.coloredName() + ":[red] " + args[0];
            Groups.player.each(p -> Admins.has(p, "a"), a -> a.sendMessage(raw, player, args[0]));
		});

		playerCommand("discord", "", "\ue80d Сервера", (arg, player) -> {
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
		
		playerCommand("mapinfo", "", "Показывает статистику ресурсов карты", (arg, player) -> {
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
			for(int x = 0; x < Vars.world.width(); x++){
				for(int y = 0; y < Vars.world.height(); y++) {
					if(Vars.world.tile(x, y).block() != Blocks.air) continue;
					Item floor = Vars.world.tile(x, y).floor().itemDrop;
					Item overlay = Vars.world.tile(x, y).overlay().itemDrop;
					Liquid lfloor = Vars.world.tile(x, y).floor().liquidDrop;
					Liquid loverlay = Vars.world.tile(x, y).overlay().liquidDrop;
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
			worldInfo.append("[gold]Название: [lightgray]" + Vars.state.map.name() + "\n");
			worldInfo.append("[gold]Рекорд: [lightgray]" + Vars.state.map.getHightScore() + "\n");
			worldInfo.append("[white]Ресурсы:\n");
			for (int i = 0; i < counter.length; i++) {
				float cv = ((float)counter[i])*typesCounter/summaryCounter/3f;
				if(cv > 1/3f) cv = 1/3f;
				Log.info("cv: @", cv);
				int percent = (int) Math.ceil(counter[i]*100d/summaryCounter);
				Color c = Color.HSVtoRGB(cv*360f, 80, 100);
				worldInfo.append(oreBlocksEmoji[i]);
				worldInfo.append('[');
				worldInfo.append('#');
				worldInfo.append(c.toString());
				worldInfo.append("] ");
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
					worldInfo.append(liquidsEmoji[i]);
					worldInfo.append("[lightgray]: ");
					worldInfo.append(counter[i]);
					isLFound = true;
				}
			}
			if(!isLFound) {
				worldInfo.append(" [red]нет");
			}
			player.sendMessage(worldInfo.toString());
		});

		playerCommand("skipmap", "Начать голосование за пропуск карты", (arg, player) -> {
			if(require(player.team() == Team.derelict, player, "[red]Вы не можете использовать эту команду")) return;
			if(require(currentlyMapSkipping[0] != null, player, "[red]Голосование уже идет: [gold]/smvote <y/n>")) return;
			SkipmapVoteSession session = new SkipmapVoteSession(currentlyMapSkipping);
			session.vote(player, 1);
			currentlyMapSkipping[0] = session;
		});

		playerCommand("smvote", "<y/n>", "Проголосовать за/протов пропуск карты", (arg, player) -> {
			if(require(player.team() == Team.derelict, player, "[red]Вы не можете использовать эту команду")) return;
			if(require(currentlyMapSkipping[0] == null, player, "[red]Нет открытого голосования")) return;
			if(require(player.isLocal(), player, "[red]Локальные игроки не могут голосовать")) return;
			if(require((currentlyMapSkipping[0].voted.contains(player.uuid()) || currentlyMapSkipping[0].voted.contains(Vars.netServer.admins.getInfo(player.uuid()).lastIP)), player, "[red]Ты уже проголосовал. Молчи!")) return;
			String voteSign = arg[0].toLowerCase();
			int sign = 0;
			if(voteSign.equals("y")) sign = +1;
			if(voteSign.equals("n")) sign = -1;
			if(require(sign == 0, player, "[red]Голосуйте либо \"y\" (да), либо \"n\" (нет)")) return;
			currentlyMapSkipping[0].vote(player, sign);
		});

		playerCommand("pluginfo", "info about pluging", (arg, player) -> {
			player.sendMessage(""
					+ "[green] Agzam's plugin " + AgzamPlugin.version() + "\n"
					+ "[gray]========================================================\n"
					+ "[white] Added [royal]skip map[white] commands\n"
					+ "[white] Added protection from [violet]thorium reactors[white]\n"
					+ "[white] Added map list command\n"
					+ "[white] Added fill items by type to core command\n"
					+ "[white] Added Map recourses statistic command\n"
					+ "[white] and more other\n"
					+ "[gray] Download: github.com/Agzam4/Mindustry-plugin");
		});
	}

	public static void kick(Player found, String name, String reason) {
		int minutes = 5;
		Integer last = lastkickTime.get(found.uuid());
		if(last != null) minutes = last;
		minutes = Math.min(minutes, 60);
		lastkickTime.put(found.uuid(), minutes*2);
		Bots.notify(NotifyTag.votekick, Strings.format("Выдан бан на <b>@</b> минут\nПричина: <i>@</i>\nБан выдал: <i>@</i>", minutes, TelegramBot.strip(reason), TelegramBot.strip(name)));
        Bots.notify(NotifyTag.votekick, Images.screenshot(found));
		
        found.kick(Strings.format("Вы были забанены на [red]@[] минут\nПричина: [orange]@[white]\nБан выдал: [orange]@[]\nОбжаловать: @", minutes, reason, name, discordLink), minutes * 60 * 1000);
		if(discordLink != null) {
			if(!discordLink.isEmpty()) Call.openURI(found.con, discordLink);
		}
		Call.sendMessage(Strings.format("[white]Игрок [orange]@[white] забанен на [orange]@[] минут [lightgray](причина: @)", found.plainName(), minutes, reason));
	}

	public static boolean require(boolean b, Player player, String string) {
		if(b) player.sendMessage(string);
		return b;
	}

	public static boolean require(boolean b, CommandReceiver receiver, String string) {
		if(b) receiver.sendMessage(string);
		return b;
	}

	public static class SkipmapVoteSession {

		float voteDuration = 3 * 60;
		ObjectSet<String> voted = new ObjectSet<>();
		SkipmapVoteSession[] map;
		Timer.Task task;
		int votes;

		int votesRequiredSkipmap;

		public SkipmapVoteSession(SkipmapVoteSession[] map){
			this.map = map;
			votesRequiredSkipmap = votesRequiredSkipmap();
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
			voted.addAll(player.uuid()); // FIXME: , Vars.netServer.admins.getInfo(player.uuid()).lastIP
			Call.sendMessage(Strings.format("[" + Game.colorToHex(player.color) + "]@[lightgray] проголосовал " + (d > 0 ? "[green]за" : "[red]против") + "[] пропуска карты[accent] (@/@)\n[lightgray]Напишите[orange] /smvote <y/n>[], чтобы проголосовать [green]за[]/[red]против",
					player.name, votes, votesRequiredSkipmap));
			checkPass();
		}

		boolean checkPass(){
			if(votes >= votesRequiredSkipmap) {
				Call.sendMessage("[gold]Голосование закончилось. Карта успешно пропущена!");
				Events.fire(new GameOverEvent(Team.derelict));
				map[0] = null;
				task.cancel();
				return true;
			}
			return false;
		}

		void update() {
			votesRequiredSkipmap = votesRequiredSkipmap();
		}
	}
	
	 public static class VoteSession {
	        Player target;
	        ObjectIntMap<String> voted = new ObjectIntMap<>();
	        Timer.Task task;
	        int votes;
	        String reason;

	        public VoteSession(Player target, String reason){
	            this.target = target;
	            this.reason = reason;
	            this.task = Timer.schedule(() -> {
	                if(!checkPass()){
	                    Call.sendMessage(Strings.format("[lightgray]Голосование провалено. Недостаточно голосов, чтобы кикнуть [orange] @[lightgray].", target.name));
	                    Bots.notify(NotifyTag.votekick, "Голосование провалено");
		                currentlyKicking = null;
	                    task.cancel();
	                }
	            }, NetServer.voteDuration);
	        }

	        public void vote(Player player, int d){
	            int lastVote = voted.get(player.uuid(), 0) | voted.get(Vars.netServer.admins.getInfo(player.uuid()).lastIP, 0) | voted.get(player.ip(), 0);
	            votes -= lastVote;

	            votes += d;
	            voted.put(player.uuid(), d);
	            voted.put(Vars.netServer.admins.getInfo(player.uuid()).lastIP, d);
	            voted.put(player.ip(), d);

	            Call.sendMessage(Strings.format("[lightgray]@[lightgray] проголосовал за кик[orange] @[lightgray].[accent] (@/@)\n[lightgray]Напиши[orange] /vote <y/n>[] чтобы проголосовать",
	                player.name, target.name, votes, votesRequired()));

	            checkPass();
	        }

	        public boolean checkPass(){
	            if(votes >= votesRequired()){
	                Call.sendMessage(Strings.format("[orange]Голосование принято. [red] @[orange] забанен на @ минут", target.name, (NetServer.kickDuration / 60)));
	                
	                Bots.notify(NotifyTag.votekick, 
	                		Strings.format("Голосование принято. <b><u>@</u></b> забанен на @ минут", TelegramBot.strip(target.name), NetServer.kickDuration / 60),
	                		Strings.format("Голосование принято. <b><u>@</u></b> забанен на @ минут\nUUID: <code>@</code>\nUSID: <code>@</code>\nIP: <code>@</code>", TelegramBot.strip(target.name), (NetServer.kickDuration / 60), target.uuid(), target.usid(), target.ip())
	                );
	                
	                Groups.player.each(p -> p.uuid().equals(target.uuid()), p -> {
	                	p.kick(KickReason.vote, NetServer.kickDuration * 1000);
		                Vars.netServer.admins.handleKicked(p.uuid(), p.ip(), NetServer.kickDuration * 1000);
	                });
	                currentlyKicking = null;
	                task.cancel();
	                return true;
	            }
	            return false;
	        }

	        public int votesRequired(){
	            return 2 + (Groups.player.size() > 4 ? 1 : 0);
	        }
	    }

	public static void stopSkipmapVoteSession() {
		for (int i = 0; i < currentlyMapSkipping.length; i++) {
			if(currentlyMapSkipping[i] == null) continue;
			currentlyMapSkipping[i].task.cancel();
			currentlyMapSkipping[i].votes = Integer.MIN_VALUE;
			currentlyMapSkipping[i] = null;
		}
	}

	public static int votesRequiredSkipmap(){
		if(Groups.player.size() == 1) return 1;
		if(Groups.player.size() == 2) return 2;
		return (int) Math.ceil(Groups.player.size()*0.45);
	}

	public static void clearDoors() {
		doorsCoordinates.clear();
	}

//	public static Sound sound(int id) {
//		if(id < 0) return null;
//		if(id >= sounds.size) return null;
//		return sounds.get(id);
//	}

	public static void cleanUpKicks() {
		Vars.netServer.admins.kickedIPs.copy().each((key,time) -> {
			if(Time.millis() < time) return;
			Vars.netServer.admins.kickedIPs.remove(key);
		});
	}
	
	private static class Brush {
		

		private static Seq<Brush> brushes = Seq.with();

		private final Player owner;
		private Block block, floor, overlay;
		private int brushLastX = -1, brushLastY = -1;
		public boolean info = false;
		
		public Brush(Player owner) {
			this.owner = owner;
		}
		
		private void update() {
    		if(owner == null) return;
    		if(!owner.shooting()) {
    			brushLastY = brushLastX = -1;
    			return;
    		}
    		
    		if(block == null && floor == null && overlay == null) return;
    		
    		int tileX = World.toTile(owner.mouseX());
    		int tileY = World.toTile(owner.mouseY());
    		if(brushLastX == tileX && brushLastY == tileY) return;

    		if(brushLastX == -1 || brushLastY == -1) {
        		brushLastX = tileX;
        		brushLastY = tileY;
        		if(brushLastX == -1 || brushLastY == -1) return;
        		paintTile(Vars.world.tile(tileX, tileY), false);
    			return;
    		}
    		
    		int x0 = brushLastX;
    		int y0 = brushLastY;
    		int x1 = tileX;
    		int y1 = tileY;
    		
    		brushLastX = tileX;
    		brushLastY = tileY;
    		
    		if(x0 == -1 || y0 == -1 || x1 == -1 || y1 == -1) return;
    		
    		Bresenham2.line(x0, y0, x1, y1, (x,y) -> paintTile(Vars.world.tile(x, y), x != x0 || y != y0));
		}

		public static Brush get(Player player) {
			var brush = brushes.find(b -> b.owner == player);
			if(brush == null) {
				brush = new Brush(player);
				brushes.add(brush);
			}
			return brush;
		}

		private void paintTile(@Nullable Tile tile, boolean saveCores) {
			if(tile == null) return;
			if(block != null) {
				if(!(saveCores && tile.block() instanceof CoreBlock)) {
					tile.setNet(block, owner.team(), 0);
				}
			}
			if(floor != null) {
				if(overlay != null) {
					tile.setFloorNet(floor, overlay);
				} else {
					tile.setFloorNet(floor, tile.overlay());
				}
			} else {
				if(overlay != null) {
					tile.setFloorNet(tile.floor(), overlay);
				}
			}		
		}
	}
	
}
