package agzam4;

import java.nio.ByteBuffer;
import agzam4.achievements.AchievementsManager;
import agzam4.achievements.AchievementsManager.Achievement;
import agzam4.admins.Admins;
import agzam4.bot.Bots;
import agzam4.bot.Bots.NotifyTag;
import agzam4.bot.TUser.MessageData;
import agzam4.bot.TelegramBot;
import agzam4.commands.*;
import agzam4.commands.players.*;
import agzam4.commands.server.*;
import agzam4.commands.admin.*;
import agzam4.commands.any.*;
import agzam4.database.Database;
import agzam4.database.Database.PlayerEntity;
import agzam4.events.*;
import agzam4.io.ByteBufferIO;
import agzam4.managers.Kicks;
import agzam4.managers.Players;
import agzam4.utils.Log;
import arc.Events;
import arc.func.*;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.ArcRuntimeException;
import arc.util.CommandHandler.CommandRunner;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.MappableContent;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.Administration.*;
import mindustry.world.*;
import static agzam4.Emoji.*;

public class CommandsManager {

	private CommandsManager() {}
	
	private static Server server = new Server();
	
	private static Player lastThoriumReactorPlayer;
	
	public static boolean needServerRestart;
	
	public static void init() {
		registerBotCommands();
		registerPlayersCommands();
		registerAdminCommands();
		
		Vars.netServer.addBinaryPacketHandler("agzam4.cmd-sug", (player, bs) -> {
			try {
				var buffer = ByteBuffer.wrap(bs);
				
				byte id = buffer.get();
				
				String command = ByteBufferIO.readString(buffer);
				if(!anyAcsessCommands.contains(command) && !Admins.has(player, command)) return;

				var completer = commandCompleters.get(command);
				if(completer == null) return;
				
				String[] args = new String[buffer.get()];
				for (int i = 0; i < args.length; i++) {
					args[i] = ByteBufferIO.readString(buffer);
				}
				var result = completer.complete(args, player, ReceiverType.player);
				if(result == null) return;

				int maxSize = Byte.MAX_VALUE;
				int parts = (result.size+maxSize-1)/maxSize;
				for (int p = 0; p < parts; p++) {
					int offset = p*maxSize;
					int size = Math.min(offset+maxSize, result.size) - offset;
					var res = ByteBuffer.allocate(size * Byte.MAX_VALUE);
					res.put(id);
					res.putShort((short) offset);
					res.put((byte) (size));
					res.putShort((short) result.size);
					for (int i = 0; i < size; i++) {
						var obj = result.get(i+offset);
						if(obj instanceof MappableContent content) {
							res.put((byte) content.getContentType().ordinal());
							res.putShort(content.id);
						} else {
							res.put((byte) -1);
							ByteBufferIO.writeString(res, obj.toString());
						}
					}
					Call.clientBinaryPacketReliable(player.con, "agzam4.cmd-sug", res.array());
				}
			} catch (Exception e) {
				Log.err(e);
			}
		});
		
		Events.on(GameOverEvent.class, e -> {
			if(needServerRestart) {
				Game.stop();
			}
		});
		
		Events.on(PlayerLeave.class, e -> {
			if(e.player == null) return;
			Bots.notify(NotifyTag.playerConnection, NotifyTag.playerConnection.bungle("left", TelegramBot.strip(e.player.name), Players.joinedAmount()));
			
			PlayerData data = PlayersData.getData(e.player.uuid());
			if(data == null || data.disconnectedMessage == null) Call.sendMessage(e.player.coloredName() + "[accent] отключился");
			else Call.sendMessage("[accent]" + data.disconnectedMessage.replaceAll("@name", e.player.coloredName() + "[accent]"));
		});

    	Events.on(PlayerJoin.class, e -> {
			Call.hideHudText(e.player.con);
    		
    		Bots.notify(NotifyTag.playerConnection,
    				Strings.format("<b>@</b>@ has joined <i>(@ players)</i>", TelegramBot.strip(e.player.name), (e.player.admin() ? " (admin)":""), Players.joinedAmount()),
    				Strings.format("<b>@</b>@ has joined <i>(@ players)</i> <code>@</code>", TelegramBot.strip(e.player.name), (e.player.admin() ? " (admin)":""), Players.joinedAmount(), e.player.uuid())
    		);
			
    		PlayerData data = PlayersData.getData(e.player.uuid());

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
				boolean extrastar = Server.extrastarUids.contains(e.player.uuid());
				if(extrastar) color = Color.magenta;
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
				for (float j = 0; j < count; j++) stars.append('\ue809');
				if(extrastar) stars.append("[magenta]\ue813");
				Call.sendMessage("Игрок " + e.player.name() + "[white] имеет рейтинг " + stars.toString());
			} else {
				Call.sendMessage("Игрок " + e.player.name() + "[white] в первый раз на этом сервере!");
			}
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
		Vars.netServer.admins.addChatFilter((player, text) -> {
			if(player != null && text != null) {
				Bots.notify(NotifyTag.chatMessage, "<u><b>" + TelegramBot.strip(player.name) + "</b></u>: " + TelegramBot.strip(text));
			}
			if(ChatfilterCommand.chatFilter) {
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
			return text;
		});
	}

	private static Seq<PlayerCommand> playerCommands = new Seq<PlayerCommand>();
	private static Seq<BaseCommand> serverCommands = new Seq<BaseCommand>();
	private static Seq<BotCommand> botCommands = new Seq<BotCommand>();
	private static ObjectMap<String, CommandHandler<? super Player>> commandCompleters = ObjectMap.of();
	private static ObjectSet<String> anyAcsessCommands = ObjectSet.with();
	
	public static enum ReceiverType {
		
		player,
		server,
		bot,
		any;

		public String bungle(String name) {
			return Game.bungleDef("command.response." + name + "." + name(), Game.bungle("command.response." + name));
		}
		
		public String format(String name, Object... args) {
			return Strings.format(bungle(name), args);
//			return Game.bungle("command.response." + name + "." + name(), args);
		}

		public String err(String name) {
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
	
	public static void serverCommand(String text, String desc, Cons4<String[], ResultSender, Object, ReceiverType> run) {
		playerCommands.add(new PlayerCommand(text, "", desc, (arg, player) -> run.get(arg, player::sendMessage, player, ReceiverType.player)).admin(true));
		serverCommands.add(new BaseCommand(text, "", desc, (arg) -> run.get(arg, Log::info, null, ReceiverType.server)));
		botCommands.add(new BotCommand(text, "", desc, (arg, chat) -> run.get(arg, m -> chat.chat.message(m), chat, ReceiverType.bot)));
	}

	public static void serverCommand(String text, String parms, String desc, Cons4<String[], ResultSender, Object, ReceiverType> run) {
		playerCommands.add(new PlayerCommand(text, parms, desc, (arg, player) -> run.get(arg, player::sendMessage, player, ReceiverType.player)).admin(true));
		serverCommands.add(new BaseCommand(text, parms, desc, (arg) -> run.get(arg, Log::info, null, ReceiverType.server)));
		botCommands.add(new BotCommand(text, parms, desc, (arg, chat) -> run.get(arg, m -> chat.chat.message(m), chat, ReceiverType.bot)));
	}

	public static void playerCommand(CommandHandler<Player> run) {
		commandCompleters.put(run.text, run);
		anyAcsessCommands.add(run.text);
		playerCommands.add(new PlayerCommand(run.text, run.parms, run.desc, (args, player) -> run.command(args, player::sendMessage, player, ReceiverType.player)));
	}
	
	public static void anyCommand(CommandHandler<Object> run) {
		commandCompleters.put(run.text, run);
		anyAcsessCommands.add(run.text);
		playerCommands.add(new PlayerCommand(run.text, run.parms, run.desc, (arg, player) -> run.command(arg, player::sendMessage, player, ReceiverType.player)));
		serverCommands.add(new BaseCommand(run.text, run.parms, run.desc, (arg) -> run.command(arg, Log::info, server, ReceiverType.server)));
		botCommands.add(new BotCommand(run.text, run.parms, run.desc, (arg, chat) -> run.command(arg, m -> chat.chat.message(m), chat, ReceiverType.bot)));
	}

	public static void adminCommand(CommandHandler<Player> run) {
		commandCompleters.put(run.text, run);
		playerCommands.add(new PlayerCommand(run.text, run.parms, run.desc, (args, player) -> run.command(args, player::sendMessage, player, ReceiverType.player)).admin(true));
	}
	
	public static void serverCommand(CommandHandler<Object> run) {
		commandCompleters.put(run.text, run);
		playerCommands.add(new PlayerCommand(run.text, run.parms, run.desc, (arg, player) -> run.command(arg, player::sendMessage, player, ReceiverType.player)).admin(true));
		serverCommands.add(new BaseCommand(run.text, run.parms, run.desc, (arg) -> run.command(arg, Log::info, null, ReceiverType.server)));
		botCommands.add(new BotCommand(run.text, run.parms, run.desc, (arg, chat) -> run.command(arg, m -> chat.chat.message(m), chat, ReceiverType.bot)));
	}
	
	public static void serverCommand(String text, String parms, String desc, CommandHandler<Object> run) {
		commandCompleters.put(text, run);
		playerCommands.add(new PlayerCommand(text, parms, desc, (arg, player) -> run.command(arg, player::sendMessage, player, ReceiverType.player)).admin(true));
		serverCommands.add(new BaseCommand(text, parms, desc, (arg) -> run.command(arg, Log::info, null, ReceiverType.server)));
		botCommands.add(new BotCommand(text, parms, desc, (arg, chat) -> run.command(arg, m -> chat.chat.message(m), chat, ReceiverType.bot)));
	}

	public static void botCommand(String text, String desc, Cons2<String[], MessageData> run) {
		botCommands.add(new BotCommand(text, "", desc, (arg, chat) -> run.get(arg, chat)));
	}

	public static void botCommand(String text, String parms, String desc, Cons2<String[], MessageData> run) {
		botCommands.add(new BotCommand(text, parms, desc, (arg, chat) -> run.get(arg, chat)));
	}
	
	public static interface ResultSender {
		void sendMessage(String message);
	}
	
	public static class BotCommand {

		private boolean registered;
		public final String text, parms, desc;
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
		private boolean registered;
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
	
	public static class PlayerCommand {
		
		public final String text, parms, desc;
		public boolean admin = false;
		private CommandRunner<Player> run;
		private boolean registered;

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

	public static void flushClientCommands() {
//		handler.removeCommand("a");
//		handler.removeCommand("help");
//		handler.removeCommand("votekick");
//		handler.removeCommand("vote");
//		handler.getCommandList().forEach(command -> {
//			if(command.text.equals("votekick")) {
//				command.description
//			}
//		});
		var handler = AgzamPlugin.clientHandler;
		
		playerCommands.each(c -> {
			if(c.registered) return;
			handler.removeCommand(c.text);
			handler.register(c.text, c.parms, c.desc, c.run());
			c.registered = true;
		});

//		registerPlayersCommands(handler);
//		registerAdminCommands(handler);
	}


	public static PlayerCommand removeClientCommand(String text) {
		PlayerCommand cmd = playerCommands.find(c -> c.text.equals(text));
		if(cmd == null) throw new ArcRuntimeException("Command not found");
		playerCommands.remove(cmd);
		if(cmd.registered) AgzamPlugin.clientHandler.removeCommand(text);
		cmd.registered = false;
		return cmd;
	}

	public static BaseCommand removeServerCommand(String text) {
		BaseCommand cmd = serverCommands.find(c -> c.text.equals(text));
		if(cmd == null) throw new ArcRuntimeException("Command not found");
		serverCommands.remove(cmd);
		if(cmd.registered) AgzamPlugin.serverHandler.removeCommand(text);
		cmd.registered = false;
		return cmd;
	}

	public static BotCommand removeBotCommand(String text) {
		BotCommand cmd = botCommands.find(c -> c.text.equals(text));
		if(cmd == null) throw new ArcRuntimeException("Command not found");
		botCommands.remove(cmd);
		if(cmd.registered) AgzamPlugin.serverHandler.removeCommand(text);
		cmd.registered = false;
		return cmd;
	}
	
	public static void flushServerCommands() {
		var handler = AgzamPlugin.serverHandler;
		serverCommands.each(c -> {
			if(c.registered) return;
			handler.removeCommand(c.text);
			handler.register(c.text, c.parms, c.desc, c.run());
			c.registered = true;
		});
	}

	public static void flushBotCommands() {
		var handler = Bots.handler;
		botCommands.each(c -> {
			if(c.registered) return;
			handler.removeCommand(c.text);
			handler.register(c.text, c.parms, c.desc, c.run());
			c.registered = true;
		});
	}


	public static void registerAdminCommands() {
		adminCommand(new AdminCommand());
		adminCommand(new UnitCommand());
		adminCommand(new BrushCommand());
		adminCommand(new agzam4.commands.admin.BotCommand());
		adminCommand(new NickCommand());
		adminCommand(new MCommand());
		adminCommand(new CustomCommand());
		adminCommand("etrigger", "<trigger> [args...]", "Устанваливает кисточку", (args, player) -> ServerEventsManager.trigger(player, args));
		
		serverCommand(new ConfigCommand());
		serverCommand(new FillitemsCommand());
		serverCommand(new NextmapCommand());
		serverCommand(new EventCommand());
		serverCommand(new RestartCommand());
		serverCommand(new SetcustomCommand());
		serverCommand(new UnbanCommand());
		serverCommand(new LinkCommand());
		serverCommand(new HelperCommand());
		serverCommand(new SetnickCommand());
		serverCommand(new TeamCommand());
		serverCommand(new ReloadmapsCommand());
		serverCommand(new RunwaveCommand());
		serverCommand(new BansCommand());
		serverCommand(new ChatfilterCommand());
		serverCommand(new JsCommand());
		serverCommand(new SetdiscordCommand());
		serverCommand(new ThreadsCommand());
		serverCommand(new DoorscapCommand());
		serverCommand(new SandboxCommand());
		serverCommand(new ExtrastarCommand());
		serverCommand(new InfoCommand());


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
			
			PlayerEntity entity = Players.joinedEntity(uuid);
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

		botCommand("this", "информация", (args, receiver) -> {
			StringBuilder result = new StringBuilder();
			result.append(Strings.format("<b><u>User</u></b>:\nID: <code>u-@</code>\nTags: <code>@</code>\nPermissions: <code>@</code>", 
					receiver.user.fuid(), receiver.user.tagsString("</code> <code>"), receiver.user.permissionsString("</code> <code>")));
			if(receiver.user != receiver.chat) result.append(Strings.format("\n<b><u>Chat</u></b>:\nID: <code>c-@</code>\nTags: <code>@</code>\nPermissions: <code>@</code>", 
					receiver.chat.fuid(), receiver.chat.tagsString("</code> <code>"), receiver.chat.permissionsString("</code> <code>")));
			receiver.sendMessage(result.toString());
		});
		

		botCommand("map", "mini screen of map", (args, receiver) -> {
			receiver.chat.message(Images.screenshot(0, 0, Vars.world.width(), Vars.world.height(), true));
		});
		botCommand("mapm", "large screen of map", (args, receiver) -> {
			receiver.chat.message(Images.screenshot(0, 0, Vars.world.width(), Vars.world.height(), false));
		});
		botCommand("at", "<player>", "screen around player", (args, receiver) -> {
			if(require(args.length != 1, receiver, "wrong args amount")) return;
			Player found = Groups.player.find(p -> p.plainName().equalsIgnoreCase(args[0]));
			if(found == null) found = Groups.player.find(p -> p.plainName().indexOf(args[0]) != -1);
			if(found == null) {
				receiver.sendMessage("Player <i>" + args[0] + "</i> not found");
				return;
			}
			receiver.chat.message(Images.screenshot(found));
		});
		botCommand("say", "<message...>", "оправить сообщение в игровой чат", (args, receiver) -> {
			if(require(args.length != 1, receiver, "wrong args amount")) return;
			Call.sendMessage(Strings.format("[coral][[[white]@[coral]]:[white] @", receiver.user.name, args[0]));
		});
//		botCommand("kick", "<player>", "kick player by name", (args, receiver) -> {
//			if(require(args.length != 1, receiver, "wrong args amount")) return;
//			Player found = Groups.player.find(p -> p.plainName().equalsIgnoreCase(args[0]));
//			if(found == null) found = Groups.player.find(p -> p.plainName().indexOf(args[0]) != -1);
//			if(found == null) {
//				receiver.sendMessage("Player <i>" + args[0] + "</i> not found");
//				return;
//			}
//			kick(found, "сервер", "неизвестно");
//		});
		botCommand("kick", "[игрок] [причина...]", "Проголосовать, чтобы кикнуть игрока по уважительной причине", (args, receiver) -> {
			try {
	            if(args.length == 0){
	                StringBuilder builder = new StringBuilder();
	                builder.append("Игроки для кика:");
	                Groups.player.each(p -> !p.admin && p.con != null, p -> {
	                    builder.append(Strings.format("\n<code>@</code> <code>#@</code>", TelegramBot.strip(p.name), p.id()));
	                });
	                receiver.sendMessage(builder.toString());
	                return;
	            }
	            
				if(require(args.length != 2, receiver, "wrong args amount")) return;
	            
	            String reason = args[1];
	            if(reason.equalsIgnoreCase("g") || reason.equalsIgnoreCase("г")) reason = "гриф";
	            if(reason.equalsIgnoreCase("f") || reason.equalsIgnoreCase("ф")) reason = "фрикик";

	            Player found;
	            if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))) {
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
	    			if(require(Admins.has(found, "votekick"), receiver, "Этот игрок защищен пластаном")) return;
	    			if(require(Admins.has(found, Permissions.whitelist), receiver, "Этот игрок защищен метастеклом")) return;
            		Kicks.kick(receiver.user.name, found, reason);
    				receiver.sendMessage("Игрок забанен");
	            } else {
	            	receiver.sendMessage("Игрок " + args[0] + " не найден.");
	            }
			} catch (Exception e) {
				Log.err(e);
				receiver.sendMessage(e.getLocalizedMessage());
			}
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
		
		playerCommand(new VotekickCommand());
		playerCommand(new DiscordCommand());

		anyCommand(new SkipmapCommand());
		anyCommand(new SmvoteCommand());
		anyCommand(new MapinfoCommand());
		anyCommand(new MapsCommand());
		anyCommand(new VoteCommand());

		playerCommand("a", "<сообщение...>", "Сообщение администраторам", (args, player) -> {
            String raw = "[#" + Color.scarlet.toString() + "]" + Iconc.admin + " " + player.coloredName() + ":[red] " + args[0];
            Groups.player.each(p -> Admins.has(p, "a"), a -> a.sendMessage(raw, player, args[0]));
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

	public static boolean require(boolean b, Player player, String string) {
		if(b) player.sendMessage(string);
		return b;
	}

	public static boolean require(boolean b, ResultSender receiver, String string) {
		if(b) receiver.sendMessage(string);
		return b;
	}

//	public static void clearDoors() {
//		doorsCoordinates.clear();
//	}

	public static void cleanUpKicks() {
		Vars.netServer.admins.kickedIPs.copy().each((key,time) -> {
			if(Time.millis() < time) return;
			Vars.netServer.admins.kickedIPs.remove(key);
		});
	}

	public static void flushCommands() {
    	CommandsManager.flushBotCommands();
    	CommandsManager.flushServerCommands();
    	CommandsManager.flushClientCommands();		
	}

	public static Seq<PlayerCommand> playerCommands() {
		return playerCommands;
	}
	
	public static Seq<BotCommand> botCommands() {
		return botCommands;
	}
}
