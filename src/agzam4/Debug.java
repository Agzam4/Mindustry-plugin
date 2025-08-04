package agzam4;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;

import agzam4.utils.Log;
import arc.files.Fi;
import arc.graphics.Color;
import arc.math.geom.Point2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Threads;
import arc.util.io.PropertiesUtils;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.gen.Call;
import mindustry.net.Administration.Config;

public class Debug {

	public static boolean debug = false;
	
	public static void effect(Point2 point) {
		effect(point.x, point.y);
	}
	
	public static void effect(int x, int y) {
		effect(x, y, 1);
	}
	
	public static void effect(int x, int y, int size) {
		if(configDebug()) Call.effect(Fx.rotateBlock, x*Vars.tilesize, y*Vars.tilesize, size, Color.white);
	}
	
	static Scanner scanner = new Scanner(System.in);
	static File cd = new File(System.getProperty("user.dir") + "/build/libs/");
	static ObjectMap<String, String> env = new ObjectMap<>();

	public static void main(String[] args) throws IOException {
		
		PropertiesUtils.load(env, Fi.get(".env.properties").reader());
		
		String type = env.get("event.name");
		
		System.out.println(cd.getPath());

		Fi user = Fi.get(System.getProperty("user.dir"));
		Fi eventsFileSrc = user.parent().child(type).child("build").child("libs").child(type + ".jar");
		Fi eventsFileDst = user.child("build").child("libs").child("config").child("events").child(type + ".jar");
		if(!user.exists()) Log.info("Events project not found");

		Log.info("User dir: [blue]@[]", user.absolutePath());
		Log.info("Events src: [blue]@[]", eventsFileSrc.absolutePath());
		Log.info("Events trget: [blue]@[]", eventsFileDst.absolutePath());
		
		new Thread(() -> {
			try {
				Seq<FileTimeContoller> controllers = Seq.with();
				
				for (var f : new File(System.getProperty("user.dir") + "/build/libs/config/mods").listFiles()) {
					if(!f.getName().endsWith(".jar")) continue;
					controllers.add(new FileTimeContoller(f));
					break;
				}
				while (true) {
					for (int i = 0; i < controllers.size; i++) {
						if(controllers.get(i).changed()) {
							runid++;
							System.out.println("Other time");
						}
					}
					if(eventsFileSrc.exists()) {
						eventsFileDst.delete();
						eventsFileSrc.moveTo(eventsFileDst);
						runid++;
						System.out.println("Events update");
					}
					Threads.sleep(1000);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, "updater").start();

		new Thread(() -> {
			final int id = runid;
			while (id == runid) {
				String line = scanner.nextLine();
				if(line.equals("a")) line = "admin add " + env.get("admin.name");
				writer.println(line);
				writer.flush();
			}
		}, "input-writer").start();
		
		while (true) {
			run();
		}
	}

	static volatile int runid = 0;
	static PrintWriter writer;
	
	private static void run() throws IOException {
		Process p = Runtime.getRuntime().exec("java -jar server-release.jar host", null, cd);
		
		System.out.println("PID: " + p);

		BufferedReader r;
		BufferedReader e;
		
		r = new BufferedReader(new InputStreamReader(p.getInputStream()));
		e = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		writer = new PrintWriter(new OutputStreamWriter(p.getOutputStream()));
		
		Thread writerThread = new Thread(() -> {
			final int id = runid;
			String line;
			while (id == runid) {
				try {
					line = r.readLine();
					if (line == null) { break; }
					if(line.contains("Opened a server")) {
						writer.println(env.get("args"));
						writer.flush();
					}
					if(line.indexOf("Selected next map to be") != -1) {
						writer.println("js Vars.state.rules.infiniteResources = true");
					}
					if(line.contains(env.get("admin.name")) && line.contains("has connected.")) {
						writer.println("admin add " + env.get("admin.name"));
						writer.flush();
					}
					System.out.println(line);
				} catch (IOException e1) {
					e1.printStackTrace();
					break;
				}
			}
			System.out.println("END");
		}, "auto-writer-thread");
		writerThread.start();
		
		Thread errorThread = new Thread(() -> {
			final int id = runid;
			String line;
			while (id == runid) {
				try {
					line = e.readLine();
					if (line == null) { break; }
					System.err.println(line);
				} catch (IOException e1) {
					e1.printStackTrace();
					break;
				}
			}
			System.err.println("END");
		}, "error thread");
		errorThread.start();		
		

		final int id = runid;
		while (id == runid && p.isAlive()) {
			Threads.sleep(100);
		}
		if(p.isAlive()) {
			writer.println("restart force");
			writer.flush();
			Threads.sleep(100);
		}
		
		System.out.println("close");
		p.destroy();
		writer.close();
		r.close();
		e.close();
		System.out.println("destroy");
		System.out.println("interrupt");
		writerThread.interrupt();
		errorThread.interrupt();
		System.out.println("\n\n\n\n\n\n\n\n=====[Refreshing]=====");
		Log.reset();
	}

	public static boolean configDebug() {
		return Config.debug.bool();
	}
	
	private static class FileTimeContoller {
		
		private File file;
		private long lastTime;

		private FileTimeContoller(String path) throws IOException {
			this(new File(path));
		}
		
		private FileTimeContoller(File file) throws IOException {
			this.file = file;
			lastTime = time();
		}
		
		private boolean changed() throws IOException {
			long time = time();
			if(lastTime != time) {
				lastTime = time;
				return true;
			}
			return false;
		}
		
		private long time() throws IOException {
			return Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis();
		}
	}
}
