package agzam4.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import agzam4.api.ApiAnnotations.BodyField;
import agzam4.api.ApiAnnotations.PostEndpoint;
import agzam4.api.ApiAnnotations.SseEndpoint;
import agzam4.api.ApiAnnotations.SseProcessor;
import agzam4.utils.Log;
import arc.func.Func;
import arc.func.Func2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.serialization.Jval;

public class ApiRouter {

	public String prefix;
	
	public ApiRouter(Class<?> cls) {
		this(cls, cls.getSimpleName().toLowerCase().replaceAll("api", ""));
	}
	
	private interface Handler {
		
		ApiResponse get(Jval body);
		
	}

	private interface SseHandler {

		ApiResponse get(HttpExchange exchange, Jval body);

	}
	
	private ObjectMap<String, Handler> handlers = ObjectMap.of();
	private ObjectMap<String, SseHandler> sseHandlers = ObjectMap.of();

	@SuppressWarnings("unchecked")
	public ApiRouter(Class<?> cls, String prefix) {
		Log.info("== Class: @ ==", cls.getSimpleName());
		this.prefix = prefix;

		Seq<Method> methods = Seq.with(cls.getMethods()).select(m -> Modifier.isStatic(m.getModifiers()));
		Seq<Field> fields = Seq.with(cls.getFields()).select(m -> Modifier.isStatic(m.getModifiers()));
		
		for (var method : methods) {
			var endpoint = method.getAnnotation(PostEndpoint.class);
			if (endpoint == null) continue;
			
			String name = enpointName(endpoint.value(), method.getName());

			Log.info("Endpoint: [cyan]/@/@", prefix, name);

			var parmsAnnotations = method.getParameterAnnotations();
			var parmsTypes = method.getParameterTypes();

			if(method.getReturnType() != String.class) throw new RuntimeException(Strings.format("return type of endpoint @.@ must be String", cls.getSimpleName(), method.getName()));
			String[] names = new String[parmsTypes.length];
			Func2<Jval, String, Object>[] extractors = new Func2[parmsTypes.length];

			for (int i = 0; i < names.length; i++) {
				for (var a : parmsAnnotations[i]) if(a instanceof BodyField parm) names[i] = parm.value();
				if(names[i] == null) throw new RuntimeException(Strings.format("no @ annotation in parametr of endpoint @.@ must be String", BodyField.class.getSimpleName(), cls.getSimpleName(), method.getName()));
				extractors[i] = exractorFor(parmsTypes[i]);
				if(extractors[i] == null) throw new RuntimeException(Strings.format("unsupported parm type \"@\" for endpoint @.@", parmsTypes[i], cls.getSimpleName(), method.getName()));
			}

			handlers.put(name, jval -> {
				Object[] args = new Object[names.length];
				try {
					for (int i = 0; i < args.length; i++) {
						if(!jval.has(names[i])) return new ApiResponse(Strings.format("Wrong parms: no @ value (@)", names[i], parmsTypes[i].getSimpleName())).wrongParms();
						args[i] = extractors[i].get(jval, names[i]);
					}
					return new ApiResponse(method.invoke(null, args).toString());
				} catch (Exception e) {
					e.printStackTrace();
					Log.info("Call @ with parms: @", name, Arrays.toString(args));
					return new ApiResponse((e.getCause().getClass().getSimpleName()) + (e.getCause().getMessage() == null ? "" : ": " + e.getCause().getMessage())).serverError();
				}
			});
		}

		for (var field : fields) {
			var endpoint = field.getAnnotation(SseEndpoint.class);
			if(endpoint == null) continue;
			
			String name = enpointName(endpoint.value(), field.getName());
			
			Object source = null;
			try {
				source = field.get(null);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				Log.err(e);
				continue;
			}
			if(source == null) throw new RuntimeException(Strings.format("SSE source must be not null"));
			if(!(source instanceof SseSource sse)) throw new RuntimeException(Strings.format("SSE source must be @", SseSource.class));
			var sourceCls = source.getClass();
			
			Seq<Method> processors = Seq.with(sourceCls.getMethods()).select(m -> Modifier.isStatic(m.getModifiers()) && m.isAnnotationPresent(SseProcessor.class));
			if(processors.size != 1) throw new RuntimeException(Strings.format("SSE source have to contains only one processors method"));
			var method = processors.first();
			
			Log.info("SSE Endpoint: [cyan]/@/@", prefix, name);
			var parmsAnnotations = method.getParameterAnnotations();
			var parmsTypes = method.getParameterTypes();

			if(method.getReturnType() != Func.class) throw new RuntimeException(Strings.format("return type of endpoint @.@ must be String", cls.getSimpleName(), method.getName()));
			String[] names = new String[parmsTypes.length];
			Func2<Jval, String, Object>[] extractors = new Func2[parmsTypes.length];

			for (int i = 0; i < names.length; i++) {
				for (var a : parmsAnnotations[i]) if(a instanceof BodyField parm) names[i] = parm.value();
				if(names[i] == null) throw new RuntimeException(Strings.format("no @ annotation in parametr of endpoint @.@ must be String", BodyField.class.getSimpleName(), cls.getSimpleName(), method.getName()));
				extractors[i] = exractorFor(parmsTypes[i]);
				if(extractors[i] == null) throw new RuntimeException(Strings.format("unsupported parm type \"@\" for endpoint @.@", parmsTypes[i], cls.getSimpleName(), method.getName()));
			}

			sseHandlers.put(name, (exchange, jval) -> {
				Object[] args = new Object[names.length];
				try {
					for (int i = 0; i < args.length; i++) {
						if(!jval.has(names[i])) return new ApiResponse(Strings.format("Wrong parms: no @ value (@)", names[i], parmsTypes[i].getSimpleName())).wrongParms();
						args[i] = extractors[i].get(jval, names[i]);
					}
					@SuppressWarnings("rawtypes")
					Func func = (Func) method.invoke(null, args);
					sse.register(exchange, func);
					return null; // OK, continue request
				} catch (Exception e) {
					e.printStackTrace();
					return new ApiResponse((e.getCause().getClass().getSimpleName()) + (e.getCause().getMessage() == null ? "" : ": " + e.getCause().getMessage())).serverError();
				}
			});
		}
	}
	
