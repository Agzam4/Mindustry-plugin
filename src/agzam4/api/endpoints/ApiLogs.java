package agzam4.api.endpoints;

import agzam4.api.ApiAnnotations.BodyField;
import agzam4.api.ApiAnnotations.SseEndpoint;
import agzam4.api.ApiAnnotations.SseProcessor;
import agzam4.api.SseSource;
import agzam4.logs.LogEvents.LogEntity;
import agzam4.logs.Logs;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import arc.func.Func;
import arc.util.Strings;

@Router("/logs")
public class ApiLogs {

	@SseEndpoint
	public static SseSource<LogEntity> logsStream = new SseSource<LogEntity>() {
		
		@SseProcessor
		public Func<LogEntity, String> processor(@BodyField("protect") boolean protect) { 
			return e -> Logs.entityJson(e, protect);
		}

	};

	@Post
	public static String timerange(@BodyParm boolean protect, @BodyParm long from, @BodyParm long end, @BodyParm int page, @BodyParm int pageSize) {
		var entities = Logs.selectByTimerange(from, end, page, pageSize);
		return Strings.format("[@]", entities.toString(",", e -> Logs.entityJson(e, protect)));
	}

}
