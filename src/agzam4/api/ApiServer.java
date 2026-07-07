package agzam4.api;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import agzam4.api.auth.AuthDatabase;
import agzam4.api.auth.SensitiveData;
import agzam4.utils.Log;
import agzam4gen.api.Routers;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.net.Administration.Config;

/**
 * HTTP API for server
 * [!] Loopback access only for security purposes
 */
public class ApiServer {

	public static Config apiPort = new Config("apiPort", "Порт к http api", Vars.port + 1, () -> port(configPort()));
	public static Config authUrl = new Config("authUrl", "Ссылка на сайт для авторизации", "");
	
    private static int currentPort = configPort();

    private static final Object lock = new Object();
    private static HttpServer server = null;
    private static ExecutorService executor = null;
    
    public static Seq<Runnable> pingListeners = Seq.with();
	
    private static final int maxThreads = 4;
	
	public static void init() {
		try {
			AuthDatabase.init(Vars.dataDirectory.child("auth.db"));
			SensitiveData.init(Vars.dataDirectory.child("sensitive.db"));
		} catch (Exception e) {
			Log.err(e);
		}
		start();
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
    			pingListeners.clear();
    			
    			server = HttpServer.create(address, 0);

    			executor = create("http-api", maxThreads);
    			server.setExecutor(executor);
    			setupRoutes();

    			server.start();
    			Log.info("Api server started at [cyan]http://127.0.0.1:@", currentPort);
    			
    			executor.execute(new Runnable() {
    			    @Override
    			    public void run() {
    			        try {
    			        	pingListeners.forEach(p -> p.run());
    			        } catch (Exception e) {
    			            Log.err(e); 
    			        }
    			        CompletableFuture.delayedExecutor(20, TimeUnit.SECONDS, executor).execute(this);
    			    }
    			});
    			
    		} catch (IOException e) {
    			Log.err(e);
    		}
    	}
    }
    
    public static ThreadPoolExecutor create(String poolName, int maxThreads) {
        final AtomicInteger threadNumber = new AtomicInteger(1);
        return new ThreadPoolExecutor(0, maxThreads, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(32),
                runnable -> {
                    String name = poolName + "-worker-" + threadNumber.getAndIncrement();
                    Thread thread = new Thread(runnable, name);
                    thread.setPriority(Thread.MIN_PRIORITY); 
                    thread.setDaemon(true); 
                    thread.setUncaughtExceptionHandler((t, e) -> Log.err(e));
                    return thread;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy() 
            );
    }

    public static void stop() {
        synchronized (lock) {
            if (server == null) {
            	Log.warn("Server already stopped");
                return;
            }
            server.stop(0);
            executor.shutdown();
            pingListeners.clear();
            server = null;
            executor = null;
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
        Log.info("Setup routes...");
        Routers.register(server);
    }
	
	
}
