package agzam4proc.api.lib;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpExchange;

import arc.func.Func;
import arc.struct.Seq;

public class SseSource<T> {

	public static final Seq<Runnable> pingListeners = Seq.with();
	
    private final ConcurrentHashMap<HttpExchange, SseClient<T>> activeClients = new ConcurrentHashMap<>();
    
    public void register(HttpExchange exchange, Func<T, String> processor) throws IOException {
    	exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
    	exchange.getResponseHeaders().set("Cache-Control", "no-cache");
    	exchange.getResponseHeaders().set("Connection", "keep-alive");
    	exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
    	exchange.sendResponseHeaders(200, 0);
        SseClient<T> client = new SseClient<>(exchange, processor);
        activeClients.put(exchange, client);
        pingListeners.add(this::pingDisconnects);
    }

    public void broadcast(T event) {
        if(activeClients.isEmpty()) return;
        activeClients.entrySet().removeIf(entry -> !entry.getValue().send(event));
    }
    
    public void pingDisconnects() {
        if(activeClients.isEmpty()) return;
        activeClients.entrySet().removeIf(entry -> !entry.getValue().ping());
    }
}
