package agzam4.database.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.fasterxml.jackson.annotation.JsonProperty;

import agzam4.Log;
import agzam4.database.Users;
import agzam4.database.Users.UserEntity;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Nullable;
import arc.util.io.Streams;
import arc.util.serialization.BaseJsonReader;
import arc.util.serialization.BaseJsonWriter;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonWriter;
import arc.util.serialization.SerializationException;
import arc.util.serialization.JsonWriter.OutputType;

@Deprecated
public class Json  {

//	private BaseJsonWriter writer;
//	
//	public void toJson(Object object, Writer writer) throws IllegalArgumentException, IllegalAccessException, IOException {
//		toJson(object, new JsonWriter(writer));
//		Streams.close(writer);
//	}
//	
//	public void toJson(Object object, JsonWriter writer) throws IllegalArgumentException, IllegalAccessException, IOException {
//		writer.setOutputType(OutputType.json);
//		writer.setQuoteLongValues(true);
//		this.writer = writer;
//		writeValue(null, object);
//	}
//	
//
//    public <T> T fromJson(Class<T> type, String string){
//		var value = new JsonReader().parse(string);
//		Log.info("Value: [blue]@[]", value);
//		
//		Log.info("Value: [blue]@[]", value);
//		if(value.isArray()) {
//
//		}
//		return null;
//	}
//
////    public <T> T readValue(Object target, String name, arc.util.serialization.JsonValue value){
////		Log.info("Value: [blue]@[]", value);
////		var type = target.getClass();
////		value.get(name);
////		if(value.isArray()) {
////            if(Seq.class.isAssignableFrom(type)){
////            	
////            }
////            
////            throw new ArcRuntimeException("Unsupported type: " + type);
////		}
////		return null;
////	}
//    
//    private void readFields(JsonValue src, Object object) throws IOException, IllegalArgumentException, IllegalAccessException {
//		for (var f : object.getClass().getFields()) {
//			@Nullable JsonProp prop = f.getAnnotation(JsonProp.class);
//			if(prop == null) continue;
//			String name = prop.value();
//			if(name == null || name.isEmpty()) name = f.getName();
//			f.setAccessible(true);
//			readValue(src, name, f);
//		}
//	}
//	
//    private void readValue(JsonValue src, @Nullable String name, Field f) throws IOException, IllegalArgumentException, IllegalAccessException {
//		if(Iterable.class.isAssignableFrom(f.getType())) {
//			var value = src.get(name);
//			if(Seq.class.isAssignableFrom(f.getType())) {
//				var seq = new Seq<>(value.size);
//				for (var i : value) {
//					
//				}
//			}
////			writer.array();
////			if(name != null) writer.name(name);
////			for (var iterator = array.iterator(); iterator.hasNext();) {
////				writeValue(null, iterator.next());
////			}
////			writer.pop();
//			return;
//		}
//		if(name == null) {
//			writer.object();
//			writeFields(value);
//			writer.pop();	
//			return;
//		}
//		
//		if(value.getClass().isPrimitive() || value.getClass().equals(String.class) || value.getClass().getPackageName().startsWith("java.lang")) {
//			writer.name(name);
//			writer.value(value);
//			return;
//		}
//		
//		writer.object();
//		writer.name(name);
//		writeFields(value);
//		writer.pop();	
//	}
//    
//    
//	@SuppressWarnings("unchecked")
//	protected <T> T newInstance(Class<T> type){
//	        try{
//	            return type.getDeclaredConstructor().newInstance();
//	        }catch(Exception ex){
//	            try{
//	                // Try a private constructor.
//	                Constructor<T> constructor = type.getDeclaredConstructor();
//	                constructor.setAccessible(true);
//	                return constructor.newInstance();
//	            }catch(SecurityException ignored){
//	            }catch(IllegalAccessException ignored){
//	                if(Enum.class.isAssignableFrom(type)){
//	                    if(type.getEnumConstants() == null) type = (Class<T>) type.getSuperclass();
//	                    return type.getEnumConstants()[0];
//	                }
//	                if(type.isArray())
//	                    throw new SerializationException("Encountered JSON object when expected array of type: " + type.getName(), ex);
//	                else if(type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))
//	                    throw new SerializationException("Class cannot be created (non-static member class): " + type.getName(), ex);
//	                else
//	                    throw new SerializationException("Class cannot be created (missing no-arg constructor): " + type.getName(), ex);
//	            }catch(Exception privateConstructorException){
//	                ex = privateConstructorException;
//	            }
//	            throw new SerializationException("Error constructing instance of class: " + type.getName(), ex);
//	        }
//	    }
//	
//	private void writeFields(Object object) throws IOException, IllegalArgumentException, IllegalAccessException {
//		for (var f : object.getClass().getFields()) {
//			@Nullable JsonProp prop = f.getAnnotation(JsonProp.class);
//			if(prop == null) continue;
//			String name = prop.value();
//			if(name == null || name.isEmpty()) name = f.getName();
//			f.setAccessible(true);
//			Object value = f.get(object);
//			writeValue(name, value);
//		}
//	}
//	
//
//	private void writeValue(@Nullable String name, Object value) throws IOException, IllegalArgumentException, IllegalAccessException {
//		if(value instanceof Iterable<?> array) {
//			writer.array();
//			if(name != null) writer.name(name);
//			for (var iterator = array.iterator(); iterator.hasNext();) {
//				writeValue(null, iterator.next());
//			}
//			writer.pop();
//			return;
//		}
//		if(name == null) {
//			writer.object();
//			writeFields(value);
//			writer.pop();	
//			return;
//		}
//		
//		if(value.getClass().isPrimitive() || value.getClass().equals(String.class) || value.getClass().getPackageName().startsWith("java.lang")) {
//			writer.name(name);
//			writer.value(value);
//			return;
//		}
//		
//		writer.object();
//		writer.name(name);
//		writeFields(value);
//		writer.pop();	
//	}
//
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Target(ElementType.FIELD)
//	public static @interface JsonProp {
//		
//		public String value() default "";
//		
//	}


	
}
