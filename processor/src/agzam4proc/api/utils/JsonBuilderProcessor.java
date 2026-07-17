package agzam4proc.api.utils;

import java.lang.annotation.*;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.*;

import agzam4proc.BaseProcessor;
import agzam4proc.api.RouterProcessor;
import agzam4proc.api.utils.element.*;
import arc.struct.*;
import arc.util.Log;
import arc.util.serialization.Jval;
import arc.util.serialization.Jval.Jformat;

public class JsonBuilderProcessor {

	private static int maxArrayDimension = 3;
	
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.METHOD)
	public @interface GeneratedJsonBuilder {
		Class<?> value();
	}

	public static final ObjectMap<TypeElem, JsonBuilderProcessor> builders = ObjectMap.of();
//	public static final ObjectMap<TypeElem, JsonBuilderProcessor> builderByType = ObjectMap.of();
	
	public static JsonBuilderProcessor builder(String packageName, TypeInfo info) {
		if(builders.containsKey(info.type)) return builders.get(info.type);
		var builder = new JsonBuilderProcessor(packageName, info);
		builders.put(info.type, builder);
		return builder;
	}

	public final TypeInfo info;
	public final TypeElem type, builder;
	public ExecutableElem[] stringMethod = new ExecutableElem[maxArrayDimension+1]; // T, T[], T[][]
	public String packageName;
	
	public JsonBuilderProcessor(String packageName, TypeInfo info) {
		this.info = info;
		this.type = info.type;
		this.packageName = packageName;
		this.builder = TypeElem.of(packageName, type.name + "JsonBuilder");
		Log.info(" - @", info.type);
	}
	
	boolean written = false;

	
	private void buildAddToJson(ExecutableElem method, VariableElem f, String _json, String _parm) {
		final TypeName jval = TypeName.get(Jval.class);
		final ObjectSet<TypeName> valueOf = ObjectSet.with(Seq.<Class<?>>with(
				int.class, long.class, float.class, boolean.class,
				Integer.class, Long.class, Float.class, Boolean.class,
				String.class
				).map(c -> TypeName.get(c)));
		
		if(f.type.dimension() == 0) {
			if(!valueOf.contains(f.type.typeName)) throw f.error("Unsupported field type: " + f.type);
			method.addStatement("$L.add($S, $T.valueOf($L))", _json, f.name, jval, _parm);
			return;
		}
		// TODO: any n-dimension arrays (primitive + typed)
		// Primitive 1d arrays
		if(f.type.dimension() == 1) {
			if(!valueOf.contains(f.type.noDimension().typeName)) throw f.error("Unsupported field type: " + f.type.noDimension().typeName);
			var _array = method.namespace.get(f.name + "Array");
			var _i = method.namespace.get("i");

			method.addCode("if($L != null){\n$>", _parm);
			
			method.addStatement("var $L = $T.newArray()", _array, jval);
			
			method.addCode("for(int $L = 0; $L < $L.length; $L++)\n$>", _i, _i, _parm, _i);
			method.addStatement("$L.add($L[$L])$<", _array, _parm, _i);
			
			method.addStatement("$L.add($S, $L)", _json, f.name, _array);

			method.addCode("$<}\n");
			return;
		}
		throw f.error("Unsupported field dimension: " + f.type.dimension());
	}
	
	public void write(String writeToPackage, BaseProcessor processor) {
		final TypeName jval = TypeName.get(Jval.class);

		String doc = null;// TODO: type.processingEnv.getElementUtils().getDocComment(type);
		

		// Method "json"
		for (int dimension = 0; dimension <= maxArrayDimension; dimension++) {
			ExecutableElem method = ExecutableElem.virtual("json", TypeElem.of(ClassName.get(Jval.class)), builder.typeName);
			method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
			builder.addMethod(method);

			var _parm = method.addParm(dimension == 0 ? "object" : "array", TypeElem.arrayOf(type, dimension)).name;
			method.addStatement("if($L == null) return $T.NULL", _parm, jval);
			
			String _json = method.namespace.get("json");
			method.addStatement("var $L = $T.$L()", _json, TypeName.get(Jval.class), dimension == 0 ? "newObject" : "newArray");
			if(dimension == 0) {
				info.eachfield(f -> {
					buildAddToJson(method, f, _json, _parm + "." + f.name);
				});
			} else {
				var _i = method.namespace.get("i");
				method.addCode("for(int $L = 0; $L < $L.length; $L++)\n$>", _i, _i, _parm, _i);
				method.addStatement("json.add($L($L[$L]))$<", _json, _parm, _i);
			}
			method.addStatement("return $L", _json);
		}
		
		// Method "string"
		for (int dimension = 0; dimension <= maxArrayDimension; dimension++) {
			ExecutableElem method = ExecutableElem.virtual("string", TypeElem.of(ClassName.get(String.class)), builder.typeName);
			method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
			builder.addMethod(method);
			var _parm = method.addParm(dimension == 0 ? "object" : "array", TypeElem.arrayOf(type, dimension)).name;
			method.addStatement("return json($L).toString($T.plain)", _parm, TypeName.get(Jformat.class));
			stringMethod[dimension] = method;
		}
		
		// Method "of"
		ExecutableElem method = ExecutableElem.virtual("of", TypeElem.of(ClassName.get(String.class)), builder.typeName);
		method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
		builder.addMethod(method);
		String _json = method.namespace.get("json");
		method.addStatement("var $L = $T.newObject()", _json, TypeName.get(Jval.class));

		info.eachfield(f -> {
			var _parm = method.addParm(f.name, f.type).name;
			buildAddToJson(method, f, _json, _parm);
		});
		method.addStatement("return $L.toString($T.plain)", _json, TypeName.get(Jformat.class));
		
		processor.write(writeToPackage, builder.build());
		written = true;
	}
	
}
