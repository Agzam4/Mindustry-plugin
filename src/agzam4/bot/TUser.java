package agzam4.bot;

import agzam4.CommandsManager.CommandReceiver;
import arc.util.CommandHandler.ResponseType;
import arc.util.Nullable;

public class TUser extends TSender {

	public TUser(long id) {
		super(id);
	}
	
	public TUser(String data) {
		super(data);
	}

	public void onMessage(TSender sender, String message) {
		if(message.startsWith("/")) {
			var response = Bots.handler.handleMessage(message, new MessageData() {{
				user = TUser.this;
				chat = sender;
			}});
			if(response.type == ResponseType.valid) return;
			if(response.type == ResponseType.noCommand) {
				sender.message("не команда найдена");
			}
			if(response.type == ResponseType.manyArguments) {
				sender.message("Слишком много аргументов");
			}
			if(response.type == ResponseType.fewArguments) {
				sender.message("Слишком мало аргументов");
			}
			if(response.type == ResponseType.unknownCommand) {
				sender.message("Команда не найдена");
			}
		}
		
//		
//		if(sender == this) {
//			sender.message("Hello user!");	
//		}
//		else sender.message("Hello chat!");
	}

	public static @Nullable TUser read(String data) {
		try {
			String[] args = data.split(" ");
			return new TUser(TSender.id(args[0]));
		} catch (Exception e) {
		}
		return null;
	}

	
	public static class MessageData implements CommandReceiver {
		
		public TUser user;
		public TSender chat;
		
		@Override
		public void sendMessage(String message) {
			chat.message(message);
		}

		public void noAccess(String command) {
			chat.message("Нет доступа к " + command);
		}
		
	}
}
