package agzam4.bot;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

import agzam4.Game;
import agzam4.Log;
import arc.files.Fi;
import arc.struct.LongMap;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Player;

public class TelegramBot extends TelegramLongPollingBot {

	public static Fi botTokenPath = Fi.get(Vars.saveDirectory + "/bot/bot.bin");
	public static Fi botUsersPath = Fi.get(Vars.saveDirectory + "/bot/users.txt");
	public static Fi botChatsPath = Fi.get(Vars.saveDirectory + "/bot/chats.txt");
	
	private static String username, token;
	private static boolean isRunning;
	public static TelegramBot bot;
	
	public static LongMap<TChat> chats = new LongMap<>();
	public static LongMap<TUser> users = new LongMap<>();
	
	public TelegramBot(String token) {
		super(token);
	}

	@Override
	public String getBotUsername() {
		return username;
	}
	
	public static void init() {
		Log.info("Loading bot...");
		
		if(botTokenPath.exists()) {
			try {
				String[] data = botTokenPath.readString().split(" ");
				if(data.length == 2) run(data[0], data[1]);
			} catch (Exception e) {
				Log.err(e);
			}
		} else {
			Log.info("Token not found");
		}
		

		try {
			if(botUsersPath.exists())
			for (var line : botUsersPath.readString().split("\n")) {
				try {
					TUser user = new TUser(line);
					users.put(user.id, user);
				} catch (Exception e) {}
			}
		} catch (Exception e) {
			Log.err(e);
		}

		try {
			if(botChatsPath.exists())
			for (var line : botChatsPath.readString().split("\n")) {
				try {
					TChat chat = new TChat(line);
					chats.put(chat.id, chat);
				} catch (Exception e) {}
			}
		} catch (Exception e) {
			Log.err(e);
		}
		Log.info("Bot loaded!");
	}
	
	public static void save() {
		try {
			save(users, botUsersPath);
			save(chats, botChatsPath);
		} catch (Exception e) {
			Log.err(e);
		}
	}
	
	private static <T> void save(LongMap<T> values, Fi file) {
		try {
			StringBuilder builder = new StringBuilder();
			values.eachValue((e) -> {
				if(builder.length() != 0) builder.append('\n');
				var obj = e.toString();
				if(obj == null) return;
				builder.append(obj);
			});
			Log.info("saving [blue]@[] [lime]@[] [gray]@[]", file, builder.toString(), values);
			file.writeString(builder.toString(), false);
		} catch (Exception e) {
			Log.err(e);
		}
	}
	
	static BotSession session;

	public static void run(String n, String t) {
		username = n;
		token = t;
		
		bot = new TelegramBot(token);
		try {
			if(!isRunning) {
				TelegramBotsApi botsApi = new TelegramBotsApi(DaemonBotSession.class);
				session = botsApi.registerBot(bot);
				botTokenPath.writeString(n + " " + t, false);
			}
			Log.info("Bot [blue]@[] is running", n.length() > 5 ? n.substring(0, 5)+"..." : n.substring(0, n.length()));
			isRunning = true;
		} catch (TelegramApiException e) {
			Log.err(e);
			isRunning = false;
			bot = null;
		}
	}
	
