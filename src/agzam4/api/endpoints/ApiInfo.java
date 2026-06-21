package agzam4.api.endpoints;

import agzam4.api.auth.AuthDatabase;
import agzam4.api.auth.SensitiveData;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import arc.util.serialization.Jval;

@Router("/info")
public class ApiInfo {

	@Post
	public static String resolve(@SessionId String sessionId, @SessionIp String ip, @BodyParm int id) {
		String uuid = AuthDatabase.validate(sessionId, ip);
		if(uuid == null) return Jval.newObject().put("error", "unauthorized").toString();

		String value = SensitiveData.resolve(id);
		if(value == null) return Jval.newObject().put("error", "not found").toString();
		return Jval.newObject().put("value", value).toString();
	}
}



