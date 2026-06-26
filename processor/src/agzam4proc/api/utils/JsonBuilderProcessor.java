package agzam4proc.api.utils;

import java.lang.annotation.*;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.*;

import agzam4proc.api.utils.element.TypeElem;
import arc.struct.*;
import arc.util.serialization.Jval;
import arc.util.serialization.Jval.Jformat;

public class JsonBuilderProcessor {

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
	public MethodInfo toString;
	public String packageName;
	
	public JsonBuilderProcessor(String packageName, TypeInfo info) {
		this.info = info;
		this.type = info.type;
		this.packageName = packageName;
		this.builder = TypeElem.of(packageName, type.name + "JsonBuilder");
	}

	public TypeSpec build() {
		final TypeName jval = TypeName.get(Jval.class);
	    
		ObjectSet<TypeName> valueOf = ObjectSet.with(Seq.<Class<?>>with(int.class, long.class, float.class, String.class, boolean.class).map(c -> TypeName.get(c)));
		String doc = null;// TODO: type.processingEnv.getElementUtils().getDocComment(type);
		

		MethodSpec.Builder json = MethodSpec.methodBuilder("json")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(jval)
				.addParameter(type.typeName, "object");

		MethodSpec.Builder of = MethodSpec.methodBuilder("of")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(jval);
		
		Namespace argsNamespace = new Namespace();
		info.eachfield(f -> argsNamespace.get(f.name));
		String varname = argsNamespace.get("json");
		
		json.addStatement("var json = $T.newObject()", TypeName.get(Jval.class));
		of.addStatement("var $L = $T.newObject()", varname, TypeName.get(Jval.class));
		
		info.eachfield(f -> {
			String name = f.name;
			if(valueOf.contains(f.type.typeName)) {
				json.addStatement("json.add($S, $T.valueOf(object.$L))", name, jval, name);
				of.addParameter(f.type.typeName, name);
				of.addStatement("$L.add($S, $T.valueOf($L))", varname, name, jval, name);
				return;
			}
			throw f.error("Unsupported field type: " + f.type);
		});
		

		json.addStatement("return json");
		of.addStatement("return json");
		
		MethodSpec.Builder string = MethodSpec.methodBuilder("string")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//				.addAnnotation(GeneratedJsonBuilder.class)
				.addAnnotation(AnnotationSpec.builder(GeneratedJsonBuilder.class)
						.addMember("value", "$T.class", type.typeName)
						.build())
				.returns(String.class)
				.addParameter(type.typeName, "object");
		string.addStatement("return json(object).toString($T.plain)", TypeName.get(Jformat.class));
		
		return TypeSpec.classBuilder(builder.name)
				.addJavadoc("Auto-generated annotation based on {@link $T}$L", this.info.type.typeName, doc == null ? "" : "<br>\n<br>\n" + doc.trim())
				.addModifiers(Modifier.PUBLIC)
				.addMethod(json.build())
				.addMethod(of.build())
				.addMethod(string.build())
				.build();
		
		
//		CodeBlock block
	}
	
}
