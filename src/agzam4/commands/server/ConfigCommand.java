package agzam4.commands.server;

import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.commands.CommandHandler;
import arc.Core;
import arc.struct.Seq;
import mindustry.net.Administration.Config;

public class ConfigCommand extends CommandHandler<Object> {

	{
		parms = "[name] [set/add] [value...]";
		desc = "Конфигурация сервера";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		if(args.length == 0){
			sender.sendMessage("All config values:");
			for(Config c : Config.all){
				sender.sendMessage("[gold]" + c.name + "[lightgray](" + c.description + ")[white]:\n> " + c.get() + "\n");
			}
			return;
		}
		Config c = Config.all.find(conf -> conf.name.equalsIgnoreCase(args[0]));
		if(c != null){
			if(args.length == 1) {
				sender.sendMessage(c.name + " is currently " + c.get());
			}else if(args.length > 2) {
				if(args[2].equals("default")){
					c.set(c.defaultValue);
				}else if(c.isBool()){
					c.set(args[2].equals("on") || args[2].equals("true"));
				}else if(c.isNum()){
					try{
						c.set(Integer.parseInt(args[2]));
					}catch(NumberFormatException e){
						sender.sendMessage("[red]Not a valid number: " + args[2]);
						return;
					}
				}else if(c.isString()) {
					if(args.length > 2) {
						if(args[1].equals("add")) {
							c.set(c.get().toString() + args[2].replace("\\n", "\n"));
						} else if(args[1].equals("set")) {
							c.set(args[2].replace("\\n", "\n"));
						} else {
							sender.sendMessage("[red]Only [gold]add/set");
							return;
						}
					} else {
						sender.sendMessage("[red]Add [gold]add/set [red]attribute");
					}
				}
				sender.sendMessage("[gold]" + c.name + "[gray] set to [white]" + c.get());
				Core.settings.forceSave();
			} else {
				sender.sendMessage("[red]Need more attributes");
			}
		}else{
			sender.sendMessage("[red]Unknown config: '" + args[0] + "'. Run the command with no arguments to get a list of valid configs.");
		}
	}
	
	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Config.all.map(c -> c.key);
		if(args.length > 0) {
			Config c = Config.all.find(conf -> conf.name.equalsIgnoreCase(args[0]));
			if(args.length == 1) {
				return c.isString() ? Seq.with("set", "add") : Seq.with("set");
			}
			if(args.length == 2) {
				if(c.isBool()) return Seq.with("true", "false", "default");
				if(c.defaultValue.equals(c.get())) return Seq.with(c.defaultValue.toString());
				return Seq.with(c.defaultValue.toString(), c.get().toString());
			}
		}
		return super.complete(args, receiver, type);
	}
}
