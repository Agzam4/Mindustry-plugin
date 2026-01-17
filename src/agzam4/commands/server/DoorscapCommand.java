package agzam4.commands.server;

import agzam4.AgzamPlugin;
import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.CommandSender;
import agzam4.commands.CommandHandler;
import agzam4.commands.Server;
import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.BlockDestroyEvent;
import mindustry.game.EventType.BuildSelectEvent;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.net.Administration.ActionFilter;
import mindustry.net.Administration.ActionType;
import mindustry.world.Block;
import mindustry.world.Tile;

public class DoorscapCommand extends CommandHandler<Object> {

	private static IntSet doorsCoordinates = IntSet.with();
	
	private static ActionFilter filter = action -> {
		if(action.type != ActionType.placeBlock) return true;
		if(action.block != Blocks.door && action.block != Blocks.doorLarge) return true;
		return doorsCoordinates.size < Server.doorsCap;
	};

	private static Cons<BlockDestroyEvent> destroy = e -> update(e.tile, true);
	private static Cons<BlockBuildEndEvent> build = e -> update(e.tile, e.breaking);
	private static Cons<BuildSelectEvent> select = e -> {
		if(e.builder != null) {
			var plan = e.builder.buildPlan();
			if(plan != null && !plan.breaking && (plan.block == Blocks.door || plan.block == Blocks.doorLarge) && e.builder.plans != null) {
				e.builder.plans.clear();
				// TODO: units restores buildings
			}
		}
		update(e.tile, e.breaking);
	};
	private static Cons<WorldLoadEndEvent> reset = e -> doorsCoordinates.clear();
	
	{
		parms = "[count]";
		desc = "Устанавливает лимит дверей";
		setup();
	}

	@Override
	public void command(String[] arg, CommandSender sender, Object receiver, ReceiverType type) {
		if(require(arg.length == 0, sender, type.format("doorscap.doors", doorsCoordinates.size, Server.doorsCap))) return;
		try {
			int lastDoorsCup = Server.doorsCap;
			Server.doorsCap = Strings.parseInt(arg[0], -1);
			Core.settings.put(AgzamPlugin.name() + "-doors-cap", Server.doorsCap);
			setup();
			if(Server.doorsCap >= 0) {
				sender.sendMessage(type.format("doorscap.set", lastDoorsCup, Server.doorsCap));
			} else {
				sender.sendMessage(type.format("doorscap.reset"));
			}
		} catch (Exception e) {
			sender.sendMessage(e.getMessage());
		}
	}
	
	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("off", "0", "100", Server.doorsCap + "");
		return super.complete(args, receiver, type);
	}

	private static void update(Tile tile, boolean remove) {
		if(tile == null) return;
		Block b = tile.block();
		if(b == null) return;
		if(remove) doorsCoordinates.remove(tile.pos());
		else if(b == Blocks.door || b == Blocks.doorLarge) doorsCoordinates.add(tile.pos());
	}
	
	private static boolean listen = false;
	
	private static void setup() {
		if(Server.doorsCap >= 0) {
			if(listen) return; // Already added
			// Adding
			Vars.netServer.admins.addActionFilter(filter);
	    	Events.on(BlockDestroyEvent.class, destroy);
			Events.on(BlockBuildEndEvent.class, build);
	    	Events.on(BuildSelectEvent.class, select);
	    	Events.on(WorldLoadEndEvent.class, reset);
	    	listen = true;
	    	return;
		}
		if(!listen) return; // Already removed / not added
		// Removing
		Vars.netServer.admins.actionFilters.remove(filter);
    	Events.remove(BlockDestroyEvent.class, destroy);
		Events.remove(BlockBuildEndEvent.class, build);
    	Events.remove(BuildSelectEvent.class, select);
    	Events.remove(WorldLoadEndEvent.class, reset);
    	listen = false;
	}
	
}
