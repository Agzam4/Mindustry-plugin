package agzam4;

import java.io.IOException;
import java.util.Scanner;

import agzam4.dev.FileWatcher;
import agzam4.dev.GithubDownloader;
import agzam4.dev.ProcessController;
import agzam4.utils.Log;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.PropertiesUtils;
import mindustry.Vars;

public class Dev {

	static Fi root = new Fi(System.getProperty("user.dir") + "/build/libs/");
	static Fi proxyRoot = new Fi(System.getProperty("user.dir") + "/proxy/build");
	
	static Scanner scanner = new Scanner(System.in);
	static ObjectMap<String, String> props = new ObjectMap<>();

	public static void main(String[] args) throws IOException, InterruptedException {
		Fi propsFi = Fi.get(".env.properties");
		if(!propsFi.exists()) {
			Log.info("[blue]Enter admin name:");
			propsFi.writeString("admin.name=" + scanner.nextLine(), false);
		}
		
		PropertiesUtils.load(props, Fi.get(".env.properties").reader());
		
		String[] type = props.get("event.name").split("\n");
		
		Fi serverRelease = root.child("server-release.jar");
		Log.info(serverRelease.absolutePath());
		if(!serverRelease.exists()) {
			GithubDownloader.latest("Anuken/Mindustry", n -> n.equals("server-release.jar"), serverRelease);
		}

		Fi user = Fi.get(System.getProperty("user.dir"));
		
		Seq<Fi> pluginFiles = root.child("config").findAll(f -> f.name().endsWith(".jar"));
		Fi[] eventsFileSrc = new Fi[type.length];
		Fi[] eventsFileDst = new Fi[type.length];
		for (int i = 0; i < type.length; i++) {
			eventsFileSrc[i] = user.parent().child(type[i]).child("build").child("libs").child(type[i] + ".jar");
			eventsFileDst[i] = user.child("build").child("libs").child("config").child("events").child(type[i] + ".jar");
			Log.info("Events src: [blue]@[]", eventsFileSrc[i].absolutePath());
			Log.info("Events trget: [blue]@[]", eventsFileDst[i].absolutePath());
		}
		
		Log.info("Java files:\n[cyan]@[]", pluginFiles.toString("\n"));

		ProcessController plugin = new ProcessController("Mindustry-plugin", "java", "--enable-native-access=ALL-UNNAMED", "-jar", "server-release.jar", "host").io().responder(input -> {
			if(input.contains("Opened a server")) return props.get("args");
			if(input.contains("Selected next map to be")) return "js Vars.state.rules.infiniteResources = true";
			if(input.contains(props.get("admin.name")) && !input.contains("status")) return "admin add " + props.get("admin.name");
			return null;
		});
		plugin.workdir = root;
		plugin.start();

		FileWatcher.watch(f -> {
			plugin.stop();
			try {
				plugin.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, pluginFiles);
		
		ProcessController proxy = new ProcessController("Proxy", "proxy"); //.out();
		proxy.workdir = proxyRoot;
		
		ObjectMap<String, String> goenv = ObjectMap.of(
				"JAVA_API_URL", "http://127.0.0.1:" + (Vars.port + 1),
				"LISTEN_ADDR", ":8080",
				"TLS_MODE", "none",
				"DOMAIN", "",
				"STATIC_DIR", "./static"
		);		
		proxy.env = goenv;
		proxy.start();
		
		FileWatcher.watch(f -> {
			plugin.stop();
			try {
				plugin.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, proxyRoot.child("proxy"));

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
		    Log.info("Shutdown...");
		    plugin.stop();
		    proxy.stop();
		}, "Shutdown-Hook"));
		
        Thread.currentThread().join();
	}
	
}
