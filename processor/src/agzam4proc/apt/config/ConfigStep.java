package agzam4proc.apt.config;

import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.google.auto.common.MoreElements;
import com.squareup.javapoet.*;

import agzam4proc.AptError;
import agzam4proc.BaseStep;
import agzam4proc.Docs;
import agzam4proc.Proc;
import agzam4proc.apt.config.ConfigAnnotations.Config;
import agzam4proc.lib.PConfig;
import agzam4proc.lib.PVars;
import agzam4proc.utils.MoreTypeUtils;
import agzam4proc.utils.element.TypeElem;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.serialization.Jval;
import mindustry.Vars;

public class ConfigStep extends BaseStep {

	private static final ClassName $Fi = ClassName.get(Fi.class);
	private static final ClassName $Vars = ClassName.get(Vars.class);
	private static final ClassName $PVars = ClassName.get(PVars.class);
	private static final ClassName $Jval = ClassName.get(Jval.class);
	private static final ClassName $Jformat = ClassName.get(Jval.class).nestedClass("Jformat");
	private static final ClassName $Config = ClassName.get(PConfig.class);
	private static final ClassName $StringBuilder = ClassName.get(StringBuilder.class);

	@Override
	public Seq<Class<?>> classes() {
		return Seq.with(Config.class);
	}

	@Override
	public Set<? extends Element> step() {
		var configs = getElements(Config.class);
		var generatedClassNames = new Seq<ClassName>();
		var configTypeElements = new Seq<TypeElement>();

		for (var element : configs) {
			if (!(element instanceof TypeElement typeElement)) continue;

			var config = typeElement.getAnnotation(Config.class);
			if (config == null) continue;
			configTypeElements.add(typeElement);

			String filename = config.value();
			ClassName originalClass = ClassName.get(typeElement);

			var fields = new Seq<VariableElement>();
			var fieldDocs = new ObjectMap<String, String>();

			for (var enclosed : typeElement.getEnclosedElements()) {
				if (!enclosed.getKind().isField()) continue;
				if (!enclosed.getModifiers().contains(Modifier.STATIC)) continue;
				if (enclosed.getModifiers().contains(Modifier.TRANSIENT)) continue;

				var field = MoreElements.asVariable(enclosed);
				fields.add(field);

				String doc = processor.processingEnv().getElementUtils().getDocComment(field);
				if (doc != null && !doc.isEmpty()) {
					fieldDocs.put(field.getSimpleName().toString(), Docs.desc(doc).trim());
				}
			}

			String genName = Strings.kebabToCamel("-" + filename + "-config");
			ClassName genClass = ClassName.get(processor.packageName, genName);
			generatedClassNames.add(genClass);

			TypeSpec.Builder classBuilder = TypeSpec.classBuilder(genName)
					.addAnnotation(Proc.generated("agzam4proc.apt.config.ConfigStep"))
					.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

			classBuilder.addField(FieldSpec.builder($Fi, "file", Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC)
					.initializer(CodeBlock.of("$T.dataDirectory.child($S).child($S)", $PVars, "config", filename + ".hjson")).build());
			
			
			classBuilder.addMethod(buildLoad(filename, originalClass, fields));
			classBuilder.addMethod(buildSave(filename, originalClass, fields, fieldDocs));

			for (var field : fields) {
				String name = field.getSimpleName().toString();
				classBuilder.addField(FieldSpec.builder(TypeName.get(field.asType()), name, Modifier.STATIC, Modifier.PUBLIC)
						.initializer(CodeBlock.of("$T.$L", originalClass, name)).build());
				
				classBuilder.addField(FieldSpec.builder($Config, name + "Config", Modifier.STATIC, Modifier.PUBLIC)
						.initializer(CodeBlock.of("new $T($S, $S, $T.$L, () -> $L, o -> $L($L))", 
								$Config, 
								filename + "." + Strings.camelToKebab(name), 
								fieldDocs.get(name, name + " config"),
								originalClass, name,
								name,
								name,
								MoreTypeUtils.of(TypeElem.of(field.asType())).valueOf.get("o")
								)).build());
				
				classBuilder.addMethod(buildSetter(originalClass, field));
			}

			processor.write(null, classBuilder.build(), typeElement);
		}

		if (!generatedClassNames.isEmpty()) {
			generateConfigsLoader(generatedClassNames, configTypeElements);
		}

		return none();
	}

	private void generateConfigsLoader(Seq<ClassName> generatedClasses, Seq<TypeElement> originatingElements) {
		var classBuilder = TypeSpec.classBuilder("Configs")
				.addAnnotation(Proc.generated("agzam4proc.apt.config.ConfigStep"))
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

		var loadMethod = MethodSpec.methodBuilder("load")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(void.class);

		for (var genClass : generatedClasses) {
			loadMethod.addStatement("$T.load()", genClass);
		}

		classBuilder.addMethod(loadMethod.build());
		processor.write(null, classBuilder.build(), originatingElements.toArray(Element.class));
	}

