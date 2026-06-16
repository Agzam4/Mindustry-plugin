package agzam4.api.auth;

import java.util.UUID;

import agzam4.api.ApiAnnotations.BodyField;
import agzam4.api.ApiAnnotations.HeadField;
import agzam4.api.ApiAnnotations.PostEndpoint;
import arc.util.serialization.Jval;

public class ApiAuth {

    @PostEndpoint("create-session")
    public static String createSession(@BodyField("token") String token, @HeadField("Client-Ip") String ip) {
        String uuid = AuthTokens.verify(token);
        if(uuid == null) return Jval.newObject().put("error", "invalid or expired token").toString();
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
