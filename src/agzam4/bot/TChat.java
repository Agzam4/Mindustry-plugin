package agzam4.bot;

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
