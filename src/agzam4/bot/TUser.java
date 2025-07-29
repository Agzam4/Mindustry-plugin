package agzam4.bot;

import arc.util.Nullable;

public class TUser extends TSender {

	public TUser(long id) {
		super(id);
	}
	
	public TUser(String data) {
		super(data);
	}

	public void onMessage(TSender sender) {
		if(sender == this) {
			sender.message("Hello user!");	
		}
		else sender.message("Hello chat!");
	}

	public static @Nullable TUser read(String data) {
		try {
			String[] args = data.split(" ");
			return new TUser(TSender.id(args[0]));
		} catch (Exception e) {
		}
		return null;
	}
	
}
