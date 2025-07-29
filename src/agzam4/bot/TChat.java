package agzam4.bot;

import arc.util.Strings;

public class TChat extends TSender {
	
	public TChat(long id) {
		super(id);
	}

	public TChat(String data) {
		super(data);
	}

	public static TChat read(String line) {
		return new TChat(line);
	}

}
