package example.events;

import java.util.ArrayList;
import arc.Events;
import arc.util.Log;
import mindustry.game.EventType.*;
import mindustry.gen.Player;
import static mindustry.Vars.*;

public class ServerEventsManager {
	
	public static enum ServerEvents {
		newYear(new NewYearEvent()),
		spaceDanger(new SpaceDangerEvent()),
		livingWorld(new LivingWorldEvent()),
		luckyPlace(new LuckyPlaceEvent()),
		abilityEvent(new AbilityEvent()),
		musicEvent(new MusicEvent());
		
		ServerEvent event;
		private ServerEvents(ServerEvent event) {
			this.event = event;
		}

		@Override
		public String toString() {
			if(event == null) return super.toString();
			return event.getName();
		}
		
		public ServerEvent getEvent() {
			return event;
		}

		public String getName() {
			if(event == null) return null;
			return event.getName();
		}
		
		public String getCommandName() {
			if(event == null) return null;
			return event.getCommandName();
		}
	}

	public static int getServerEventsCount() {
		return ServerEvents.values().length;
	}

	public static ServerEvent getServerEvent(int id) {
		if(id < 0) return null;
		if(id >= getServerEventsCount()) return null;
		return ServerEvents.values()[id].getEvent();
	}
	
	public static ServerEvents[] getServerEvents() {
		return ServerEvents.values();
	}

	public static long eventsTPS = 1_000 / 60;
	public boolean[] isEventsOn;

	private ArrayList<ServerEvent> activeEvents;
    
	public ServerEventsManager() {
		activeEvents = new ArrayList<>();
	}
	
	public void init() {
		Events.on(WorldLoadBeginEvent.class, e -> {
			isLoaded = false;
			world.setGenerating(true);
			for (int i = 0; i < activeEvents.size(); i++) {
				activeEvents.get(i).isGenerated = false;
			}
        });
		
		Events.on(BlockBuildEndEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(activeEvents.get(i).isGenerated) activeEvents.get(i).blockBuildEnd(e);
				}
			}
		});

		Events.on(UnitDestroyEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(activeEvents.get(i).isGenerated) activeEvents.get(i).unitDestroy(e);
				}
			}
		});
		Events.on(DepositEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(activeEvents.get(i).isGenerated) activeEvents.get(i).deposit(e);
				}
			}
		});
		Events.on(WithdrawEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(activeEvents.get(i).isGenerated) activeEvents.get(i).withdraw(e);
				}
			}
		});
		Events.on(TapEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(activeEvents.get(i).isGenerated) activeEvents.get(i).tap(e);
				}
			}
		});
		Events.on(ConfigEvent.class, e -> {
			if(isLoaded) {
				for (int i = 0; i < activeEvents.size(); i++) {
					if(activeEvents.get(i).isGenerated) activeEvents.get(i).config(e);
				}
			}
		});
		
		
		for (int i = 0; i < ServerEvents.values().length; i++) {
			ServerEvents.values()[i].event.init();
		}
	}
	
	public void worldLoadEnd(WorldLoadEndEvent e) {
		for (int i = 0; i < activeEvents.size(); i++) {
			activeEvents.get(i).generateWorld();
			activeEvents.get(i).isGenerated = true;
		}
		world.setGenerating(false);
		isLoaded = true;
	}
	
	public void update() {
		if(isLoaded) {
			for (int i = 0; i < activeEvents.size(); i++) {
				if(activeEvents.get(i).isGenerated) activeEvents.get(i).update();
			}
		}
	}

	public void playerJoin(PlayerJoin e) {
		if(isLoaded) {
			for (int i = 0; i < activeEvents.size(); i++) {
				if(activeEvents.get(i).isGenerated) activeEvents.get(i).playerJoin(e);
			}
		}
	}

	public boolean isLoaded = false;
	boolean isRunning = false;
	

	public void runEvent(String commandName) {
		ServerEvent event = getEventByCommandName(commandName);
		if(event == null) {
			Log.info("Event not found!");
			return;
		}
		Log.info(commandName + ": " + event.getName());
		
		if(activeEvents.contains(event)) {
			Log.info("Event already active!");
			return;
		} else {
			event.run();
			activeEvents.add(event);
		}
	}
	
	public void stopEvent(String commandName) {
		ServerEvent event = getEventByCommandName(commandName);
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

	/**
	 * !!! Warning !!!
	 * need /sync every player on fast start
	 *  
	 */
	public void fastRunEvent(String commandName) {
		ServerEvent event = getEventByCommandName(commandName);
		if(event == null) {
			Log.info("Event not found!");
			return;
		}
		if(activeEvents.contains(event)) {
			Log.info("Event already active!");
			return;
		} else {
			event.run();
			event.generateWorld();
			event.isGenerated = true;
			isLoaded = true;
			activeEvents.add(event);
		}
	}
	
	private ServerEvent getEventByCommandName(String commandName) {
		for (int i = 0; i < getServerEventsCount(); i++) {
			if(commandName.equals(getServerEvent(i).getCommandName())) {
				return getServerEvent(i);
			}
		}
		return null;
	}

	public void trigger(Player player, String... args) {
		for (int i = 0; i < activeEvents.size(); i++) {
			activeEvents.get(i).trigger(player, args);
		}
	}
}
