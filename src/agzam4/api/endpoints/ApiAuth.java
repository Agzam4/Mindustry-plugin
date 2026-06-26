package agzam4.api.endpoints;

import java.util.UUID;

import agzam4.api.auth.AuthDatabase;
import agzam4.api.auth.AuthTokens;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import arc.util.serialization.Jval;

@Router("/auth")
public class ApiAuth {
	
    @Post
    public static String createSession(@BodyParm String token, @SessionIp String ip) {
        String uuid = AuthTokens.verify(token);
        if(uuid == null) return Jval.newObject().put("error", "Неверный токен, зайди в игру и пропиши /auth").toString();
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        AuthDatabase.createSession(sessionId, uuid, ip);
        return Jval.newObject().put("id", sessionId).put("uuid", uuid).toString();
    }

    @Post
    public static String logout(@BodyParm String id) {
        AuthDatabase.remove(id);
        return Jval.newObject().put("ok", true).toString();
    }
    
    @Type
    public static class OkResponse {

    	public boolean ok;
    	
    }
    
    
    @Type
    public static class SessionResponse extends OkResponse {

    	public String id;
    	public String uuid;
    	
    }
    
}
