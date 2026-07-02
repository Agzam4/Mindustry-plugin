package agzam4proc.api.utils;

import java.lang.annotation.*;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.*;

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

	public TypeSpec build() {
		final TypeName jval = TypeName.get(Jval.class);

		ObjectSet<TypeName> valueOf = ObjectSet.with(Seq.<Class<?>>with(
				int.class, long.class, float.class, boolean.class,
				Integer.class, Long.class, Float.class, Boolean.class,
				String.class
				).map(c -> TypeName.get(c)));
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
					if(!valueOf.contains(f.type.typeName)) throw f.error("Unsupported field type: " + f.type);
					method.addStatement("json.add($S, $T.valueOf($L.$L))", f.name, jval, _parm, f.name);
				});
			} else {
				var _i = method.namespace.get("i");
				method.addCode("for(int $L = 0; $L < $L.length; $L++)\n$>", _i, _i, _parm, _i);
				method.addStatement("json.add(json($L[$L]))$<", _parm, _i);
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
			String name = f.name;
			if(valueOf.contains(f.type.typeName)) {
				var _parm = method.addParm(name, f.type).name;
				method.addStatement("$L.add($S, $T.valueOf($L))", _json, _parm, jval, _parm);
				return;
			}
			throw f.error("Unsupported field type: " + f.type);
		});
		method.addStatement("return $L.toString($T.plain)", _json, TypeName.get(Jformat.class));
		

		return builder.build();
	}
	
}
