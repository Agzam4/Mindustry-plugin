package agzam4.api;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpExchange;

import arc.func.Func;

public class SseSource<T> {

    private final ConcurrentHashMap<HttpExchange, SseClient<T>> activeClients = new ConcurrentHashMap<>();
    
    public void register(HttpExchange exchange, Func<T, String> processor) throws IOException {
    	exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
    	exchange.getResponseHeaders().set("Cache-Control", "no-cache");
    	exchange.getResponseHeaders().set("Connection", "keep-alive");
    	exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
    	exchange.sendResponseHeaders(200, 0);
        SseClient<T> client = new SseClient<>(exchange, processor);
        activeClients.put(exchange, client);
        ApiServer.pingListeners.add(this::pingDisconnects);
    }

    public void broadcast(T event) {
        if(activeClients.isEmpty()) return;
        activeClients.forEach((exchange, client) -> {
            if(client.send(event)) return;
            activeClients.remove(exchange);
        });
    }
    
    public void pingDisconnects() {
        if(activeClients.isEmpty()) return;
        activeClients.forEach((exchange, client) -> {
            if(client.ping()) return;
            activeClients.remove(exchange);
        });
    }
}
