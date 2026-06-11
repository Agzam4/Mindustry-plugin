package agzam4.api;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import com.sun.net.httpserver.HttpServer;

import agzam4.api.endpoints.ApiLogs;
import agzam4.utils.Log;
import arc.Core;
import arc.Settings;
import arc.util.Threads;
import mindustry.Vars;
import mindustry.net.Administration.Config;

/**
 * HTTP API for server
 * [!] Loopback access only for security purposes
 */
public class ApiServer {

	static {
		Core.settings = new Settings();
	}
	public static Config apiPort = new Config("apiPort", "Порт к http api", Vars.port + 1, () -> port(configPort()));
    private static int currentPort = configPort();

    private static final Object lock = new Object();
    private static HttpServer server = null;
    private static ExecutorService executor = null;
	
    private static final int maxThreads = 4;
	
	public static void init() {
		start();
	}
	
	public static void main(String[] args) {
		init();
	}

    private ApiServer() {}

    public static void start() {
        synchronized (lock) {
            if (server != null) {
                Log.warn("Server already running at @", currentPort);
                return;
            }

            try {
                InetAddress loopback = InetAddress.getLoopbackAddress();
                InetSocketAddress address = new InetSocketAddress(loopback, currentPort);

                server = HttpServer.create(address, 0);
                
                executor = Threads.boundedExecutor("http-api", maxThreads);
                server.setExecutor(executor);
                setupRoutes();
                
                server.start();
                Log.info("Api server started at http://127.0.0.1:@", currentPort);
            } catch (IOException e) {
                Log.err(e);
            }
        }}

    public static void stop() {
        synchronized (lock) {
            if (server == null) {
            	Log.warn("Server already stopped");
                return;
            }
            server.stop(0);
            server = null;
            Log.info("Api server stoped");
        }
    }

    public static void port(int newPort) {
        synchronized (lock) {
            if (currentPort == newPort) return;
            currentPort = newPort;
            if (server != null) {
                stop();
                start();
            }
        }
    }

    public static int configPort() {
    	return apiPort.num();
    }
    
    public static int currentPort() {
        synchronized (lock) {
            return currentPort;
        }
    }

    private static void setupRoutes() {
        if (server == null) return;

        new ApiRouter(ApiLogs.class).register(server);
//        
//        server.createContext("/", exchange -> {
//            String response = "Hello from secured localhost!";
//            exchange.sendResponseHeaders(200, response.getBytes().length);
//            exchange.getResponseBody().write(response.getBytes());
//            exchange.getResponseBody().close();
//        });
    }
	
	
}
