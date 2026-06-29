package agzam4.api.endpoints;

import agzam4.logs.LogEvents.LogEntity;
import agzam4.logs.Logs;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import agzam4proc.api.lib.SseSource;
import arc.util.Strings;

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
	public static String timerange(@BodyParm boolean protect, @BodyParm long from, @BodyParm long end, @BodyParm int page, @BodyParm int pageSize) {
		var entities = Logs.selectByTimerange(from, end, page, pageSize);
		return Strings.format("[@]", entities.toString(",", e -> Logs.entityJson(e, protect)));
	}
	
	@Post 
	public static LogEntity[] list(@BodyParm int afterId, @BodyParm int limit) {
		return new LogEntity[] {}; // TODO
	}
	
	@Post 
	public static LogEntity[] search(@BodyParm int afterId, @BodyParm int limit, @BodyParm long from, @BodyParm long to, @BodyParm long[] tags, @BodyParm String query) {
		return new LogEntity[] {}; // TODO: possibly more parameters (for json content possible)
	}
	

}
