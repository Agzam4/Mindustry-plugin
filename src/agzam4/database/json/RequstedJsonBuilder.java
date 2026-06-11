package agzam4.database.json;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import agzam4.utils.Strs;
import arc.func.Cons2;
import arc.func.Func2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;

public class RequstedJsonBuilder<T> {
	
	private static final ObjectMap<Class<?>, JsonTypes> typeMap = ObjectMap.of();
	
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
		string((o, s) -> s.append('"').append(Strs.escape(o.toString())).append('"'), o -> o.toString().length() + 6, Strings.class);
		
		public final SizeFunc buffer;
		public final Cons2<Object, StringBuilder> func;
		
		private JsonTypes(Cons2<Object, StringBuilder> func, SizeFunc buffer, Class<?> ...types) {
			for (int i = 0; i < types.length; i++) typeMap.put(types[i], this);
			this.buffer = buffer;
			this.func = func;
		}
		
	}
	
	public final Class<?> cls;

	private Func2<JsonRequest, T, Object>[] extractors;
	private Object[] extracted;
	private JsonTypes[] types;
	private String[] precomputedHeaders; // "name":
	
	public RequstedJsonBuilder(Class<T> cls) {
		this.cls = cls;

        Lookup lookup = MethodHandles.lookup();
        
		StringBuilder part = new StringBuilder();
		
		Seq<String> parts = new Seq<>();
		Seq<JsonTypes> types = new Seq<>();
		Seq<Func2<JsonRequest, T, Object>> extractors = new Seq<>();
		Seq<Func2<JsonRequest, T, String>> parms = new Seq<>();
		
		Seq<Field> fileds = Seq.with(cls.getDeclaredFields());
		
		for (var f : fileds) {
			@Nullable RequestedJsonProp prop = f.getDeclaredAnnotation(RequestedJsonProp.class);
			if(prop == null) continue;
			JsonTypes type = typeMap.get(f.getType());
			if(type == null) {
				Log.warn("Unknow type \"@\" for json parm \"@\"", f.getType(), f.getName());
				continue;
			}
			
			try {
				MethodHandle getter = lookup.unreflectGetter(f);
				extractors.add((r,o) -> {
					try {
						return prop.value().conv.get(r, getter.invoke(o));
					} catch (Throwable e) {
						Log.err(e);
					}
					return null;
				});
				types.add(type);
				
				part.append('"').append(f.getName()).append("\":");
				parts.add(part.toString());
				part.setLength(0);
				
				parms.add((r, v) -> {
					// TODO
					
					return null;
				});
			} catch (IllegalAccessException e) {
				Log.err(e);
			}
		}
		
		this.precomputedHeaders = parts.toArray();
		this.extractors = extractors.toArray();
		this.extracted = new Object[extractors.size];
	}
	
	
	public String build(JsonRequest request, T t) {
		int size = 1; // "{}" + no first ","
		for (int i = 0; i < extractors.length; i++) {
			extracted[i] = extractors[i].get(request, t);
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
	
}