	@Override
	public void onUpdateReceived(Update u) {
		try {
			if(u == null) return;
			if(u.getUpdateId() == null) return;
	        Message message = u.getMessage();
	        if(message == null) return;
	        
			String text = message.getText();
			if(text == null) return;
			
			if(message.getChatId() == null) return;

			var from = message.getFrom();
			if(from == null) return;
			var fromId = from.getId();
			if(fromId == null) return;

			TUser user = users.get(fromId);
			
			long chatId = message.getChatId();
			
			if(user == null) {
				sendMessageMarkdown(chatId, "Аккаунт не найден, ваш id: `u-" + Long.toUnsignedString(fromId, Character.MAX_RADIX) + "`");
				return;
			}
			
			if(chatId == user.id) {
				user.onMessage(user, text);
				return;
			}

			var chat = chats.get(chatId);
			
			if(chat == null) {
				sendMessageMarkdown(chatId, "Чат не найден, id чата: `c-" + Long.toUnsignedString(chatId, Character.MAX_RADIX) + "`");
				return;
			}

			user.onMessage(chat, text);
			
			/*
			
			Log.info("chatId: @, user: @", chatId, message.getFrom().getId());
			if(chats.containsKey(chatId)) {
				TChat chat = chats.get(chatId);
				
				
				
				
				
//				if(tUid == chatId) {
//					chat.users(message);
//				}
				
				
				if(!message.isCommand()) {
					String txt = message.getText();
					if(txt != null) Call.sendMessage(txt);
				} else {
					String txt = message.getText();
					System.out.println(txt);
					if(txt != null) {
						if(txt.equals("/map") || txt.equals("/mapm")) {
							boolean single = !txt.startsWith("/mapm");
							BufferedImage screen = takeScreen(0, 0, Vars.world.width(), Vars.world.height(), single);
							drawData(screen, single, 0, 0);
							sendMessagePhoto(chatId, screen);
						} else if(txt.startsWith("/at ")) {
							String args = txt.substring("/at ".length());
							Player found = Groups.player.find(p -> p.plainName().equalsIgnoreCase(args));
							if(found == null) found = Groups.player.find(p -> p.plainName().indexOf(args) != -1);
							if(found == null) {
								sendMessageHtml(chatId, "Player <i>" + args + "</i> not found");
								return;
							}
							sendPlayer(chatId, found);
//						} else if(txt.startsWith("/kick ")) {
//							String args = txt.substring("/kick ".length());
//							Player found = Groups.player.find(p -> Strings.stripGlyphs(Strings.stripColors(p.plainName())).equalsIgnoreCase(args) || p.uuid().equals(args));
//							if(found == null) found = Groups.player.find(p -> ("#" + p.id).equals(args));
//							if(found == null) {
//								sendMessageHtml(chatId, "Player <i>" + args + "</i> not found");
//								return;
//							}
//							ExamplePlugin.commandsManager.kick(found, "сервер", "неизвестно");
						} else if(txt.startsWith("/admin ")) {
							String[] args = txt.substring("/admin ".length()).split(" ");
							
							if(require(args.length != 2 || !(args[0].equals("add") || args[0].equals("remove")), chatId, "Second parameter must be either 'add' or 'remove'.")) return;
							boolean add = args[0].equals("add");
							PlayerInfo target;
							Player playert = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(args[1])));
							if(playert != null) {
								target = playert.getInfo();
							} else {
								target = Vars.netServer.admins.getInfoOptional(args[1]);
								playert = Groups.player.find(p -> p.getInfo() == target);
							}
							if(target != null){
								if(add) Vars.netServer.admins.adminPlayer(target.id, playert == null ? target.adminUsid : playert.usid());
								else Vars.netServer.admins.unAdminPlayer(target.id);
								if(playert != null) playert.admin(add);
								sendMessageMarkdown(chatId, "Изменен статус администратора игрока: " + Game.strip(target.lastName) + " admin: " + add);
							} else {
								sendMessageMarkdown(chatId, "Игрока с таким именем или ID найти не удалось. При добавлении администратора по имени убедитесь, что он подключен к Сети; в противном случае используйте его UUID");
							}
							Vars.netServer.admins.save();
						} else if (txt.startsWith("/")) {
							handler.handleMessage(txt, chatId);
						}
					};
				}
			} else {
				sendMessageMarkdown(chatId, "Подтвердите свой аккаунт, зайдя в миндастри и прописав: `/bot add " + chatId + "`");
			}
			*/
		} catch (Exception e) {
			Log.err(e);
		}
	}

//	private boolean require(boolean b, long chatId, String string) {
//		if(b) sendMessageHtml(chatId, string);
//		return b;
//	}
	
	public void sendMessageMarkdown(long id, String message) {
		if(bot == null) return;
		try {
			execute(SendMessage.builder().chatId(id)
					.text(message).parseMode("markdown")
					.build());
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}
	
	public static void stop() {
		botTokenPath.writeString("", false);
		if(session != null) session.stop();
	}

	public void sendMessageHtml(long id, String message) {
		if(bot == null) return;
		try {
			execute(SendMessage.builder().chatId(id)
					.text(message).parseMode("html")
					.build());
		} catch (TelegramApiException e) {
			Log.err("Message: @", message);
			Log.err(e);
		}
	}
	
	public static String escapeHtml(String text) {
	    return text.replace("&", "&amp;")
	               .replace("<", "&lt;")
	               .replace(">", "&gt;");
	}

	public static void sendMessagePhoto(long id, BufferedImage image) {
		if(bot == null) return;
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", outputStream);
			SendPhoto sendPhoto = new SendPhoto();
			sendPhoto.setChatId(id);
			ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
			sendPhoto.setPhoto(new InputFile(stream, "tmp-" + System.nanoTime()));
			bot.execute(sendPhoto);
			outputStream.close();
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Deprecated
	public static void sendPlayerToAll(Player found) {}

	@Deprecated
	public static void sendToAll(String message) {}

	public static void sendTo(Long id, String message) {
		if(bot == null) return;
		final String msg = Strings.stripGlyphs(message);
		bot.sendMessageHtml(id, msg);
	}

	public static String strip(String str) {
		return escapeHtml(Game.strip(str));//str.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

}
