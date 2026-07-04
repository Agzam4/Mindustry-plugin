package agzam4.api.endpoints;

import agzam4.Game;
import agzam4.api.auth.SensitiveData;
import agzam4gen.api.dependencies.*;
import agzam4proc.api.ApiAnnotations.*;

@Router("/info")
public class ApiInfo {

    @Type
    public static class ResolvedPlayerInfo {

    	public String uuid;
    	public String name;
    	
    	public ResolvedPlayerInfo(String uuid, String name) {
    		this.uuid = uuid;
    		this.name = name;
		}
    	
    }
	
	@Post
	public static ResolvedPlayerInfo[] resolvePlayer(@SessionId String sessionId, @SessionIp String ip, @BodyParm int[] ids) {
//		String uuid = AuthDatabase.validate(sessionId, ip);
//		if(uuid == null) return new String[] {}; FIXME
		
		ResolvedPlayerInfo[] result = new ResolvedPlayerInfo[ids.length];
		for (int i = 0; i < result.length; i++) {
			String resolved = SensitiveData.resolve(ids[i]);
			if(resolved == null) continue;
			result[i] = new ResolvedPlayerInfo(resolved, Game.nameByUuid(resolved));
		}
		return result;
	}
}



