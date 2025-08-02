package agzam4.events;

import arc.Events;
import arc.struct.Seq;
import mindustry.game.EventType.ConnectPacketEvent;
import mindustry.game.MapObjectives.TextMarker;
import mindustry.gen.Call;
import mindustry.logic.LMarkerControl;
import mindustry.net.NetConnection;

public class EventsLabels {

	private static Seq<NetMarker> markers = new Seq<>();
	
	public static void init() {
		Events.on(ConnectPacketEvent.class, e -> markers.each(m -> m.sync(e.connection)));
	}

	public static void reset() {
		markers.clear();
	}
	
	public static class NetMarker {

		private static int ids = 0;
		public final int id = ids++;
		
		private TextMarker marker;
		private boolean visible = false;
		
		public NetMarker(String text, float x, float y) {
			marker = new TextMarker();
			marker.pos.x = x;
			marker.pos.y = y;
			marker.text = "";
			visible = false;
//			text(text);
			markers.add(this);
		}
		
		private void sync(NetConnection con) {
//			Call.createMarker(con, id, marker);
//			Call.updateMarker(con, id, LMarkerControl.labelFlags, visible ? 1d : 0d, 1d, 0d);
			if(visible) Call.createMarker(con, id, marker);
			else Call.removeMarker(con, id);
		}

		public void text(String text) {
			if(text == null) text = "";
//			text = id + "#" + text; DEBUG
			if(marker.text.equals(text)) return;
			marker.text = text;
			setVisible(text == null || !text.isEmpty());
//			Vars.state.markers.add(id, marker);
//			if(visible) {
//				Log.info("[ ] update: @ (@) @", id, marker.text, marker.pos);
				Call.updateMarkerText(id, LMarkerControl.flushText, false, marker.text);
//			}
		}

		public String text() {
			return marker.text;
		}
		
		public boolean setVisible(boolean visible) {
			if(this.visible != visible) {
				this.visible = visible;
				if(visible) {
					Call.createMarker(id, marker);
					return true;
				} else {
					Call.removeMarker(id);
					return true;
				}
			}
			return false;
		}
	}

	
}
