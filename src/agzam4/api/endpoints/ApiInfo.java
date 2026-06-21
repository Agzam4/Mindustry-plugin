package agzam4.api.endpoints;

import agzam4.api.ApiAnnotations.BodyField;
import agzam4.api.ApiAnnotations.HeadField;
import agzam4.api.ApiAnnotations.PostEndpoint;
import agzam4.api.auth.AuthDatabase;
import agzam4.api.auth.SensitiveData;
import agzam4proc.api.ApiAnnotations.Router;
import arc.util.serialization.Jval;

@Router("/info")
public class ApiInfo {

	@PostEndpoint("resolve")
	public static String resolve(
			@HeadField("Session-Id") String sessionId,
			@HeadField("Client-Ip") String ip,
			@BodyField("id") int id
	) {
		String uuid = AuthDatabase.validate(sessionId, ip);
		if(uuid == null) return Jval.newObject().put("error", "unauthorized").toString();

		String value = SensitiveData.resolve(id);
		if(value == null) return Jval.newObject().put("error", "not found").toString();
		return Jval.newObject().put("value", value).toString();
	}
}
