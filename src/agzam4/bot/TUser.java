package agzam4.bot;

import agzam4.CommandsManager.CommandReceiver;

import java.io.IOException;

import agzam4.Log;
import arc.util.CommandHandler.ResponseType;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonWriter;
import arc.util.Nullable;
import mindustry.gen.Call;

public class TUser extends TSender {

	public String name = "user";
	
	public TUser(long id) {
		super(id);
	}
	
	public TUser(JsonValue json) {
		super(json);
		name = json.getString("name", name);
	}
	
	@Override
	protected void write(JsonWriter writer) throws IOException {
		super.write(writer);
		writer.set("name", name);
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
				return;
			}
			if(response.type == ResponseType.manyArguments) {
				sender.message("Слишком много аргументов");
				return;
			}
			if(response.type == ResponseType.fewArguments) {
				sender.message("Слишком мало аргументов");
				return;
			}
			if(response.type == ResponseType.unknownCommand) {
				sender.message("Команда не найдена");
				return;
			}
			sender.message(":(");
			return;
		}
		if(this == sender && hasPermission("server-say")) {
			Call.sendMessage(message);
			return;
		}
		sender.message("Type /help for more");
		return;
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

		public boolean hasPermissions(String permission) {
			if(!chat.hasPermission(permission)) return false;
			if(user.hasOnlyChatPermission(permission)) {
				Log.info("Checking [blue]only-chat @[] @ ([gray]@[] != [gray]@[])", permission, user != chat, user.uid(), chat.uid());
				return user != chat;
			}
			return user.hasPermission(permission);
		}
		
	}
	
}
