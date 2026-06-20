package agzam4.api.endpoints;

import agzam4.api.ApiAnnotations.BodyField;
import agzam4.api.ApiAnnotations.PostEndpoint;
import agzam4.api.ApiAnnotations.SseEndpoint;
import agzam4.api.ApiAnnotations.SseProcessor;
import agzam4.api.SseSource;
import agzam4.logs.LogEvents.LogEntity;
import agzam4proc.api.annotations.Router;
import agzam4.logs.Logs;
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

	@PostEndpoint
	public static String timerange(
			@BodyField("protect") boolean protect, 
			@BodyField("from") long from, 
			@BodyField("end") long end, 
			@BodyField("page") int page, 
			@BodyField("pageSize") int pageSize
			) {
		var entities = Logs.selectByTimerange(from, end, page, pageSize);
		return Strings.format("[@]", entities.toString(",", e -> Logs.entityJson(e, protect)));
	}

	
	
	
}
