package agzam4.api.endpoints;

import java.util.UUID;

import agzam4.api.auth.AuthDatabase;
import agzam4.api.auth.AuthTokens;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;
import agzam4proc.api.lib.ApiResponse;
import arc.util.serialization.Jval;

@Router("/auth")
public class ApiAuth {
	
    @Post
    public static SessionResponse createSession(@BodyParm String token, @SessionIp String ip) throws ApiResponse {
        String uuid = AuthTokens.verify(token);
        if(uuid == null) throw new ApiResponse("Неверный токен, зайди в игру и пропиши /auth");
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        AuthDatabase.createSession(sessionId, uuid, ip);
        return new SessionResponse(sessionId, uuid); //Jval.newObject().put("id", sessionId).put("uuid", uuid).toString();
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
    	
    	public SessionResponse(String id, String uuid) {
    		this.id = id;
    		this.uuid = uuid;
		}
    	
    }
    
}
