package agzam4.dev;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

import arc.files.Fi;
import arc.func.Func;
import arc.struct.ObjectMap;
import arc.util.Strings;
import arc.util.Threads;

public class ProcessController {
	
	public final String name;
	private final String[] command;
	public Fi workdir = null;
	public boolean out = false;
	public boolean in = false;
	public Func<String, String> responder = null;
	public ObjectMap<String, String> env = ObjectMap.of();
	
	private Process process;
	private Thread outputThread;
	private Thread inputThread;
	private BufferedWriter writer;

	public ProcessController(String name, String... command) {
		this.command = command;
		this.name = name;
	}

	public ProcessController out() {
		out = true;
		return this;
	}

	public ProcessController in() {
		in = true;
		return this;
	}
	
	public ProcessController io() {
		in = true;
		out = true;
		return this;
	}

	public ProcessController responder(Func<String, String> responder) {
		this.responder = responder;
		return this;
	}

	public synchronized void start() throws IOException {
		if (process != null && process.isAlive()) return;

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		if(workdir != null) pb.directory(workdir.file());
		env.each((k,v) -> pb.environment().put(k,v));
		

		process = pb.start();
		process.onExit().thenAccept(p -> System.out.print("Process " + name + " stopped"));

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
		    if (process != null && process.isAlive()) {
		        process.destroyForcibly();
		    }
		}, name + "-shutdown-hook"));
		
		writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

		outputThread = Threads.daemon(name + "-output", this::readOutput);
		if(in) inputThread = Threads.daemon(name + "-input", this::forwardInput);
	}

	public synchronized void stop() {
		if(process == null) return;

		if(outputThread != null) outputThread.interrupt();
		if(inputThread != null) inputThread.interrupt();

		try {
			if (writer != null) writer.close();
		} catch (IOException ignored) {}

		if (process.isAlive()) {
			process.destroy();
			try {
				if (!process.waitFor(1, TimeUnit.SECONDS)) {
					process.destroyForcibly();
				}
			} catch (InterruptedException e) {
				process.destroyForcibly();
				Thread.currentThread().interrupt();
			}
		}
		process = null;
	}

	public synchronized void command(String cmd) {
		if (process != null && process.isAlive() && writer != null) {
			try {
				writer.write(cmd);
				writer.newLine();
				writer.flush();
			} catch (IOException e) {}
		}
	}

	private void readOutput() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if(out) {
					System.out.println(Strings.format("[@] @", name, line));
				}

				if(responder != null) {
					String response = responder.get(line);
					if(response != null && !response.isEmpty()) {
						command(response);
					}
				}
			}
		} catch (IOException ignored) {}
	}

	private void forwardInput() {
		try (BufferedReader sysReader = new BufferedReader(new InputStreamReader(System.in))) {
			String line;
			while (!Thread.currentThread().isInterrupted() && in && (line = sysReader.readLine()) != null) {
				command(line);
			}
		} catch (IOException ignored) {}
	}

}
