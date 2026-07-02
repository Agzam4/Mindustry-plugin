package agzam4.api.endpoints;

import com.sun.net.httpserver.HttpExchange;

import agzam4.logs.LogEvents.LogEntity;
import agzam4.logs.Logs;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import agzam4proc.api.lib.SseSource;
import arc.util.Log;
import arc.util.Strings;
import mindustry.ui.fragments.ChatFragment;

@Router("/logs")
public class ApiLogs {
	
	/**
	 * TODO: access check (Dependency that locks it for non-autharisated non-admin users)
	 * TODO: Search in LogEntity.message support
	 */

	@Sse
	public static LogSseSource logsStream = new LogSseSource();

	public static class LogSseSource extends SseSource<LogEntity> {
		
		@SseHandler
		public static String processor(LogEntity entity, @BodyParm boolean protect) { 
			return Logs.entityJson(entity, protect);
		}

	};

	@Post
	public static long lastId(HttpExchange e) {
		return Logs.lastId();
	}
	
	@Post 
	public static LogEntity[] search(@BodyParm int id, @BodyParm int limit, @BodyParm long t1, @BodyParm long t2, @BodyParm int[] tags, @BodyParm String query) {
		return Logs.logsBy(id, limit, t1, t2, tags);
	}

}
