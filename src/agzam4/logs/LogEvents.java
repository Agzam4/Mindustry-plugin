package agzam4.logs;

import agzam4.database.DBFields.*;
import agzam4.logs.LogsAnnotations.JsonProp;
import agzam4.logs.LogsAnnotations.Sensitive;
import agzam4.logs.LogsAnnotations.Sensitive.SensitiveProtector;
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
	
	public static class LogEntity {
		

		public @FIELD @AUTOINCREMENT @PRIMARY_KEY Integer id;
		
//		public @FIELD Long uuid;
		
		/**
		 * Id of tag
		 */
		public @FIELD Integer tag;
		
		/**
		 * Is record has IP/UUID/etc
		 */
//		public @FIELD Boolean sensitive;
		

		public @FIELD String message = "";

		public @FIELD long timestamp;
	
		public int logId = -1;
	}
	

	public static class LogEvent {
		
		public long timestamp = Time.millis();
		
	}

	public static class ServerStartLogEvent extends LogEvent {

		public ServerStartLogEvent() {}
		
	}


	public static class ChatMessageLogEvent extends LogEvent {

		@JsonProp
		@Sensitive(SensitiveProtector.uuid)
		public String player;
		
		@JsonProp
		public String message;

		public ChatMessageLogEvent(Player sender, String message) {
			this.player = sender.uuid();
			this.message = message;
		}
		
	}

	public static class PlayerCommandLogEvent extends LogEvent {

		@JsonProp
		@Sensitive(SensitiveProtector.uuid)
		public String player;
		
		@JsonProp
		public String command;

		public PlayerCommandLogEvent(Player sender, String command) {
			this.player = sender.uuid();
			this.command = command;
		}
		
	}

	public static class AdminCommandLogEvent extends LogEvent {

		@JsonProp
		@Sensitive(SensitiveProtector.uuid)
		public String player;
		
		@JsonProp
		public String command;

		public AdminCommandLogEvent(Player sender, String command) {
			this.player = sender.uuid();
			this.command = command;
		}
		
	}

	public static class KickLogEvent extends LogEvent {

		@JsonProp
		@Sensitive(SensitiveProtector.uuid)
		public String actor, target;
		
		@JsonProp
		public String reason;
		
		@JsonProp
		public long seconds;

		public KickLogEvent(Player actor, Player target, String reason, long seconds) {
			this.actor = actor.uuid();
			this.target = target.uuid();
			this.reason = reason;
			this.seconds = seconds;
		}
		
	}
	

	public static class VotekickLogEvent extends LogEvent {

		@JsonProp
		@Sensitive(SensitiveProtector.uuid)
		public String actor, target;
		
		@JsonProp
		public String reason;
		
		public VotekickLogEvent(Player actor, Player target, String reason) {
			this.actor = actor.uuid();
			this.target = target.uuid();
			this.reason = reason;
		}
		
	}


	public static class PlayerLeaveLogEvent extends LogEvent {

		@JsonProp
		@Sensitive(SensitiveProtector.uuid)
		public String player;
		
		@JsonProp
		public int players;
		
		public PlayerLeaveLogEvent(Player player, int players) {
			this.player = player.uuid();
			this.players = players;
		}
		
	}


	public static class PlayerJoinLogEvent extends LogEvent {

		@JsonProp
		@Sensitive(SensitiveProtector.uuid)
		public String player;
		
		@JsonProp
		public int players;
		
		public PlayerJoinLogEvent(Player player, int players) {
			this.player = player.uuid();
			this.players = players;
		}
		
	}

	public static class GameOverLogEvent extends LogEvent {

		@JsonProp
		public String map;
		
		@JsonProp
		public int wave, hightscore;
		
		public GameOverLogEvent(String map, int wave, int hightscore) {
			this.map = map;
			this.wave = wave;
			this.hightscore = hightscore;
		}
		
	}
	public static class GameBeginLogEvent extends LogEvent {

		@JsonProp
		public String map;
		
		public GameBeginLogEvent(String map) {
			this.map = map;
		}
		
	}
	
}
