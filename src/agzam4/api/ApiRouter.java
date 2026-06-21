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
import agzam4.api.ApiAnnotations.HeadField;
import agzam4.api.ApiAnnotations.PostEndpoint;
import agzam4.api.ApiAnnotations.SseEndpoint;
import agzam4.api.ApiAnnotations.SseProcessor;
import agzam4.utils.Log;
import agzam4proc.api.ApiSnippets.ApiResponse;
import arc.func.Func;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Reflect;
import arc.util.Strings;
import arc.util.serialization.Jval;


public class ApiRouter {

	public String prefix;
	
	public ApiRouter(Class<?> cls) {
		this(cls, cls.getSimpleName().toLowerCase().replaceAll("api", ""));
	}
	
	private interface Handler {
		
		ApiResponse get(HttpExchange exchange, Jval body);
		
	}

	private interface SseHandler {

		ApiResponse get(HttpExchange exchange, Jval body);

	}

	private interface Extracotor {

		Object get(HttpExchange e, Jval v) throws ApiResponse;

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

			if(method.getReturnType() != String.class) throw new RuntimeException(Strings.format("return type of endpoint @.@ must be String", cls.getSimpleName(), method.getName()));
			
			Extracotor[] extractors = generateExtractors(method);

			handlers.put(name, (exchange, jval) -> {
				Object[] args = new Object[extractors.length];
				try {
					for (int i = 0; i < args.length; i++) {
						args[i] = extractors[i].get(exchange, jval);
					}
					return new ApiResponse(method.invoke(null, args).toString());
				} catch (ApiResponse e) {
					return e;
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
			
			Object source = Reflect.get(field);
			if(source == null) throw new RuntimeException(Strings.format("SSE source must be not null"));
			if(!(source instanceof SseSource sse)) throw new RuntimeException(Strings.format("SSE source must be @", SseSource.class));
			var sourceCls = source.getClass();
			
			Seq<Method> processors = Seq.with(sourceCls.getMethods()).select(m -> m.isAnnotationPresent(SseProcessor.class));
			if(processors.size != 1) throw new RuntimeException(Strings.format("SSE source have to contains only one processors method"));
			var method = processors.first();
			
			Log.info("SSE Endpoint: [cyan]/@/@", prefix, name);

			if(method.getReturnType() != Func.class) throw new RuntimeException(Strings.format("return type of endpoint @.@ must be String", cls.getSimpleName(), method.getName()));

			Extracotor[] extractors = generateExtractors(method);

			sseHandlers.put(name, (exchange, jval) -> {
				Object[] args = new Object[extractors.length];
				try {
					for (int i = 0; i < args.length; i++) {
						args[i] = extractors[i].get(exchange, jval);
					}
					@SuppressWarnings("rawtypes")
					Func func = (Func) method.invoke(source, args);
					sse.register(exchange, func);
					return null; // OK, continue request
				} catch (ApiResponse e) {
					return e;
				} catch (Exception e) {
					e.printStackTrace();
					return new ApiResponse((e.getCause().getClass().getSimpleName()) + (e.getCause().getMessage() == null ? "" : ": " + e.getCause().getMessage())).serverError();
				}
			});
		}
	}
	
	private Extracotor[] generateExtractors(Method method) {
		var parmsAnnotations = method.getParameterAnnotations();
		var parmsTypes = method.getParameterTypes();
		
		Extracotor[] extractors =new Extracotor[parmsTypes.length];
		for (int i = 0; i < extractors.length; i++) {
			for (var a : parmsAnnotations[i]) {
				if(a instanceof BodyField parm) {
					extractors[i] = bodyExractorFor(parmsTypes[i], parm.value());
					break;
				}
				if(a instanceof HeadField parm) {
					extractors[i] = headExractorFor(parmsTypes[i], parm.value());
					break;
				}
			}
			if(extractors[i] == null) throw new RuntimeException(Strings.format("unsupported parm type \"@\" for endpoint @.@", parmsTypes[i], method.getDeclaringClass(), method.getName()));
		}
		return extractors;
	}

	private String enpointName(String name, String method) {
		if(name.isEmpty()) return Strings.camelToKebab(method).replace('-', '/');
		return name;
	}

	private @Nullable Extracotor bodyExractorFor(Class<?> clz, String n) {
		if(clz == int.class) return (e,j) -> {
			if(j.has(n)) return j.getInt(n, 0);
			throw new ApiResponse(Strings.format("Wrong body: no int value \"@\"", n)).wrongParms();
		};
		if(clz == long.class) return (e,j) -> {
			if(j.has(n)) return j.getLong(n, 0);
			throw new ApiResponse(Strings.format("Wrong body: no long value \"@\"", n)).wrongParms();
		};

		if (clz == float.class) return (e, j) -> {
		    if (j.has(n)) return j.getFloat(n, 0f);
		    throw new ApiResponse(Strings.format("Wrong body: no float value \"@\"", n)).wrongParms();
		};
		if (clz == double.class) return (e, j) -> {
		    if (j.has(n)) return j.getDouble(n, 0d);
		    throw new ApiResponse(Strings.format("Wrong body: no double value \"@\"", n)).wrongParms();
		};

		if (clz == boolean.class) return (e, j) -> {
		    if (j.has(n)) return j.getBool(n, false);
		    throw new ApiResponse(Strings.format("Wrong body: no boolean value \"@\"", n)).wrongParms();
		};

		if (clz == String.class) return (e, j) -> {
		    if (j.has(n)) return j.getString(n);
		    throw new ApiResponse(Strings.format("Wrong body: no String value \"@\"", n)).wrongParms();
		};
		return null;
	}

	private String header(HttpExchange e, String header) {
		var h = e.getRequestHeaders().getFirst("Agzam4-" + header);
		if(h == null)  new ApiResponse(Strings.format("Wrong header: no \"Agzam4-@\" header", header)).wrongParms();
		return h;
	}
	
	private @Nullable Extracotor headExractorFor(Class<?> clz, String n) {
		if(clz == int.class) return (e,j) -> Strings.parseInt(header(e, n), 0);
		if(clz == long.class) return (e,j) -> Strings.parseLong(header(e, n), 0);

		if(clz == float.class) return (e,j) -> Strings.parseFloat(header(e, n), 0);
		if(clz == double.class) return (e,j) -> Strings.parseDouble(header(e, n), 0);

		if(clz == boolean.class) return (e,j) -> Boolean.valueOf(header(e, n));
		
		if(clz == String.class) return (e,j) -> header(e, n);
		
		return null;
	}

	public void register(HttpServer server) {
        handlers.each((path, handler) -> {
            server.createContext(Strings.format("/@/@", prefix, path), exchange -> {
            	readBody(exchange, jval -> {
                    var resp = handler.get(exchange, jval);
                    
                    String response = resp.content;
                    exchange.sendResponseHeaders(resp.code, response.getBytes().length);
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                    handler.get(exchange, jval);
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
