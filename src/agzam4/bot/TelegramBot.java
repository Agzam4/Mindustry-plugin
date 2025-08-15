package agzam4.bot;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import javax.imageio.ImageIO;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage.SendMessageBuilder;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

import agzam4.Game;
import agzam4.utils.Log;
import arc.files.Fi;
import arc.func.Cons;
import arc.struct.LongMap;
import arc.util.ArcRuntimeException;
import arc.util.Strings;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonWriter;
import arc.util.serialization.JsonWriter.OutputType;
import mindustry.Vars;
import mindustry.gen.Player;

public class TelegramBot extends TelegramLongPollingBot {

	private static JsonReader reader = new JsonReader();
	
	public static Fi botTokenPath = Fi.get(Vars.saveDirectory + "/bot/bot.bin");
	public static Fi botUsersPath = Fi.get(Vars.saveDirectory + "/bot/users.json");
	public static Fi botChatsPath = Fi.get(Vars.saveDirectory + "/bot/chats.json");
	
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
		load();
		Log.info("Bot loaded!");
	}
	
	public static void load() {
		try {
			if(botUsersPath.exists()) {
				for (var json : reader.parse(botUsersPath)) {
					try {
						TUser chat = new TUser(json);
						users.put(chat.id, chat);
					} catch (Exception e) {}
				}
			}
		} catch (Exception e) {
			Log.err(e);
		}
		try {
			if(botChatsPath.exists()) {
				for (var json : reader.parse(botChatsPath)) {
					try {
						TChat chat = new TChat(json);
						if(chat.thread != null) throw new ArcRuntimeException(Strings.format("Group @ has thread id", chat.id));
						chats.put(chat.id, chat);
					} catch (Exception e) {}
				}
			}
		} catch (Exception e) {
			Log.err(e);
		}
	}
	
	public static void save() {
		try {
			save(users, botUsersPath);
			save(chats, botChatsPath);
		} catch (Exception e) {
			Log.err(e);
		}
	}
	
	private static <T extends TSender> void save(LongMap<T> values, Fi file) {
		try {
			Log.info("Saving [blue]@[]", values);
	        StringWriter string = new StringWriter();
			var writer = new JsonWriter(string);
			writer.array();
			for (var e : values) {
				writer.object();
				e.value.write(writer);
				writer.pop();
			}
			writer.pop();
			writer.close();
			var w = file.writer(false);
			reader.parse(string.toString()).prettyPrint(OutputType.json,w);
			w.close();
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
			var threadId = message.getMessageThreadId();
			
			if(user == null) {
				send(b -> {
					b.chatId(chatId);
					if(threadId != null) b.messageThreadId(threadId);
					b.text("Аккаунт не найден, ваш id: `u-" + Long.toUnsignedString(fromId, Character.MAX_RADIX) + "`");
					b.parseMode("markdown");
				});
				return;
			}
			
			if(chatId == fromId) {
				user.onMessage(user, text);
				return;
			}

			var chat = chats.get(chatId);

			if(threadId == null) {
				if(chat == null) {
					sendMessageMarkdown(chatId, "Чат не найден, id чата: `c-" + Long.toUnsignedString(chatId, Character.MAX_RADIX) + "`");
					return;
				}
				user.onMessage(chat, text);
			} else {
				if(chat == null) {
					send(b -> {
						b.chatId(chatId);
						b.messageThreadId(threadId);
						b.text("Чат не найден, id чата: `c-" + Long.toUnsignedString(chatId, Character.MAX_RADIX) + "`");
						b.parseMode("markdown");
					});
					return;
				}
				var thread = chat.thread(threadId);
				if(thread == null) {
					send(b -> {
						b.chatId(chatId);
						b.messageThreadId(threadId);
						b.text("Тема не найдена, id темы: `c-" + chat.uid() + "/" + Integer.toUnsignedString(threadId, Character.MAX_RADIX) + "`");
						b.parseMode("markdown");
					});
					return;
				}
				user.onMessage(thread, text);
			}
		} catch (Exception e) {
			Log.err(e);
		}
	}
	
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

	public static void send(SendPhoto photo) {
		if(bot == null) return;
		try {
			bot.execute(photo);
		} catch (Exception e) {
			Log.err(e);
		}
	}
	
	public static void send(Cons<SendMessageBuilder> cons) {
		if(bot == null) return;
		try {
			var builder = SendMessage.builder();
			cons.get(builder);
			bot.execute(builder.build());
		} catch (Exception e) {
			Log.err(e);
		}
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
