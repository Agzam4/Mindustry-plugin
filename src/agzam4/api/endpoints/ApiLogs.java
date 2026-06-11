package agzam4.api.endpoints;

import agzam4.api.ApiAnnotations.BodyField;
import agzam4.api.ApiAnnotations.PostEndpoint;
import agzam4.logs.Logs;
import arc.util.Log;
import arc.util.Strings;

public class ApiLogs {


	@PostEndpoint
	public static String timerange(
			@BodyField("protect") boolean protect, 
			@BodyField("from") long from, 
			@BodyField("end") long end, 
			@BodyField("page") int page, 
			@BodyField("pageSize") int pageSize
			) {
		Log.info("Meow");
		var entities = Logs.selectByTimerange(from, end, page, pageSize);
		Log.info("entities: @", entities.toString(",", e -> protect ? Logs.protectedJson(e) : e.message));
		return Strings.format("[@]", entities.toString(",", e -> protect ? Logs.protectedJson(e) : e.message));
	}



}
