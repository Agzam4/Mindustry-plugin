package agzam4.events;

import arc.util.Nullable;
import mindustry.gen.Call;
import mindustry.gen.Player;

public class EventNet {

	public final ServerEvent event;
	
	public EventNet(ServerEvent event) {
		this.event = event;
	}

	public void message(String type) {
		Call.sendMessage(event.bungle(type));
	}
	
	public void message(String type, Object... args) {
		Call.sendMessage(event.bungle(type, args));
	}

	public void message(@Nullable Player player, String type) {
		if(player == null) return;
		player.sendMessage(event.bungle(type));
	}
	
	public void message(@Nullable Player player, String type, Object... args) {
		if(player == null) return;
		player.sendMessage(event.bungle(type, args));
	}

	public void announce(String string) {
		Call.announce(event.bungle(string));
	}

	public void announce(String type, Object... args) {
		Call.announce(event.bungle(type, args));
	}
	
}
