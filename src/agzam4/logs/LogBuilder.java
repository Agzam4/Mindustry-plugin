package agzam4.logs;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import agzam4.api.auth.SensitiveData;
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

public class LogBuilder<T> {
	
	private static final ObjectMap<Class<?>, JsonTypes> typeMap = ObjectMap.of();

	static {
		for (var jt: JsonTypes.values()) {
			for (int i = 0; i < jt.types.length; i++) typeMap.put(jt.types[i], jt);
		}
//		Log.info("[cyan]Type map: @", typeMap);
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
	
	
	public final Class<?> cls;
	
	interface Extractor<T> {

	    Object get(T param);
	
	}

	private Extractor<T>[] extractors;
	private Object[] extracted;
	private JsonTypes[] types;
	private boolean[] sensitive;
	private String[] sensitiveTypes;
	private String[] precomputedHeaders; // "name":
	public final int id;
	private String[] names;

	public LogBuilder(Class<T> cls, int id) {
		this.cls = cls;
		this.id = id;

        Lookup lookup = MethodHandles.lookup();
        
		StringBuilder part = new StringBuilder();

		Seq<String> names = new Seq<>();
		Seq<String> parts = new Seq<>();
		Seq<JsonTypes> types = new Seq<>();
		Seq<Extractor<T>> extractors = new Seq<>();
		
		Seq<Field> fileds = Seq.with(cls.getDeclaredFields());
		Seq<String> sensitiveTypesSeq = new Seq<>();
		
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
				sensitiveTypesSeq.add(sensitive != null ? sensitive.value().name() : null);
				
				part.append('"').append(name).append("\":");
				parts.add(part.toString());
				part.setLength(0);
				names.add(name);
			} catch (IllegalAccessException e) {
				Log.err(e);
			}
		}

		this.precomputedHeaders = parts.toArray(String.class);
		this.names = names.toArray(String.class);
		this.extractors = extractors.toArray(Extractor.class);
		this.sensitive = new boolean[sensitiveTypesSeq.size];
		this.sensitiveTypes = new String[sensitiveTypesSeq.size];
		for (int i = 0; i < sensitive.length; i++) {
			sensitive[i] = sensitiveTypesSeq.get(i) != null;
			sensitiveTypes[i] = sensitiveTypesSeq.get(i);
		}
		this.extracted = new Object[extractors.size];
		this.types = types.toArray(JsonTypes.class);
	}
	
	
	public String build(T t) {
		int size = 1; // "{}" + no first ","
		for (int i = 0; i < extractors.length; i++) {
			Object val = extractors[i].get(t);
			if(val == null) continue;
//			if(sensitive[i]) {
//				val = SensitiveData.insertOrGet((String) val, sensitiveTypes[i]);
//			}
			extracted[i] = val;
			size += precomputedHeaders[i].length() + 1; // part + ","
			size += types[i].buffer.get(val);
		}
		
		StringBuilder json = new StringBuilder(size);
		json.append('{');
		for (int i = 0; i < extracted.length; i++) {
			if(extracted[i] == null) continue;
			if(json.length() != 1) json.append(',');
			json.append(precomputedHeaders[i]);
			if(sensitive[i]) {
				json.append((int) extracted[i]); // number, no quotes
			} else {
				types[i].func.get(extracted[i], json);
			}
		}
		json.append("}");
		
		return json.toString();
	}
	
    private static final ObjectMap<Class<?>, Func<Jval, Object>> cast = create();
	
	private static ObjectMap<Class<?>, Func<Jval, Object>> create() {
		ObjectMap<Class<?>, Func<Jval, Object>> m = ObjectMap.of();
		m.put(Boolean.class, j -> j.asBool());
		m.put(boolean.class, j -> j.asBool());
		m.put(Integer.class, j -> j.asInt());
		m.put(int.class, j -> j.asInt());
		m.put(Long.class, j -> j.asLong());
		m.put(long.class, j -> j.asLong());
		m.put(Short.class, j -> (short)j.asInt());
		m.put(short.class, j -> (short)j.asInt());
		m.put(Byte.class, j -> (byte)j.asInt());
		m.put(byte.class, j -> (byte)j.asInt());

		m.put(Double.class, j -> j.asDouble());
		m.put(double.class, j -> j.asDouble());
		m.put(Float.class, j -> j.asFloat());
		m.put(float.class, j -> j.asFloat());

		m.put(Character.class, j -> {
			var s = j.asString();
			return s.isEmpty() ? '\0' : s.charAt(0);
		});
		m.put(String.class, j -> j.asString());
		return m;
	}


	public T read(String json) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException {
		var c = cls.getDeclaredConstructor();
		c.setAccessible(true);
		@SuppressWarnings("unchecked")
		T event = (T) c.newInstance();
		var jval = Jval.read(json);
		for (int i = 0; i < names.length; i++) {
			var f = cls.getField(names[i]);
			f.set(event, cast.get(f.getType()).get(jval.get(names[i])));
		}
		return event;
	}
	
}
