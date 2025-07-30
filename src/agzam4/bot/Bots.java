package agzam4.bot;

import arc.util.CommandHandler;
import arc.util.Strings;

public class Bots {

	public static CommandHandler handler = new CommandHandler("/");
	
	public static enum NotifyTag {

		event,
		achievement,
		round,
		votekick,
		playerCommand,
		adminCommand,
		playerConnection,
		chatMessage,
		serverInfo;
		
		
		private final String tag;
		
		private NotifyTag() {
			tag = Strings.camelToKebab(name());
		}
		
		
	}

	

//	@Deprecated
	public static void notify(NotifyTag tag, String message) {
		notify(tag.tag, message);
	}

	/**
	 * 
	 * @param tag
	 * @param message
	 * @param superMessage - use for sending important information like uuid and ip
	 */
	public static void notify(NotifyTag tag, String message, String superMessage) {
		if(message != null) notify(tag.tag, message);
		if(superMessage != null) notify("!" + tag.tag, superMessage);
	}
	
	public static void notify(String tag, String message) {
		TSender.senders(tag).eachValue(s -> {
			s.message(message);
		});
	}
	
}
