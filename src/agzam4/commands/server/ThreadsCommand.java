package agzam4.commands.server;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.commands.CommandHandler;
import arc.func.Func;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Structs;

public class ThreadsCommand extends CommandHandler<Object> {

	{
		parms = "[filter]";
		desc = "Отладка потоков";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		StringBuilder message = new StringBuilder("Threads");
		Func<String, Byte> filter = (keyword) -> {
			if(Structs.contains(args, keyword) || Structs.contains(args, "+" + keyword)) return 1;
			if(Structs.contains(args, "-" + keyword)) return -1;
			return 0;
		};
		byte daemon = filter.get("daemon");
		
		Thread.getAllStackTraces().keySet().forEach(t -> {
			if(daemon == 1 & !t.isDaemon()) return;
			if(daemon == -1 & t.isDaemon()) return;
			
			message.append(Strings.format("\n@: (@) @", t.getName(), t.isDaemon() ? "Daemon" : "", t.getState()));
		});
		sender.sendMessage(message.toString());
	}

	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("+daemon только демон-потоки", "-daemon без демон-потоков");
		return super.complete(args, receiver, type);
	}
	
}
