package agzam4proc.api.utils;

import java.lang.annotation.ElementType;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.sun.net.httpserver.HttpServer;

import agzam4proc.AptError;
import agzam4proc.Proc;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.serialization.Jval;

public class JsonBuilderProcessor {

	public static final ObjectMap<TypeInfo, JsonBuilderProcessor> builders = ObjectMap.of();
	
	public static JsonBuilderProcessor builder(TypeInfo info) {
		if(builders.containsKey(info)) return builders.get(info);
		var builder = new JsonBuilderProcessor(info);
		builders.put(info, builder);
		return builder;
	}

	public final TypeInfo type;
	public final String name;
	
	public JsonBuilderProcessor(TypeInfo type) {
		this.type = type;
		this.name = type.type.getSimpleName() + "JsonBuilder";
	}
	
	
	public TypeSpec build() {
		final TypeName jval = TypeName.get(Jval.class);
	    
		ObjectSet<TypeName> valueOf = ObjectSet.with(Seq.<Class<?>>with(int.class, long.class, float.class, String.class, boolean.class).map(c -> TypeName.get(c)));
		String doc = null;// TODO: type.processingEnv.getElementUtils().getDocComment(type);
		

		MethodSpec.Builder json = MethodSpec.methodBuilder("json")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(jval)
				.addParameter(ClassName.get(type.type), "object");

		MethodSpec.Builder of = MethodSpec.methodBuilder("of")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(jval);
		
		Namespace argsNamespace = new Namespace();
		type.eachfield(f -> argsNamespace.get(f.getSimpleName().toString()));
		String varname = argsNamespace.get("json");
		
		json.addStatement("var json = $T.newObject()", TypeName.get(Jval.class));
		of.addStatement("var $L = $T.newObject()", varname, TypeName.get(Jval.class));
		
		type.eachfield(f -> {
			String name = f.getSimpleName().toString();
			if(valueOf.contains(TypeName.get(f.asType()))) {
				json.addStatement("json.add($S, $T.valueOf(object.$L))", name, jval, name);
				of.addParameter(TypeName.get(f.asType()), name);
				of.addStatement("$L.add($S, $T.valueOf($L))", varname, name, jval, name);
				return;
			}
			throw new AptError(f, "Unsupported field type: " + TypeName.get(f.asType()));
		});
		

		json.addStatement("return json");
		of.addStatement("return json");
		
		return TypeSpec.classBuilder(name)
				.addJavadoc("Auto-generated annotation based on {@link $T}$L", this.type.type, doc == null ? "" : "<br>\n<br>\n" + doc.trim())
				.addModifiers(Modifier.PUBLIC)
				.addMethod(json.build())
				.addMethod(of.build())
				.build();
		
		
//		CodeBlock block
	}
	
}
