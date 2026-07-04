package agzam4.logs;

import agzam4.api.auth.SensitiveData;
import agzam4.api.auth.SensitiveData.SensitiveType;
import agzam4.database.DBFields.*;
import agzam4.logs.LogsAnnotations.JsonProp;
import agzam4.logs.LogsAnnotations.Sensitive;
import agzam4.logs.LogsAnnotations.Sensitive.SensitiveProtector;
import agzam4proc.api.ApiAnnotations.Type;
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
		
	}

	@Type
	public static class ServerStartLogEvent extends LogEvent {

		public ServerStartLogEvent() {}
		
	}


	@Type
	public static class ChatMessageLogEvent extends LogEvent {

		@JsonProp
		public int player;
		
		@JsonProp
		public String message;

		public ChatMessageLogEvent(Player sender, String message) {
			this.player = SensitiveData.insertOrGet(sender.uuid(), SensitiveType.uuid);
			this.message = message;
		}
		
	}

	@Type
	public static class PlayerCommandLogEvent extends LogEvent {

		@JsonProp
		public int player;
		
		@JsonProp
		public String command;

		public PlayerCommandLogEvent(Player sender, String command) {
			this.player = SensitiveData.insertOrGet(sender.uuid(), SensitiveType.uuid);
			this.command = command;
		}
		
	}

	@Type
	public static class AdminCommandLogEvent extends LogEvent {

		@JsonProp
		public int player;
		
		@JsonProp
		public String command;

		public AdminCommandLogEvent(Player sender, String command) {
			this.player = SensitiveData.insertOrGet(sender.uuid(), SensitiveType.uuid);
			this.command = command;
		}
		
	}

	@Type
	public static class KickLogEvent extends LogEvent {

		@JsonProp
		public int actor, target;
		
		@JsonProp
		public String reason;
		
		@JsonProp
		public long seconds;

		public KickLogEvent(Player actor, Player target, String reason, long seconds) {
			this.actor = SensitiveData.insertOrGet(actor.uuid(), SensitiveType.uuid);
			this.target = SensitiveData.insertOrGet(target.uuid(), SensitiveType.uuid);
			this.reason = reason;
			this.seconds = seconds;
		}
		
	}
	

	@Type
	public static class VotekickLogEvent extends LogEvent {

		@JsonProp
		public int actor, target;
		
		@JsonProp
		public String reason;
		
		public VotekickLogEvent(Player actor, Player target, String reason) {
			this.actor = SensitiveData.insertOrGet(actor.uuid(), SensitiveType.uuid);
			this.target = SensitiveData.insertOrGet(target.uuid(), SensitiveType.uuid);
			this.reason = reason;
		}
		
	}


	@Type
	public static class PlayerLeaveLogEvent extends LogEvent {

		@JsonProp
		public int player;
		
		@JsonProp
		public int players;
		
		public PlayerLeaveLogEvent(Player player, int players) {
			this.player = SensitiveData.insertOrGet(player.uuid(), SensitiveType.uuid);
			this.players = players;
		}
		
	}


	@Type
	public static class PlayerJoinLogEvent extends LogEvent {

		@JsonProp
		public int player;
		
		@JsonProp
		public int players;
		
		public PlayerJoinLogEvent(Player player, int players) {
			this.player = SensitiveData.insertOrGet(player.uuid(), SensitiveType.uuid);
			this.players = players;
		}
		
	}

	@Type
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
	
	@Type
	public static class GameBeginLogEvent extends LogEvent {

		@JsonProp
		public String map;
		
		public GameBeginLogEvent(String map) {
			this.map = map;
		}
		
	}
	
}