	private void generateConfigsLoader(Seq<ClassName> generatedClasses) {
		var classBuilder = TypeSpec.classBuilder("Configs")
				.addAnnotation(Proc.generated("agzam4proc.apt.config.ConfigStep"))
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

		var loadMethod = MethodSpec.methodBuilder("load")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(void.class);

		for (var genClass : generatedClasses) {
			loadMethod.addStatement("$T.load()", genClass);
		}

		classBuilder.addMethod(loadMethod.build());
		processor.write(null, classBuilder.build());
	}

	private MethodSpec buildLoad(String filename, ClassName originalClass, Seq<VariableElement> fields) {
		var method = MethodSpec.methodBuilder("load")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(void.class);

//		method.addStatement("$T $L = $T.dataDirectory.child($S).child($S).child($S)", $Fi, "file", $Vars, filename + ".hjson");
		method.beginControlFlow("if (!$L.exists())", "file");
		method.addStatement("$T.warn($S, $L)", ClassName.get(Log.class), "@ config not found, creating default", "file");
		method.addStatement("save()");
		method.addStatement("return");
		method.endControlFlow();

		method.addStatement("var $L = $T.read($L.readString())", "data", $Jval, "file");

		for (var field : fields) {
			String name = field.getSimpleName().toString();
			String asMethod = jvalAsMethod(field);
			String cast = fieldCast(field);
			method.beginControlFlow("if ($L.has($S))", "data", name);
			method.addStatement("$L = $L$L.get($S).$L()", name, cast, "data", name, asMethod);
			method.endControlFlow();
		}

		return method.build();
	}

	private MethodSpec buildSave(String filename, ClassName originalClass, Seq<VariableElement> fields,
			ObjectMap<String, String> fieldDocs) {
		var method = MethodSpec.methodBuilder("save")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(void.class);

		method.addStatement("var $L = $T.newObject()", "obj", $Jval);

		for (var field : fields) {
			String name = field.getSimpleName().toString();
			method.addStatement("$L.put($S, $T.valueOf($L))", "obj", name, $Jval, name);
		}

		method.addStatement("$T[] $L = $L.toString($T.hjson).split($S)", String.class, "lines", "obj", $Jformat, "\n");
		method.addStatement("$T $L = new $T($S)", $StringBuilder, "sb", $StringBuilder, "{\n");

		method.beginControlFlow("for ($T $L : $L)", String.class, "line", "lines");
		method.addStatement("$T $L = $L.trim()", String.class, "trimmed", "line");

		for (var field : fields) {
			String name = field.getSimpleName().toString();
			String doc = fieldDocs.get(name);
			if (doc != null && !doc.isEmpty()) {
				String escaped = doc.replace("\\", "\\\\").replace("\"", "\\\"");
				method.beginControlFlow("if ($L.startsWith($S))", "trimmed", name);
				method.addStatement("$L.append($S)", "sb", "\t// " + escaped + "\n");
				method.endControlFlow();
			}
		}

		method.addStatement("$L.append($S).append($L).append($S)", "sb", "\t", "line", "\n");
		method.endControlFlow();

		method.addStatement("$L.append($S)", "sb", "}\n");
		method.addStatement("$L.parent().mkdirs()", "file");
		method.addStatement("$L.writeString($L.toString(), $L)", "file", "sb", false);

		return method.build();
	}

	private MethodSpec buildSetter(ClassName originalClass, VariableElement field) {
		String name = field.getSimpleName().toString();
		TypeName type = TypeName.get(field.asType());

		var method = MethodSpec.methodBuilder(name)
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addParameter(type, "value")
				.returns(void.class);

		method.addStatement("$L = $L", name, "value");
		method.addStatement("save()");

		return method.build();
	}

	private String jvalAsMethod(VariableElement field) {
		TypeMirror type = field.asType();
		if (type.getKind() == TypeKind.DECLARED) {
			TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
			if (typeElement.getQualifiedName().contentEquals("java.lang.String")) {
				return "asString";
			}
			throw new AptError(field, "Unsupported field type: @", type);
		}
		return switch (type.getKind()) {
			case BOOLEAN -> "asBool";
			case BYTE, SHORT, INT -> "asInt";
			case LONG -> "asLong";
			case FLOAT -> "asFloat";
			case DOUBLE -> "asDouble";
			default -> throw new AptError(field, "Unsupported field type: @", type);
		};
	}

	private String fieldCast(VariableElement field) {
		return switch (field.asType().getKind()) {
			case BYTE -> "(byte) ";
			case SHORT -> "(short) ";
			default -> "";
		};
	}
}
