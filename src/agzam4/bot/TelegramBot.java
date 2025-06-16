package agzam4.bot;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import agzam4.Game;
import arc.files.Fi;
import arc.func.Boolf;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.graphics.Pal;
import mindustry.net.Administration.PlayerInfo;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.storage.CoreBlock;

public class TelegramBot extends TelegramLongPollingBot {

	public static Fi botTokenPath = Fi.get(Vars.saveDirectory + "/bot/bot.save");
	public static Fi botFollowersPath = Fi.get(Vars.saveDirectory + "/bot/followers.save");
	
	private static String username, token;
	private static boolean isRunning;
	public static TelegramBot bot;
	public static Seq<Long> followers = new Seq<Long>();

	public static CommandHandler handler = new CommandHandler("/");
	
	public TelegramBot(String token) {
		super(token);
	}
	
	public static void init() {
		if(botTokenPath.exists()) {
			try {
				String[] data = botTokenPath.readString().split(" ");
				if(data.length == 2) run(data[0], data[1]);
			} catch (Exception e) {
				Log.err(e);
			}
		}
		if(botFollowersPath.exists()) {
			try {
				String[] data = botFollowersPath.readString().split("\n");
				for (int i = 0; i < data.length; i++) {
					try {
						Long id = Long.parseLong(data[i]);
						followers.add(id);
					} catch (Exception e) {
						Log.err(e);
					}
				}
			} catch (Exception e) {
				Log.err(e);
			}
		}
	}
	
