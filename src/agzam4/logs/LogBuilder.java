package agzam4.logs;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;

import agzam4.logs.LogsAnnotations.JsonProp;
import agzam4.logs.LogsAnnotations.Sensitive;
import agzam4.utils.Log;
import agzam4.utils.Strs;
import arc.func.Cons2;
import arc.func.Func;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.serialization.Jval;
import arc.util.serialization.Jval.Jformat;

public class LogBuilder<T> {
	
	private static final ObjectMap<Class<?>, JsonTypes> typeMap = ObjectMap.of();

	static {
		for (var jt: JsonTypes.values()) {
			for (int i = 0; i < jt.types.length; i++) typeMap.put(jt.types[i], jt);
		}
		Log.info("[cyan]Type map: @", typeMap);
	}
	
	private interface SizeFunc { int get(Object o); }
	
	private enum JsonTypes {

		bool((o, s) -> s.append(o), o -> 5, boolean.class, Boolean.class),
		number((o, s) -> s.append(o), o -> 0, 
				int.class, Integer.class, 
				long.class, Long.class,
	            float.class, Float.class, 
	            double.class, Double.class,
	            byte.class, Byte.class,
	            short.class, Short.class
	       ),
		character((o, s) -> s.append('"').append(Strs.getEscapedChar((char)o)).append('"'), o -> 4, char.class, Character.class),
		string((o, s) -> s.append('"').append(Strs.escape(o.toString())).append('"'), o -> o.toString().length() + 6, String.class);
		
		public final SizeFunc buffer;
		public final Cons2<Object, StringBuilder> func;
		private Class<?>[] types;
		
		private JsonTypes(Cons2<Object, StringBuilder> func, SizeFunc buffer, Class<?> ...types) {
			this.types = types;
			this.buffer = buffer;
			this.func = func;
		}
		
	}
	
	
	private class Protector {
		
		String name;
		Func<String, String> protector;
		
		public Protector(String name, Func<String, String> protector) {
			this.name = name;
			this.protector = protector;
		}
		
	}
	
	public final Class<?> cls;
	
	interface Extractor<T> {

	    Object get(T param);
	
	}

	private Extractor<T>[] extractors;
	private Object[] extracted;
	private JsonTypes[] types;
	private String[] precomputedHeaders; // "name":
	private Protector[] protectors;
	public final int id;

	public LogBuilder(Class<T> cls, int id) {
		this.cls = cls;
		this.id = id;

        Lookup lookup = MethodHandles.lookup();
        
		StringBuilder part = new StringBuilder();
		
		Seq<String> parts = new Seq<>();
		Seq<JsonTypes> types = new Seq<>();
		Seq<Extractor<T>> extractors = new Seq<>();
		
		Seq<Field> fileds = Seq.with(cls.getDeclaredFields());
		Seq<Protector> protectors = new Seq<>();
		
		for (var f : fileds) {
			@Nullable JsonProp prop = f.getDeclaredAnnotation(JsonProp.class);
			String name = f.getName();
			if(prop == null) continue;
			JsonTypes type = typeMap.get(f.getType());
			if(type == null) {
				Log.warn("Unknow type \"@\" for json parm \"@\"", f.getType(), name);
				continue;
			}
			
			try {
				MethodHandle getter = lookup.unreflectGetter(f);
				extractors.add(o -> {
					try {
						return getter.invoke(o);
					} catch (Throwable e) {
						Log.err(e);
					}
					return null;
				});
				types.add(type);

				@Nullable Sensitive sensitive = f.getDeclaredAnnotation(Sensitive.class);
				if(sensitive != null) {
					var protector = sensitive.value();
					protectors.add(new Protector(name, protector.func));
				}
				
				part.append('"').append(name).append("\":");
				parts.add(part.toString());
				part.setLength(0);
			} catch (IllegalAccessException e) {
				Log.err(e);
			}
		}
		
		this.precomputedHeaders = parts.toArray(String.class);
		this.extractors = extractors.toArray(Extractor.class);
		this.protectors = protectors.toArray(Protector.class);
		this.extracted = new Object[extractors.size];
		this.types = types.toArray(JsonTypes.class);
	}
	
	
	public String build(T t) {
		int size = 1; // "{}" + no first ","
		for (int i = 0; i < extractors.length; i++) {
			extracted[i] = extractors[i].get(t);
			Log.info("#@: @", i, extracted[i]);
			if(extracted[i] == null) continue;
			size += precomputedHeaders[i].length() + 1; // part + ","
			size += types[i].buffer.get(extracted[i]);
		}
		
		StringBuilder json = new StringBuilder(size);
		json.append('{');
		for (int i = 0; i < extracted.length; i++) {
			if(extracted[i] == null) continue;
			if(json.length() != 1) json.append(',');
			json.append(precomputedHeaders[i]);
			types[i].func.get(extracted[i], json);
		}
		json.append("}");
		
		return json.toString();
	}
	
	public String protect(String json) {
		var val = Jval.read(json);
		for (int i = 0; i < protectors.length; i++) {
			String protectedData = protectors[i].protector.get(val.getString(protectors[i].name));
			val.put(protectors[i].name, protectedData);
		}
		return val.toString(Jformat.plain);
	}
	
	
}
