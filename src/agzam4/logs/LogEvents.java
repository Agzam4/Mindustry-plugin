package agzam4.logs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import agzam4.api.auth.SensitiveData;
import agzam4.api.auth.SensitiveData.SensitiveType;
import agzam4.database.DBFields.*;
import agzam4.logs.LogsAnnotations.JsonProp;
import agzam4.mcp.McpUlits;
import agzam4proc.apt.api.ApiAnnotations.Type;
import arc.func.Func;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.gen.Player;

public class LogEvents {

	@SuppressWarnings("unchecked")
	public static final Class<? extends LogEvent>[] events = new Class[] {
			ServerStartLogEvent.class,
			ChatMessageLogEvent.class,
			PlayerCommandLogEvent.class,
			AdminCommandLogEvent.class,
			KickLogEvent.class,
			VotekickLogEvent.class,
			PlayerLeaveLogEvent.class,
			PlayerJoinLogEvent.class,
			GameOverLogEvent.class,
			GameBeginLogEvent.class,
	};
	
	public enum SearchFiledTypes {
		player;
		
		@SuppressWarnings("unchecked")
		Seq<String>[] sql = new Seq[events.length];
		int fields;
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SearchField {
		public SearchFiledTypes value();
	}
	
	
	public static void init() {
		for (var search : SearchFiledTypes.values()) {
			for (int i = 0; i < events.length; i++) {
				Log.info("@-@", search, i);
				Seq<String> fields = Seq.with();
				for (var f : events[i].getDeclaredFields()) {
					SearchField a = f.getDeclaredAnnotation(SearchField.class);
					Log.info("@: @", f.getName(), a);
					if(a == null) continue;
					if(a.value() != search) continue;
					fields.add(f.getName());
					Log.info(f.getName());
					search.fields++;
				}
				search.sql[i] = fields;
			}
		}
	}
	
	
	@Type
	public static class LogEntity {
		

		public @FIELD @AUTOINCREMENT @PRIMARY_KEY Integer id;
		
		/** Id of tag */
		public @FIELD Integer tag;

		public @FIELD String message = "";

		public @FIELD long timestamp;
	
		public long globalId = -1;
		
		
		@Override
		public String toString() {
			return "log-" + globalId;
		}
		
	}
	

	@Type
	public static class LogEvent {
		
		public long timestamp = Time.millis();

		private LogEvent() {}
		
		public String mcpText() {
			return "";
		}

		public String mcpTag() {
			return McpUlits.snakeCase(getClass().getSimpleName().replaceAll("LogEvent", ""));
		}
	}

	@Type
	public static class ServerStartLogEvent extends LogEvent {

		public ServerStartLogEvent() {}
		
	}


	@Type
	public static class ChatMessageLogEvent extends LogEvent {

		@JsonProp
		@SearchField(SearchFiledTypes.player)
		public int player;
		
		@JsonProp
		public String message;

		@SuppressWarnings("unused")
		private ChatMessageLogEvent() {}
		
		public ChatMessageLogEvent(Player sender, String message) {
			this.player = SensitiveData.insertOrGet(sender.uuid(), SensitiveType.uuid);
			this.message = message;
		}
		
		@Override
		public String mcpText() {
			return Strings.format("@: \"@\"", player, McpUlits.escapedStrip(message));
		}
		
		@Override
		public String mcpTag() {
			return "chat";
		}
		
	}

	@Type
	public static class PlayerCommandLogEvent extends LogEvent {

		@JsonProp
		@SearchField(SearchFiledTypes.player)
		public int player;
		
		@JsonProp
		public String command;

		@SuppressWarnings("unused")
		private PlayerCommandLogEvent() {}
		
		public PlayerCommandLogEvent(Player sender, String command) {
			this.player = SensitiveData.insertOrGet(sender.uuid(), SensitiveType.uuid);
			this.command = command;
		}

		@Override
		public String mcpText() {
			return Strings.format("@: \"@\"", player, McpUlits.escapedStrip(command));
		}
	}

	@Type
	public static class AdminCommandLogEvent extends LogEvent {

		@JsonProp
		@SearchField(SearchFiledTypes.player)
		public int player;
		
		@JsonProp
		public String command;

