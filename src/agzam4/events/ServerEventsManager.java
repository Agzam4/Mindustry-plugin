package agzam4.events;

import java.util.ArrayList;
import java.util.Arrays;

import arc.Events;
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
	
	public static final Seq<ServerEvent> events = new Seq<ServerEvent>();
	
//	public static enum ServerEvents {
//		newYear(new NewYearEvent()),
//		spaceDanger(new SpaceDangerEvent()),
//		livingWorld(new LivingWorldEvent()),
//		luckyPlace(new LuckyPlaceEvent()),
//		abilityEvent(new AbilityEvent()),
//		musicEvent(new MusicEvent());
//		;
		
//		ServerEvent event;
		
//		private ServerEvents(ServerEvent event) {
//			this.event = event;
//			event.type = this;
//		}
//
//		@Override
//		public String toString() {
//			if(event == null) return super.toString();
//			return event.getTagName();
//		}
//		
//		public ServerEvent getEvent() {
//			return event;
//		}
//
//		public String getCommandName() {
//			if(event == null) return null;
//			return event.getCommandName();
//		}
//
//		public String tag() {
//			if(event == null) return "none";
//			return event.getTagName();
//		}
//
//		public String color() {
//			if(event == null) return "white";
//			return event.color;
//		}
//	}

//	public static ServerEvent getServerEvent(int id) {
//		if(id < 0) return null;
//		if(id >= getServerEventsCount()) return null;
//		return ServerEvents.values()[id].getEvent();
//	}
//	
//	public static ServerEvents[] getServerEvents() {
//		return ServerEvents.values();
//	}

	public static long eventsTPS = 1_000 / 60;
	public boolean[] isEventsOn;

	private ArrayList<ServerEvent> activeEvents;
    
	public ServerEventsManager() {
		activeEvents = new ArrayList<>();
	}
	
	private boolean isGenerated = false;
	public void init() {
		Events.on(WorldLoadBeginEvent.class, e -> {
			EventsBlocks.reset();
			applyMapEvents();
			isLoaded = false;
			world.setGenerating(true);
			isGenerated = false;
//			for (int i = 0; i < activeEvents.size(); i++) {
//				activeEvents.get(i).
//			}
        });
		
		Events.on(BlockBuildEndEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(isGenerated) activeEvents.get(i).blockBuildEnd(e);
				}
			}
		});

		Events.on(UnitDestroyEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(isGenerated) activeEvents.get(i).unitDestroy(e);
				}
			}
		});
		Events.on(DepositEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(isGenerated) activeEvents.get(i).deposit(e);
				}
			}
		});
		Events.on(WithdrawEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(isGenerated) activeEvents.get(i).withdraw(e);
				}
			}
		});
		Events.on(TapEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(isGenerated) activeEvents.get(i).tap(e);
				}
			}
		});
		Events.on(ConfigEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(isGenerated) activeEvents.get(i).config(e);
				}
			}
		});
    	Events.on(BlockDestroyEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(isGenerated) activeEvents.get(i).blockDestroy(e);
				}
			}
    	});
		
//		for (int i = 0; i < ServerEvents.values().length; i++) {
//			ServerEvents.values()[i].event.init();
//		}
	}

	
	private Seq<MessageBuild> worldMessages = new Seq<>();
	boolean firstUpdate = false;
	
	public void worldLoadEnd(WorldLoadEndEvent e) {
		EventsBlocks.set();
		for (int i = 0; i < activeEvents.size(); i++) {
			activeEvents.get(i).prepare();
		}
		world.setGenerating(false);
		firstUpdate = true;
		isLoaded = true;
		worldMessages.clear();
	}
	
	int updates = 0;
	
	public void update() {
		if(isLoaded) {
			for (int i = 0; i < activeEvents.size(); i++) {
				if(isGenerated) activeEvents.get(i).update();
			}
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
//			if(updates >= 60) {
//				updates = 0;
//				for (int i = 0; i < worldMessages.size; i++) {
//					String config = worldMessages.get(i).config();
//					if(config.startsWith("/")) {
//						config = config.substring(1);
//						String name = "";
//						for (int j = 0; j < activeEvents.size(); j++) {
//							if(!isGenerated) continue;
//							name = activeEvents.get(j).type.getCommandName();
//							if(config.length() < name.length() + 2) continue;
//							if(!config.startsWith(name)) continue;
//							activeEvents.get(j).commandBlock(config.substring(name.length()+1));
//							break;
//						}
//						worldMessages.get(i).handleString("");
//					}
//				}
//			}
		}
	}

	public void playerJoin(PlayerJoin e) {
		if(isLoaded) {
			for (int i = 0; i < activeEvents.size(); i++) {
				if(isGenerated) activeEvents.get(i).playerJoin(e);
			}
		}
	}

	public boolean isLoaded = false;
	boolean isRunning = false;
	

	public void runEvent(String commandName) {
		ServerEvent event = find(commandName);
		if(event == null) {
			Log.info("Event not found!");
			return;
		}
		if(activeEvents.contains(event)) {
			Log.info("Event already active!");
			return;
		} else {
			event.run();
			activeEvents.add(event);
		}
	}
	
	public void stopEvent(String commandName) {
		ServerEvent event = find(commandName);
		if(event == null) {
			Log.info("Event not found!");
			return;
		}
		if(activeEvents.contains(event)) {
			event.stop();
			activeEvents.remove(event);
		} else {
			Log.info("Event not active!");
			return;
		}
	}
	
	public static @Nullable ServerEvent find(String name) {
		return events.find(e -> e.name.equals(name));
	}

	/**
	 * !!! Warning !!!
	 * need /sync every player on fast start
	 *  
	 */
	public void fastRunEvent(String commandName) {
		ServerEvent event = find(commandName);
		if(event == null) {
			Log.info("Event not found!");
			return;
		}
		if(activeEvents.contains(event)) {
			Log.info("Event already active!");
			return;
		} else {
			event.run();
			event.prepare();
			isGenerated = true;
			isLoaded = true;
			activeEvents.add(event);
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

	public void trigger(Player player, String... args) {
		for (int i = 0; i < activeEvents.size(); i++) {
			activeEvents.get(i).trigger(player, args);
		}
	}
	
	
//	boolean[] defaultEvents = new boolean[ServerEvents.values().length];
	EventMap eventMap = null;
//	EventMap nextEventMap = null;
	
	public void setNextMapEvents(EventMap map) {
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
	

	private void applyMapEvents() {
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
