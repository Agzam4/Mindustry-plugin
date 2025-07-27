package agzam4.events;

import java.util.ArrayList;
import java.util.Arrays;

import agzam4.Game;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.func.Cons2;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType.*;
import mindustry.gen.Building;
import mindustry.gen.Player;
import mindustry.world.blocks.logic.MessageBlock.MessageBuild;

import static mindustry.Vars.*;

public class ServerEventsManager {
	
	private ServerEventsManager() {};
	
	public static final Seq<ServerEvent> events = Seq.with();
	public static final Seq<ServerEvent> activeEvents = Seq.with();
	private static Boolf<ServerEvent> runningFilter = e -> e.isRunning();

	public static long eventsTPS = 1_000 / 60;

	public static boolean[] isEventsOn;

	private static boolean isLoaded = false;
	boolean isRunning = false;
	
	public static void init() {
		for (var child : Vars.dataDirectory.child("events").list()) {
			if(!child.extension().equals("jar") && !child.extension().equals("zip")) continue;
			try {
				events.addAll(EventsLoader.load(child));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Events.on(WorldLoadBeginEvent.class, e -> {
			events.each(event -> event.stop());
			EventsBlocks.reset();
			applyMapEvents();
			isLoaded = false;
			world.setGenerating(true);
        });
		
		registerEvent(UnitDestroyEvent.class, 	(se,e) -> se.unitDestroy(e));
		registerEvent(DepositEvent.class, 		(se,e) -> se.deposit(e));
		registerEvent(WithdrawEvent.class, 		(se,e) -> se.withdraw(e));
		registerEvent(TapEvent.class, 			(se,e) -> se.tap(e));
		registerEvent(ConfigEvent.class, 		(se,e) -> se.config(e));
		registerEvent(BlockBuildEndEvent.class, (se,e) -> se.blockBuildEnd(e));
		registerEvent(BlockDestroyEvent.class, 	(se,e) -> se.blockDestroy(e));
	}

	
	private static <T> void registerEvent(Class<T> type, Cons2<ServerEvent, T> listener) {
		Events.on(type, e -> {
			if(!isLoaded) return;
			activeEvents.each(runningFilter, se -> listener.get(se, e));
		});
	}


	private static Seq<MessageBuild> worldMessages = new Seq<>();
	private static boolean firstUpdate = false;
	
	public static void worldLoadEnd(WorldLoadEndEvent e) {
		EventsBlocks.set();

		activeEvents.each(event -> event.prepare());
		activeEvents.each(event -> event.run());
		
		world.setGenerating(false);
		firstUpdate = true;
		isLoaded = true;
		worldMessages.clear();
	}
	
	private static int updates = 0;
	
	public static void update() {
		if(isLoaded) {
			activeEvents.each(e -> e.update());
			if(firstUpdate) {
				firstUpdate = false;

				worldMessages.clear();
				if(eventMap != null) {
					Seq<Building> bs = Vars.state.rules.waveTeam.data().getBuildings(Blocks.worldMessage);
					Log.info("found: @", bs);
					for (int i = 0; i < bs.size; i++) {
						if(bs.get(i) instanceof MessageBuild ms) {
							worldMessages.add(ms);
						}
					}
				}
			}
			EventsBlocks.update();
			updates++;
		}
	}

	public static void playerJoin(PlayerJoin e) {
		if(!isLoaded) return;
		activeEvents.each(runningFilter, se -> se.playerJoin(e));
	}

	

	public static void runEvent(String commandName) {
		ServerEvent event = find(commandName);
		if(event == null) {
			Log.info("Event not found!");
			return;
		}
		if(activeEvents.contains(event)) {
			Log.info("Event already active!");
			return;
		}
		activeEvents.add(event);
		event.announce();
	}
	
	public static void stopEvent(String commandName) {
		ServerEvent event = find(commandName);
		if(event == null) return;
		event.stop();
		activeEvents.remove(event);
	}
	
	public static @Nullable ServerEvent find(String name) {
		return events.find(e -> e.name.equals(name));
	}

	/**
	 * !!! Warning !!!
	 * need /sync every player on fast start
	 *  
	 */
	public static void fastRunEvent(String commandName) {
		ServerEvent event = find(commandName);
		if(event == null) {
			Log.info("Event not found!");
			return;
		}
		if(activeEvents.contains(event)) {
			Log.info("Event already active!");
			return;
		} else {
			event.prepare();
			event.run();
			isLoaded = true;
			activeEvents.add(event);
			Game.sync();
		}
	}

//	public @Nullable ServerEvent getEventByCommandName(String commandName) {
//		for (int i = 0; i < getServerEventsCount(); i++) {
//			if(commandName.equals(getServerEvent(i).getCommandName())) {
//				return getServerEvent(i);
//			}
//		}
//		return null;
//	}
//	
//
//	public @Nullable static ServerEvents getEventEnumByCommandName(String commandName) {
//		for (int i = 0; i < ServerEvents.values().length; i++) {
//			if(commandName.equals(ServerEvents.values()[i].getEvent().getCommandName())) {
//				return ServerEvents.values()[i];
//			}
//		}
//		return null;
//	}

	public static void trigger(Player player, String... args) {
		activeEvents.each(e -> e.trigger(player, args));
	}
	
	
//	boolean[] defaultEvents = new boolean[ServerEvents.values().length];
	private static EventMap eventMap = null;
//	EventMap nextEventMap = null;
	
	public static void setNextMapEvents(EventMap map) {
//		if(map == null) {
//			nextEventMap = null;
//			return;
//		}
//		if(eventMap == null) {
//			for (int i = 0; i < defaultEvents.length; i++) defaultEvents[i] = false;
//			for (int i = 0; i < activeEvents.size(); i++) {
//				int id = activeEvents.get(i).id();
//				if(id == -1) continue;
//				defaultEvents[id] = true;
//			}
//			Log.info("saved events: @", Arrays.toString(defaultEvents));
//		}
//		nextEventMap = map;
	}
	

	private static void applyMapEvents() {
//		if(eventMap != null || nextEventMap != null) {
//			for (int i = 0; i < activeEvents.size(); i++) {
//				ServerEvent event = activeEvents.get(i);
//				if(activeEvents.contains(event)) {
//					event.stop();
//				}
//			}
//			activeEvents.clear();
//			if(nextEventMap == null) {
//				Log.info("loaded events: @", Arrays.toString(defaultEvents));
//				for (int i = 0; i < defaultEvents.length; i++) {
//					if(defaultEvents[i]) {
//						ServerEvent event = ServerEvents.values()[i].event;
//						if(!activeEvents.contains(event)) {
//							activeEvents.add(event);
//							event.runSilently();
//						}
//					}
//				}
//			} else {
//				for (int i = 0; i < ServerEvents.values().length; i++) {
//					ServerEvents s = ServerEvents.values()[i];
//					if(nextEventMap.has(s)) {
//						ServerEvent event = s.event;
//						if(!activeEvents.contains(event)) {
//							activeEvents.add(event);
//							event.runSilently();
//						}
//					}
//				}
//			}
//		}
//		eventMap = nextEventMap;
//		nextEventMap = null;
	}
}
