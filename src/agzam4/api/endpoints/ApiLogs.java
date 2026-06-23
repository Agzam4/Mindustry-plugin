package agzam4.api.endpoints;

import agzam4.logs.LogEvents.LogEntity;
import agzam4.logs.Logs;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import agzam4proc.api.lib.SseSource;
import arc.util.Strings;

@Router("/logs")
public class ApiLogs {

	@Sse
	public static LogSseSource logsStream = new LogSseSource();

	public static class LogSseSource extends SseSource<LogEntity> {
		
		@SseHandler
		public static String processor(LogEntity entity, @BodyParm boolean protect) { 
			return Logs.entityJson(entity, protect);
		}

	};
	
	@Post
	public static String timerange(@BodyParm boolean protect, @BodyParm long from, @BodyParm long end, @BodyParm int page, @BodyParm int pageSize) {
		var entities = Logs.selectByTimerange(from, end, page, pageSize);
		return Strings.format("[@]", entities.toString(",", e -> Logs.entityJson(e, protect)));
	}

}
