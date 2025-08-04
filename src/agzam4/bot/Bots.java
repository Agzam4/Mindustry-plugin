package agzam4.bot;

import java.awt.image.BufferedImage;

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


	public static void notify(NotifyTag tag, BufferedImage image) {
		notify(tag.tag, image);
		notify("!" + tag.tag, image);
		notify("all", image);
		notify("!all", image);
	}

	public static void notify(NotifyTag tag, String message) {
		notify(tag.tag, message);
		notify("!" + tag.tag, message);
		notify("all", message);
		notify("!all", message);
	}

	/**
	 * 
	 * @param tag
	 * @param message
	 * @param superMessage - use for sending important information like uuid and ip
	 */
	public static void notify(NotifyTag tag, String message, String superMessage) {
		if(message != null) {
			notify(tag.tag, message);
			notify("all", message);
		}
		if(superMessage != null) {
			notify("!" + tag.tag, superMessage);
			notify("!all", superMessage);
		}
	}

	private static void notify(String tag, String message) {
		TSender.senders(tag).eachValue(s -> s.message(message));
	}

	private static void notify(String tag, BufferedImage image) {
		TSender.senders(tag).eachValue(s -> s.message(image));
	}
	
}
