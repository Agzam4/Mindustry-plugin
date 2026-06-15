package agzam4.api;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;

import arc.func.Func;

public class SseClient<T> {
	
    private final HttpExchange exchange;
    private final OutputStream os;
    public final Func<T, String> processor; 

    public SseClient(HttpExchange exchange, Func<T, String> processor) {
        this.exchange = exchange;
        this.os = exchange.getResponseBody();
        this.processor = processor;
    }

    public synchronized boolean send(T event) {
        try {
            os.write("data: ".getBytes(StandardCharsets.UTF_8));
            os.write(processor.get(event).getBytes(StandardCharsets.UTF_8));
            os.write("\n\n".getBytes(StandardCharsets.UTF_8));
            os.flush();
            return true;
        } catch (Exception e) {
            try { exchange.close(); } catch (Exception ignored) {}
            return false;
        }
    }

    public synchronized boolean ping() {
        try {
            os.write(": ping\n\n".getBytes(StandardCharsets.UTF_8));
            os.flush();
            return true;
        } catch (Exception e) {
            close();
            return false;
        }
    }
    
    private void close() {
        try { exchange.close(); } catch (Exception ignored) {}
    }

}