	private String enpointName(String name, String method) {
		if(name.isEmpty()) return Strings.camelToKebab(method).replace('-', '/');
		return name;
	}
	
	private class ApiResponse {
		
		String content;
		int code = 200;

		public ApiResponse(String string) {
			this.content = string;
		}

		private ApiResponse wrongParms() {
			code = 400;
			return this;
		}
		private ApiResponse serverError() {
			code = 500;
			return this;
		}
		
	}

	private @Nullable Func2<Jval, String, Object> exractorFor(Class<?> clz) {
		if(clz == int.class) return (j,n) -> j.getInt(n, 0);
		if(clz == long.class) return (j,n) -> j.getLong(n, 0);

		if(clz == float.class) return (j,n) -> j.getFloat(n, 0f);
		if(clz == double.class) return (j,n) -> j.getDouble(n, 0);

		if(clz == boolean.class) return (j,n) -> j.getBool(n, false);
		
		if(clz == String.class) return (j,n) -> j.getString(n);
		
		return null;
	}

	public void register(HttpServer server) {

        server.createContext("/", exchange -> {
            String response = "Hello from secured localhost!";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        });
        
        handlers.each((path, handler) -> {
            server.createContext(Strings.format("/@/@", prefix, path), exchange -> {
            	readBody(exchange, jval -> {
                    var resp =  handler.get(jval);
                    
                    String response = resp.content;
                    exchange.sendResponseHeaders(resp.code, response.getBytes().length);
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                    handler.get(jval);
                    Log.info("< \"@\"", jval);
                    Log.info("> \"@\"", response);
        		});
            });
        });

        sseHandlers.each((path, handler) -> {			
        	server.createContext(Strings.format("/@/@", prefix, path), exchange -> {
        		readBody(exchange, jval -> {
        			var resp = handler.get(exchange, jval);
        			if(resp == null) return; // OK, stream started
                    String response = resp.content;
                    exchange.sendResponseHeaders(resp.code, response.getBytes().length);
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
        		});
        	});
        });
	}

	private interface BodyHandler {
		void get(Jval jval) throws IOException;
	}
	
	private void readBody(HttpExchange exchange, BodyHandler cons) throws IOException {
		if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
			String response = "Method not allowed";
			exchange.sendResponseHeaders(405, response.getBytes().length);
			exchange.getResponseBody().write(response.getBytes());
			exchange.getResponseBody().close();
			return;
		}
		var jval = Jval.read(new InputStreamReader(exchange.getRequestBody()));
		cons.get(jval);
	}

}
