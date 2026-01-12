package agzam4.commands;

import agzam4.CommandsManager.ResultSender;
import agzam4.CommandsManager.ReceiverType;
import arc.struct.Seq;
import arc.util.Nullable;

public abstract class CommandHandler<T> {

	public String text = name(), parms, desc;
	
	private String name() {
		String name = getClass().getSimpleName().toLowerCase();
		if(name.endsWith("command")) name = name.substring(0, name.length() - "command".length());
		return name;
	}
	
	public abstract void command(String[] args, ResultSender sender, T receiver, ReceiverType type);

	public @Nullable Seq<String> complete(String[] args, T receiver, ReceiverType type) {
		return null;
	}
	
	public static boolean require(boolean b, ResultSender receiver, String string) {
		if(b) receiver.sendMessage(string);
		return b;
	}
}