	public static void saveFollowers() {
		try {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < followers.size; i++) {
				if(i != 0) builder.append('\n');
				builder.append(followers.get(i));
			}
			botFollowersPath.writeString(builder.toString(), false);
		} catch (Exception e) {
			Log.err(e);
		}
	}
	
	static BotSession session;
	public static int[][] mapColors;

	public static void run(String n, String t) {
		username = n;
		token = t;
		
		bot = new TelegramBot(token);
		try {
			if(!isRunning) {
				TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
				session = botsApi.registerBot(bot);
				botTokenPath.writeString(n + " " + t, false);
			}
			isRunning = true;
		} catch (TelegramApiException e) {
			Log.err(e);;
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
			if(message.getChatId() == null) return;
			long chatId = message.getChatId();
			if(hasFollower(chatId)) {
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
		} catch (Exception e) {
			Log.err(e);
		}
	}

	private boolean require(boolean b, long chatId, String string) {
		if(b) sendMessageHtml(chatId, string);
		return b;
	}

	private void sendPlayer(long chatId, Player found) {
		if(found == null) return;
		int dx = (int) (found.tileX() - Vars.world.width()/6);
		int dy = (int) (found.tileY() - Vars.world.height()/6);
		
		BufferedImage screen = takeScreen(dx, dy, Vars.world.width()/3, Vars.world.height()/3, false);
		drawData(screen, false, dx, dy);
		sendMessagePhoto(chatId, screen);		
	}

	private static void drawData(BufferedImage screen, boolean single, int dx, int dy) {
		int scale = single ? 1 : 3;
		int sdx = -dx*scale;
		int sdy = -dy*scale;
		Graphics2D g = (Graphics2D) screen.getGraphics();
		g.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int i = 0; i < Groups.player.size(); i++) {
			var p = Groups.player.index(i);								
			if(p.unit() != null) {
				for (int pl = 0; pl < p.unit().plans.size; pl++) {
					var plan = p.unit().plans.get(pl);
					g.setColor(plan.breaking ? Color.red : Color.yellow);
					g.drawLine(p.tileX()*scale+sdx, screen.getHeight()-(p.tileY()*scale+sdy), 
							plan.x*scale+sdx, screen.getHeight()-(plan.y*scale+sdy));
				}
			}
			drawString(screen, g, p.plainName(), p.color, p.tileX()*scale+sdx, p.tileY()*scale+sdy);
		}
		for (int y = 0; y < Vars.world.width(); y++) {
			for (int x = 0; x < Vars.world.height(); x++) {
				Tile t = Vars.world.tile(x, y);
				if(t == null) continue;
				if(!t.isCenter()) continue;
				if(t.block() == null) continue;
				if(!t.block().hasBuilding()) continue;
				if(t.block() instanceof CoreBlock) {
					drawString(screen, g, t.block().localizedName, t.team().color, x*scale+sdx, y*scale+sdy);
				}
				if(t.block() instanceof LogicBlock) {
					drawString(screen, g, "P", Pal.logicOperations, x*scale+sdx, y*scale+sdy);
				}
			}
		}
		Groups.unit.each(un -> {
			g.setColor(new Color(un.team.color.rgb888()));
			if(!single) {
				if(!un.spawnedByCore) drawString(screen, g, un.type.name, un.team.color, un.tileX()*scale+sdx, un.tileY()*scale+sdy);
			}
			g.fillOval(un.tileX()*scale+sdx, screen.getHeight()-(un.tileY()*scale+sdy),4,4);
		});
		g.dispose();		
	}

	private static BufferedImage takeScreen(int sx, int sy, int w, int h, boolean single) {
		BufferedImage screen = new BufferedImage(single ? w : w*3, single ? h : h*3, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < screen.getHeight(); y++) {
			for (int x = 0; x < screen.getWidth(); x++) {
				screen.setRGB(x, y, x | (y << 8));
			}
		}
		Log.info("screenshot (@;@) @x@", sx, sy, w, h);
		
		Boolf<Tile> hasBlock = t -> t != null && t.build != null;
		
		try {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					Tile t = Vars.world.tile(sx+x, sy+h-y-1);
					if(t == null) continue;
					if(t.block() != null && t.block() != Blocks.air) {
						int size = t.block().size;
						int dx = t.x-t.centerX()+size/2 - 1 + (size%2);
						int dy = t.centerY()-t.y+size/2;
						int[] arr = mapColors[t.blockID()];
						if(single) {
							screen.setRGB(x, y, mapColors[t.blockID()][dx*3+1+dy*3*size + size]);
						} else {
							for (int py = 0; py < 3; py++) {
								for (int px = 0; px < 3; px++) {
									int index = dx*3+px + (dy*3+py)*size*3;
									if(index < 0 || index >= arr.length) continue;
									screen.setRGB(x*3+px, y*3+py, arr[index]);
								}
							}
						}
						continue;
					}
					if(single && (hasBlock.get(Vars.world.tile(x-1, h-y-1)) 
							|| hasBlock.get(Vars.world.tile(x+1, h-y-1)) 
							|| hasBlock.get(Vars.world.tile(x, h-y-2))
							|| hasBlock.get(Vars.world.tile(x, h-y)))) {
						screen.setRGB(x, y, Color.black.getRGB());
						continue;
					}
					if(t.floor() != null && t.floor() != Blocks.air) {
						if(single) {
							screen.setRGB(x, y, mapColors[t.floorID()][4]);
						} else {
							for (int py = 0; py < 3; py++) {
								for (int px = 0; px < 3; px++) {
									int rgba = mapColors[t.floorID()][px+py*3];
									screen.setRGB(x*3+px, y*3+py, rgba);
								}
							}
						}
					}
					if(t.overlay() != null && t.overlay() != Blocks.air) {
						if(single) {
							screen.setRGB(x, y, mapColors[t.overlayID()][4]);
						} else {
							for (int py = 0; py < 3; py++) {
								for (int px = 0; px < 3; px++) {
									int rgba = mapColors[t.overlayID()][px+py*3];
									if(!isTransparent(rgba)) screen.setRGB(x*3+px, y*3+py, rgba);
								}
							}
						}
						continue;
					}
				}
			}	
		} catch (Exception e) {
			Log.err(e);
		}
		return screen;
	}
	
	private static boolean isTransparent(int rgba) {
		return (rgba & 0x000000ff) == 0;
	}

	private static void drawString(BufferedImage img, Graphics2D g, String text, arc.graphics.Color color, int x, int y) {
		g.setColor(Color.black);
		g.drawString(text, x - g.getFontMetrics().stringWidth(text)/2, (img.getHeight()-y-1)-5);
		g.setColor(new Color(color.rgb888()));
		g.drawString(text, x - g.getFontMetrics().stringWidth(text)/2-1, (img.getHeight()-y-1)-4);
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

	public void sendMessagePhoto(long id, BufferedImage image) {
		if(bot == null) return;
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", outputStream);
			SendPhoto sendPhoto = new SendPhoto();
			sendPhoto.setChatId(id);
			ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
			sendPhoto.setPhoto(new InputFile(stream, "tmp-" + System.nanoTime()));
			execute(sendPhoto);
			outputStream.close();
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static void sendPlayerToAll(Player found) {
		if(found == null) return;
		if(bot == null) return;
		int dx = (int) (found.tileX() - Vars.world.width()/6);
		int dy = (int) (found.tileY() - Vars.world.height()/6);
		
		BufferedImage screen = takeScreen(dx, dy, Vars.world.width()/3, Vars.world.height()/3, false);
		drawData(screen, false, dx, dy);
		followers.each(id -> bot.sendMessagePhoto(id, screen));
	}

	private boolean hasFollower(long chatId) {
		return followers.contains(chatId);
	}

	public static void sendToAll(String message) {
		if(bot == null) return;
		final String msg = Strings.stripGlyphs(message);
		followers.each(id -> {
			bot.sendMessageHtml(id, msg);
		});
	}

	public static void sendTo(Long id, String message) {
		if(bot == null) return;
		final String msg = Strings.stripGlyphs(message);
		bot.sendMessageHtml(id, msg);
	}

	@Override
	public String getBotUsername() {
		return username;
	}

	public static String strip(String str) {
		return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

}
