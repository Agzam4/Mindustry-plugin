package agzam4.bot;

import java.awt.image.BufferedImage;
import java.io.IOException;
import arc.struct.LongMap;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonWriter;

public class TSender {

	private static ObjectMap<String, LongMap<TSender>> senders = new ObjectMap<>();

	private final ObjectSet<String> tags = new ObjectSet<String>();
	
	/**
	 * tags format:
	 * <code>
	 * [!]tag<br>
	 * "!" - super tag<br>
	 * </code>
	 * 
	 * permissions format:
	 * <code>
	 * [$]name<br>
	 * "$" - allow only in chats<br>
	 * </code>
	 * 
	 */
	private final ObjectSet<String> permissions = new ObjectSet<String>();
	
	public final long id;
	
	public TSender(long id) {
		this.id = id;
		addPermission("this");
		addPermission("help");
	}

	public TSender(JsonValue json) {
		this.id = TSender.id(json.getString("id"));
		if(json.has("permissions")) {
			for (var p : json.get("permissions")) {
				addPermission(p.asString());
			}
		}
		if(json.has("tags")) {
			for (var p : json.get("tags")) {
				addTag(p.asString());
			}
		}
	}
	
	protected void write(JsonWriter writer) throws IOException {
		writer.set("id", uid());
		// tags
		writer.array("tags");
		for (var t : tags) writer.value(t);
		writer.pop();
//		// permissions
		writer.array("permissions");
		for (var p : permissions) writer.value(p);
		writer.pop();
	}

	public void addTag(String tag) {
		tags.add(tag);
		senders(tag).put(this.id, this);
	}
	
	public void removeTag(String tag) {
		tags.remove(tag);
		senders(tag).remove(id);
	}

	public void addPermission(String permission) {
		permissions.add(permission);
	}

	public void removePermission(String permission) {
		permissions.remove(permission);
	}

//	public boolean applyPermission(boolean value, String permission) {
//		if(hasPermission("|" + permission)) return true;
//		return value & hasPermission(permission);
//	}

	public boolean hasPermission(String permission) {
		return permissions.contains(permission) || permissions.contains("all");
	}
	
	public boolean hasOnlyChatPermission(String permission) {
		return permissions.contains("$" + permission) || (permissions.contains("$all") && !permissions.contains(permission));
	}
	
	public final String uid() {
		return Long.toUnsignedString(id, Character.MAX_RADIX);
	}
	
	public static long id(String uid) {
		return Long.parseUnsignedLong(uid, Character.MAX_RADIX);
	}

	public void message(String message) {
		TelegramBot.sendTo(id, message);
	}

	public void message(BufferedImage image) {
		TelegramBot.sendMessagePhoto(id, image);
	}
	
	public static LongMap<TSender> senders(String tag) {
		var list = senders.get(tag);
		if(list == null) {
			list = new LongMap<>();
			senders.put(tag, list);
		}
		return list;
	}

	public String permissionsString(String separator) {
		return permissions.toString(separator);
	}

	public String tagsString(String separator) {
		return tags.toString(separator);
	}

	public String fuid() {
		return uid();
	}

}