		@SuppressWarnings("unused")
		private AdminCommandLogEvent() {}
		
		public AdminCommandLogEvent(Player sender, String command) {
			this.player = SensitiveData.insertOrGet(sender.uuid(), SensitiveType.uuid);
			this.command = command;
		}

		@Override
		public String mcpText() {
			return Strings.format("@: \"@\"", player, McpUlits.escapedStrip(command));
		}
	}

	@Type
	public static class KickLogEvent extends LogEvent {

		@JsonProp
		@SearchField(SearchFiledTypes.player)
		public int actor, target;
		
		@JsonProp
		public String reason;
		
		@JsonProp
		public long seconds;

		@SuppressWarnings("unused")
		private KickLogEvent() {}
		
		public KickLogEvent(Player actor, Player target, String reason, long seconds) {
			this.actor = SensitiveData.insertOrGet(actor.uuid(), SensitiveType.uuid);
			this.target = SensitiveData.insertOrGet(target.uuid(), SensitiveType.uuid);
			this.reason = reason;
			this.seconds = seconds;
		}

		@Override
		public String mcpText() {
			return Strings.format("@ -> @ \"@\" on @ m", actor, target, McpUlits.escapedStrip(reason), seconds/60);
		}
	}
	

	@Type
	public static class VotekickLogEvent extends LogEvent {

		@JsonProp
		@SearchField(SearchFiledTypes.player)
		public int actor, target;
		
		@JsonProp
		public String reason;

		@SuppressWarnings("unused")
		private VotekickLogEvent() {}
		
		public VotekickLogEvent(Player actor, Player target, String reason) {
			this.actor = SensitiveData.insertOrGet(actor.uuid(), SensitiveType.uuid);
			this.target = SensitiveData.insertOrGet(target.uuid(), SensitiveType.uuid);
			this.reason = reason;
		}

		@Override
		public String mcpText() {
			return Strings.format("@ -> @ \"@\"", actor, target, McpUlits.escapedStrip(reason));
		}
	}


	@Type
	public static class PlayerLeaveLogEvent extends LogEvent {

		@JsonProp
		@SearchField(SearchFiledTypes.player)
		public int player;
		
		@JsonProp
		public int players;

		@SuppressWarnings("unused")
		private PlayerLeaveLogEvent() {}
		
		public PlayerLeaveLogEvent(Player player, int players) {
			this.player = SensitiveData.insertOrGet(player.uuid(), SensitiveType.uuid);
			this.players = players;
		}
		
		@Override
		public String mcpText() {
			return Strings.format("@ leave (@ players)", player, players);
		}
	}


	@Type
	public static class PlayerJoinLogEvent extends LogEvent {

		@JsonProp
		@SearchField(SearchFiledTypes.player)
		public int player;
		
		@JsonProp
		public int players;

		@SuppressWarnings("unused")
		private PlayerJoinLogEvent() {}
		
		public PlayerJoinLogEvent(Player player, int players) {
			this.player = SensitiveData.insertOrGet(player.uuid(), SensitiveType.uuid);
			this.players = players;
		}

		@Override
		public String mcpText() {
			return Strings.format("@ joined (@ players)", player, players);
		}
	}

	@Type
	public static class GameOverLogEvent extends LogEvent {

		@JsonProp
		public String map;
		
		@JsonProp
		public int wave, hightscore;

		@SuppressWarnings("unused")
		private GameOverLogEvent() {}
		
		public GameOverLogEvent(String map, int wave, int hightscore) {
			this.map = map;
			this.wave = wave;
			this.hightscore = hightscore;
		}

		@Override
		public String mcpText() {
			return Strings.format("Map: \"@\", (Wave: @, High Score: @)", McpUlits.escapedStrip(map), wave, hightscore);
		}
	}
	
	@Type
	public static class GameBeginLogEvent extends LogEvent {

		@JsonProp
		public String map;

		@SuppressWarnings("unused")
		private GameBeginLogEvent() {}
		
		public GameBeginLogEvent(String map) {
			this.map = map;
		}
		
		@Override
		public String mcpText() {
			return Strings.format("Map: \"@\"", McpUlits.escapedStrip(map));
		}
		
	}
	
}
