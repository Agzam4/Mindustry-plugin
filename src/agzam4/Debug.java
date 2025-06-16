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

import arc.graphics.Color;
import arc.math.geom.Point2;
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
	
	public static void main(String[] args) throws IOException {
		System.out.println(cd.getPath());

		new Thread(() -> {
			try {
				File modFile = null;
				for (var f : new File(System.getProperty("user.dir") + "/build/libs/config/mods").listFiles()) {
					if(!f.getName().endsWith(".jar")) continue;
					modFile = f;
					break;
				}
				long lastTime = Files.readAttributes(modFile.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis();
				while (true) {
					// TODO: check file data for auto restart
					long time = Files.readAttributes(modFile.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis();

					//System.out.println(time + "/" + lastTime);
					if(lastTime != time) {
						runid++;
						lastTime = time;
						System.out.println("Other time");
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();

		new Thread(() -> {
			final int id = runid;
			while (id == runid) {
				String line = scanner.nextLine();
				if(line.equals("a")) line = "admin add Agzam";
				writer.println(line);
				writer.flush();
			}
		}).start();
		
		while (true) {
			run();
		}
	}

	static volatile int runid = 0;
	static PrintWriter writer;
	
	private static void run() throws IOException {
		Process p = Runtime.getRuntime().exec(
				"java -jar server-release.jar host",
				null, cd);
		
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
					if(line.endsWith("Opened a server on port 6567.")) {
						writer.println("event space_danger faston");
						writer.println("event music_event faston");
						writer.println("event lucky_place faston");
						writer.println("js Vars.state.rules.infiniteResources = true");
//						writer.println("js Vars.state.wave = -999");
						writer.println("event ability_event faston");
						writer.println("rules add reactorExplosions true");
						writer.flush();
//						writer.println("nextmap Shattered");
//						writer.flush();
//						writer.println("gameover");
//						writer.flush();
					}
					if(line.indexOf("Selected next map to be") != -1) {
						writer.println("js Vars.state.rules.infiniteResources = true");
//						writer.println("js Vars.state.wave = -999");
					}
					if(line.indexOf("Agzam has connected.") != -1) {
						writer.println("admin add Agzam");
						writer.flush();
					}
					System.out.println(line);
				} catch (IOException e1) {
					e1.printStackTrace();
					break;
				}
			}
			System.out.println("END");
		});
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
		});
		errorThread.start();		
		

		final int id = runid;
		while (id == runid) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		System.out.println("close");
		p.destroy();
		writer.close();
		r.close();
		e.close();
		System.out.println("destroy");
		System.out.println("interrupt");
//		scannerThread.interrupt();
		writerThread.interrupt();
		errorThread.interrupt();
		System.out.println("\n\n\n\n\n\n\n\n=====[Refreshing]=====");
	}

	public static boolean configDebug() {
		return Config.debug.bool();
	}
}
