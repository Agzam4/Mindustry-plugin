package agzam4.bot;

import arc.util.CommandHandler;
import arc.util.Strings;

public class Bots {

	public static CommandHandler handler = new CommandHandler("/");
	
	public static enum NotifyTag {
		
		chatMessage;
		
		
		private final String tag;
		
		private NotifyTag() {
			tag = Strings.camelToKebab(name());
		}
		
		
	}

	public static void notify(NotifyTag tag, String message) {
		notify(tag.tag, message);
	}
	
	public static void notify(String tag, String message) {
		TSender.senders(tag).eachValue(s -> {
			s.message(message);
		});
	}
	
}
