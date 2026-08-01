package agzam4.api.endpoints;

import agzam4.events.ServerEventsManager;
import agzam4gen.api.dependencies.Auth;
import agzam4gen.api.dependencies.BodyParm;
import agzam4gen.api.dependencies.SessionIp;
import agzam4proc.api.ApiAnnotations.Post;
import agzam4proc.api.ApiAnnotations.Router;
import agzam4proc.api.ApiAnnotations.Type;
import agzam4proc.api.lib.ApiResponse;
import arc.util.Strings;
import mindustry.net.Administration.PlayerInfo;

@Router("/events")
public class ApiEvents {

	@Type
	public static class EventInfo {
		
		public String key;
		public String name;
		public boolean enabled;
		
	}
	
    @Post
    public static EventInfo[] list(@Auth PlayerInfo player) throws ApiResponse {
    	EventInfo[] events = new EventInfo[ServerEventsManager.events.size];
    	for (int i = 0; i < events.length; i++) {
			var info = new EventInfo();
			var event = ServerEventsManager.events.get(i);
			
			info.key = event.name;
			info.name = Strings.format("[@]@", event.bungle("color"), event.bungle("name"));
			info.enabled = ServerEventsManager.targetEvents.contains(event);
			events[i] = info;
		}
    	return events;
    }
	
	
	
}
