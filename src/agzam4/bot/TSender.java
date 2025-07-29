package agzam4.bot;

import arc.struct.LongMap;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Strings;

public class TSender {

	private static ObjectMap<String, LongMap<TSender>> senders = new ObjectMap<>();
	
	public final ObjectSet<String> tags = new ObjectSet<String>();
	public final long id;
	
	public TSender(long id) {
		this.id = id;
	}
	
	public TSender(String data) {
		String[] args = data.split(" ");
		this.id = TSender.id(args[0]);
		for (int i = 1; i < args.length; i++) {
			addTag(args[i]);
		}
	}
	
	private void addTag(String tag) {
		tags.add(tag);
		senders(tag).put(this.id, this);
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
		return Strings.format("@ @", id, tags.toString(" "));
	}
	
	public static LongMap<TSender> senders(String tag) {
		var list = senders.get(tag);
		if(list == null) {
			list = new LongMap<>();
			senders.put(tag, list);
		}
		return list;
	}
}
