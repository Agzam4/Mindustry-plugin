package agzam4.commands.server;

import java.util.Objects;

import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.commands.CommandHandler;
import agzam4proc.lib.PConfig;
import arc.Core;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.net.Administration.Config;

public class ConfigCommand extends CommandHandler<Object> {

	{
		parms = "[name] [set/add] [value...]";
		desc = "Конфигурация сервера";
		new Config("playerlimit", "limit players on server", 30);
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
		
		// Plugin configs
		Config mc = Config.all.find(conf -> conf.name.equalsIgnoreCase(args[0]));
		if(mc == null) {
			PConfig c = PConfig.all.find(conf -> conf.key.equalsIgnoreCase(args[0]));
			if(require(c == null, sender, "Config not found")) return;
			if(require(args.length < 2, sender, Objects.toString(c.get()))) return;
			
			Object old = c.get();
			if(args[2].equals("default")) {
				c.set(Objects.toString(c.defaultValue));
			} else if(c.isBool()) {
				Log.info("set bool");
				c.set(Objects.toString(args[2].equals("on") || args[2].equals("true")));
			} else if(c.isString()) {
				if(args.length > 2) {
					if(args[1].equals("add")) {
						Log.info("set string");
						c.set(c.get().toString() + args[2].replace("\\n", "\n"));
					} else if(args[1].equals("set")) {
						Log.info("set string");
						c.set(args[2].replace("\\n", "\n"));
					} else {
						sender.sendMessage("[red]Only [gold]add/set");
						return;
					}
				} else {
					sender.sendMessage("[red]Add [gold]add/set [red]attribute");
				}
			} else {
				Log.info("set default");
				c.set(args[2]);
			}
			sender.sendMessage(Strings.format("[gold]@: []@[gray] -> []@", c.key, old, c.get()));
			return;
		}
		
		// Mindustry configs
		if(args.length == 1) {
			sender.sendMessage(mc.name + " is currently " + mc.get());
		}else if(args.length > 2) {
			if(args[2].equals("default")){
				mc.set(mc.defaultValue);
			}else if(mc.isBool()){
				mc.set(args[2].equals("on") || args[2].equals("true"));
			}else if(mc.isNum()){
				try{
					mc.set(Integer.parseInt(args[2]));
				}catch(NumberFormatException e){
					sender.sendMessage("[red]Not a valid number: " + args[2]);
					return;
				}
			}else if(mc.isString()) {
				if(args.length > 2) {
					if(args[1].equals("add")) {
						mc.set(mc.get().toString() + args[2].replace("\\n", "\n"));
					} else if(args[1].equals("set")) {
						mc.set(args[2].replace("\\n", "\n"));
					} else {
						sender.sendMessage("[red]Only [gold]add/set");
						return;
					}
				} else {
					sender.sendMessage("[red]Add [gold]add/set [red]attribute");
				}
			}
			sender.sendMessage("[gold]" + mc.name + "[gray] set to [white]" + mc.get());
			Core.settings.forceSave();
		} else {
			sender.sendMessage("[red]Need more attributes");
		}
		return;
	}
	
	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Config.all.map(c -> c.key).addAll(PConfig.all.map(c -> c.key));
		if(args.length > 0) {
			Config mc = Config.all.find(conf -> conf.name.equalsIgnoreCase(args[0]));
			if(mc != null) {
				if(args.length == 1) {
					return mc.isString() ? Seq.with("set", "add") : Seq.with("set");
				}
				if(args.length == 2) {
					if(mc.isBool()) return Seq.with("true", "false", "default");
					if(mc.defaultValue.equals(mc.get())) return Seq.with(mc.defaultValue.toString());
					return Seq.with(mc.defaultValue.toString(), mc.get().toString());
				}
				return null;
			}
			PConfig c = PConfig.all.find(conf -> conf.key.equalsIgnoreCase(args[0]));
			if(c == null) return null;
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
