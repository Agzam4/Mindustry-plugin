package agzam4.dev;

import java.util.concurrent.CountDownLatch;

import agzam4.utils.Log;
import arc.files.Fi;
import arc.func.Boolf;
import arc.math.Mathf;
import arc.util.ArcRuntimeException;
import arc.util.Http;
import arc.util.io.Streams;
import arc.util.serialization.Jval;
import mindustry.Vars;

public class GithubDownloader {

	 public static void latest(String repo, Boolf<String> assetSearcher, Fi dst) {
		 CountDownLatch latch = new CountDownLatch(1);
		 Throwable[] exceptionHolder = new Throwable[1];
		 Http.get(Vars.ghApi + "/repos/" + repo + "/releases/latest", res -> {
			 try {
				 var json = Jval.read(res.getResultAsString());
				 var assets = json.get("assets").asArray();

				 var asset = assets.find(j -> assetSearcher.get(j.getString("name", "")));

				 if (asset != null) {
					 var url = asset.getString("browser_download_url");
					 String fileName = asset.getString("name");

					 Http.get(url, result -> {
						 long len = result.getContentLength();
						 dst.parent().mkdirs();

						 try (var stream = dst.write(false)) {
							 Streams.copyProgress(result.getResultAsStream(), stream, len, 4096, p -> {
								 Log.info("Downloading: [cyan]@[] [blue]@%[]", fileName, Mathf.round(p * 100));
							 });
							 Log.info("Saved [cyan]@[]", dst.absolutePath());
						 } catch (Exception e) {
							 exceptionHolder[0] = e;
						 } finally {
							 latch.countDown();
						 }
					 }, err -> {
						 exceptionHolder[0] = err;
						 latch.countDown();
					 });

				 } else {
					 exceptionHolder[0] = new ArcRuntimeException("Asset not found");
					 latch.countDown();
				 }
			 } catch (Exception e) {
				 exceptionHolder[0] = e;
				 latch.countDown();
			 }
		 }, err -> {
			 exceptionHolder[0] = err;
			 latch.countDown();
		 });

		 try {
			 latch.await();
		 } catch (InterruptedException e) {
			 Thread.currentThread().interrupt();
			 throw new ArcRuntimeException("Thread interrupted", e);
		 }

		 if (exceptionHolder[0] != null) {
			 if (exceptionHolder[0] instanceof RuntimeException) {
				 throw (RuntimeException) exceptionHolder[0];
			 } else {
				 throw new ArcRuntimeException("Error", exceptionHolder[0]);
			 }
		 }
	 }
	 
}
