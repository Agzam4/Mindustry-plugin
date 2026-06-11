package agzam4.api;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import com.sun.net.httpserver.HttpServer;

import agzam4.api.ApiAnnotations.BodyField;
import agzam4.api.ApiAnnotations.PostEndpoint;
import agzam4.utils.Log;
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
	
	private ObjectMap<String, Handler> handlers = ObjectMap.of();

	public ApiRouter(Class<?> cls, String prefix) {
		Log.info("== Class: @ ==", cls.getSimpleName());
		this.prefix = prefix;
		
		Seq<Method> methods = Seq.with(cls.getMethods()).select(m -> Modifier.isStatic(m.getModifiers()));
		
		for (var method : methods) {
			var endpoint = method.getAnnotation(PostEndpoint.class);
			if(endpoint == null) continue;
			String name = endpoint.value().isEmpty() ? Strings.camelToKebab(method.getName()).replace('-', '/') : endpoint.value();
			
			Log.info("Endpoint: [cyan]/@/@", prefix, name);

	        var parmsAnnotations = method.getParameterAnnotations();
	        var parmsTypes = method.getParameterTypes();
	        
	        if(method.getReturnType() != String.class) throw new RuntimeException(Strings.format("return type of endpoint @.@ must be String", cls.getSimpleName(), method.getName()));
	        String[] names = new String[parmsTypes.length];
	        @SuppressWarnings("unchecked")
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
	        			if(jval.has(name)) return new ApiResponse(Strings.format("Wrong parms: no @ value (@)", name, parmsTypes[i].getSimpleName())).wrongParms();
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
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
//                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    String response = "Hello from secured localhost!";
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }
                
                var jval = Jval.read(new InputStreamReader(exchange.getRequestBody()));
                
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

//        try {
//            // 2. Читаем все байты из тела запроса
//            java.io.InputStream inputStream = exchange.getRequestBody();
//            byte[] bytes = inputStream.readAllBytes();
//            String body = new String(bytes, "UTF-8");
//
//            // 3. Парсим JSON с помощью встроенного в Arc класса Jval
//            arc.util.serialization.Jval json = arc.util.serialization.Jval.read(body);
//
//            // Пример: получаем строку "action" и число "amount" из JSON
//            // { "action": "spawn", "amount": 5 }
//            String action = json.getString("action", "unknown");
//            int amount = json.getInt("amount", 1);
//
//            Log.info("[PluginServer] Получена команда: @, количество: @", action, amount);
//
//            // ВАЖНО: Если вы меняете мир игры (спавните юнитов, даете ресурсы),
//            // это нужно делать строго в главном потоке Mindustry!
//            arc.Core.app.post(() -> {
//                // Код, изменяющий мир игры, пишется здесь
//                if (action.equals("spawn")) {
//                    // mindustry.gen.UnitTypes.dagger.spawn(...)
//                }
//            });
//
//            // 4. Отправляем успешный ответ
//            String response = "{\"success\":true}";
//            byte[] responseBytes = response.getBytes("UTF-8");
//            exchange.getResponseHeaders().set("Content-Type", "application/json");
//            exchange.sendResponseHeaders(200, responseBytes.length);
//            exchange.getResponseBody().write(responseBytes);
//
//        } catch (Exception e) {
//            Log.err("[PluginServer] Ошибка обработки POST-запроса", e);
//            
//            // Если прислали невалидный JSON или упала ошибка
//            String errorResponse = "{\"error\":\"Invalid request\"}";
//            byte[] errorBytes = errorResponse.getBytes("UTF-8");
//            exchange.getResponseHeaders().set("Content-Type", "application/json");
//            exchange.sendResponseHeaders(400, errorBytes.length);
//            exchange.getResponseBody().write(errorBytes);
//        } finally {
//            exchange.getResponseBody().close();
//        }
	}
	
}
