package agzam4.bot;

import arc.struct.LongMap;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Strings;

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
	
	public TSender(String data) {
		String[] args = data.split(" ");
		this.id = TSender.id(args[0]);
		if(args.length <= 1) return;
		for (var t : args[1].split(",")) {
			addTag(t);
		}
		if(args.length <= 2) return;
		for (var p : args[2].split(",")) {
			addPermission(p);
		}
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
	
	public String uid() {
		return Long.toUnsignedString(id, Character.MAX_RADIX);
	}
	
	public static long id(String uid) {
		return Long.parseUnsignedLong(uid, Character.MAX_RADIX);
	}
	
	public final void message(String message) {
		TelegramBot.sendTo(id, message);
	}
	
	@Override
	public String toString() {
		return Strings.format("@ @ @", uid(), tags.toString(","), permissions.toString(","));
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
}
