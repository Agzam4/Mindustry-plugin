package agzam4.commands.admin;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.bot.Bots.NotifyTag;
import agzam4.bot.TChat;
import agzam4.bot.TSender;
import agzam4.bot.TUser;
import agzam4.bot.TelegramBot;
import agzam4.commands.CommandHandler;
import arc.func.Cons2;
import arc.struct.LongMap;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.gen.Player;

public class BotCommand extends CommandHandler<Player> {

	{
		parms = "[add/remove/list/start/stop/t/p/name/load] [id/name] [token...]";
		desc = "Привязать/отвязать телеграм аккаунт";
	}
	
	@Override
	public void command(String[] args, ResultSender player, Player receiver, ReceiverType type) {
		if(require(args.length < 1, player, "Мало аргументов")) return;
		try {
			args[0] = args[0].toLowerCase();

			if(args[0].equalsIgnoreCase("load")) {
				try {
					TelegramBot.init();
					player.sendMessage("Chats & Users loaded!");
				} catch (Exception exc) {
					player.sendMessage("Error: " + exc.getMessage());
				}
				return;
			}
			if(args[0].equalsIgnoreCase("list")) {
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
			
			if(args[0].equals("add") || args[0].equals("remove") || args[0].equals("t") || args[0].equals("p") || args[0].equals("name")) {
				if(require(args.length < 2, player, "Должно быть 2 аргумента: " + args[0] + " <id>")) return;

				boolean isUser = args[1].startsWith("u-");
				boolean isChat = args[1].startsWith("c-");
				
				LongMap<? extends TSender> senders = null;
				if(isChat) senders = TelegramBot.chats;
				if(isUser) senders = TelegramBot.users;
				
				if(require(senders == null, player, "Неверный Id")) return;
				if(require(!isUser && !isChat, player, "Неверный Id")) return;

				String[] idData = args[1].split("/");
				Integer threadId = idData.length >= 2 ? Integer.parseUnsignedInt(idData[1], Character.MAX_RADIX) : null;
				long id = Long.parseUnsignedLong(idData[0].substring(2), Character.MAX_RADIX);

				if(args[0].equals("name")) {
					if(require(!isUser, player, "Это не пользователь")) return;
					var user = TelegramBot.users.get(id);
					if(require(user == null, player, "Пользователь не найден")) return;
					if(require(args.length < 3, player, "Имя пользователя: " + user.name)) return;
					user.name = args[2];
					player.sendMessage("Установлено имя: " + user.name);
					return;
				}
				
				if(args[0].equals("add")) {
					if(isUser) {
						if(TelegramBot.users.containsKey(id)) {
							player.sendMessage("Пользователь [gold]" + id + "[] уже был добавлен!");
						} else {
							TelegramBot.users.put(id, new TUser(id));
							TelegramBot.save();
							player.sendMessage("Пользователь [gold]" + id + "[] добавлен!");
						}
						return;
					}
					if(isChat) {
						if(threadId != null) {
							TChat chat = TelegramBot.chats.get(id);
							if(chat == null) {
								chat = new TChat(id);
								player.sendMessage("Чат [gold]" + id + "[] добавлен!");
								TelegramBot.chats.put(id, chat);
							}
							TChat thread = new TChat(id);
							thread.thread = threadId;
							
							chat.thread(thread);
							player.sendMessage(Strings.format("Тема [gold]@/@[] добавлена!", id, threadId));
							
							TelegramBot.save();
							return;
						}
						
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
				if(args[0].equals("remove")) {
					if(threadId == null) {
						if(senders.remove(id) == null) player.sendMessage("[lightgray]" + id + "[red] не найден!");
						else player.sendMessage("[lightgray]" + id + "[gold] убран!");
					} else {
						var sender = senders.get(id);
						if(require(sender == null, player, "Чат не найден")) return;
						if(sender instanceof TChat chat) {
							if(chat.threads.remove(threadId) == null) player.sendMessage("Тема [lightgray]" + id + "[red] не найдена!");
							else player.sendMessage("Тема [lightgray]" + id + "[gold] убрана!");
							TelegramBot.save();
							return;
						}
						player.sendMessage("Это не чат");
						return;
					}
					TelegramBot.save();
					return;
				}

				boolean t = args[0].equals("t");
				boolean p = args[0].equals("p");
				
				if(t || p) {
					TSender sender = senders.get(id);
					if(require(sender == null, player, "Id не найден")) return;

					if(threadId != null) {
						if(!(sender instanceof TChat chat)) {
							player.sendMessage("Это не чат");
							return;
						}
						sender = chat.threads.get(threadId);
						if(require(sender == null, player, "Тема не найдена")) return;
					}
					
					if(args.length >= 3) {
						for (var a : args[2].split(" ")) {
							boolean add = true;
							if(a.startsWith("+")) a = a.substring(1);
							if(a.startsWith("-")) {
								add = false;
								a = a.substring(1);
							}
							if(t) {
								if(add) sender.addTag(a);
								else sender.removeTag(a);
							}
							if(p) {
								if(add) sender.addPermission(a);
								else sender.removePermission(a);
							}
						}
					}
					player.sendMessage("[gold]Объект []" + sender.fuid() + ":");
					player.sendMessage("[gold]Разрешения: []" + sender.permissionsString(" "));
					player.sendMessage("[gold]Теги: []" + sender.tagsString(" "));
					TelegramBot.save();
					return;
				}
				
				return;
			}
			if(args[0].equals("start")) {
				if(require(args.length != 3, player, "Должно быть 3 аргумента: start <name> <token>")) return;
				TelegramBot.run(args[1], args[2]);
				player.sendMessage("[gold]Бот запущен! [gray](" + args[1] + " " + args[2] + ")");
				return;
			}
			if(args[0].equals("stop")) {
				TelegramBot.stop();
				player.sendMessage("Бот остановлен!");
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			player.sendMessage("[red]" + e.getMessage());
		}
			
	}
	
	@Override
	public Seq<String> complete(String[] args, Player receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with(
				"add добавить чат", 
				"remove удалить чат", 
				"list список чатов",
				"start запустить бота",
				"stop остановить бота",
				"t теги чата",
				"p разрешения чата",
				"name установить имя пользователя",
				"load загрузить из файла"
		);
		if(args.length == 1 && args[0].equalsIgnoreCase("start")) return Seq.with("@ имя бота в формате @name_bot");
		if(args.length == 2 && args[0].equalsIgnoreCase("t")) return new Seq<>(NotifyTag.values()).map(t -> t.tag);
		if(args.length == 2 && args[0].equalsIgnoreCase("p")) return new Seq<>(NotifyTag.values()).map(t -> t.tag); // TODO
		
		
		return super.complete(args, receiver, type);
	}

}
