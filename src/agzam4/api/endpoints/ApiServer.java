package agzam4.api.endpoints;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import agzam4.admins.Admins;
import agzam4gen.api.dependencies.Auth;
import agzam4gen.api.dependencies.BodyParm;
import agzam4proc.apt.api.ApiAnnotations.Post;
import agzam4proc.apt.api.ApiAnnotations.Router;
import agzam4proc.apt.api.lib.ApiResponse;
import arc.Core;
import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;

@Router("/server")
public class ApiServer {

	@Post
	public static String js(@Auth PlayerInfo info, @BodyParm String js) throws ApiResponse {
		if(!Admins.has(info, "js")) throw ApiResponse.forbidden;
		
		AtomicReference<String> resultRef = new AtomicReference<>();
		AtomicReference<Throwable> errorRef = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		Core.app.post(() -> {
			try {
				String res = Vars.mods.getScripts().runConsole(js);
				resultRef.set(res);
			} catch (Throwable t) {
				errorRef.set(t);
			} finally {
				latch.countDown();
			}
		});
		try {
			boolean completed = latch.await(3, TimeUnit.SECONDS);
			if (!completed) {
				throw new ApiResponse("uncompleted due timeout (3 SECONDS)");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiResponse(e.getMessage());
		}

		if (errorRef.get() != null) return "Error: " + errorRef.get().getMessage();
	    return resultRef.get();
	}
	
}
