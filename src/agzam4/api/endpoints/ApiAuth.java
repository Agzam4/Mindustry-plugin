package agzam4.api.endpoints;

import java.util.UUID;

import agzam4.api.ApiAnnotations.BodyField;
import agzam4.api.ApiAnnotations.PostEndpoint;
import agzam4.api.auth.AuthDatabase;
import agzam4.api.auth.AuthTokens;
import agzam4gen.api.dependencies.BodyParm;
import agzam4proc.api.ApiAnnotations.Post;
import agzam4proc.api.ApiAnnotations.Router;
import arc.util.serialization.Jval;

@Router("/auth")
public class ApiAuth {
	
    @Post("create-session")
    public static String createSession(@BodyParm String token, @BodyParm String ip) {
        String uuid = AuthTokens.verify(token);
        if(uuid == null) return Jval.newObject().put("error", "Неверный токен, зайди в игру и пропиши /auth").toString();
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        AuthDatabase.createSession(sessionId, uuid, ip);
        return Jval.newObject().put("id", sessionId).put("uuid", uuid).toString();
    }

    @PostEndpoint("logout")
    public static String logout(@BodyField("id") String id) {
    	
        AuthDatabase.remove(id);
        return Jval.newObject().put("ok", true).toString();
    }
}
