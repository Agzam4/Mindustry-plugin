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
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.serialization.Jval;
import arc.util.serialization.Jval.Jformat;
import mindustry.Vars;

public class ConfigStep extends BaseStep {

	private static final ClassName $Fi = ClassName.get(Fi.class);
	private static final ClassName $Vars = ClassName.get(Vars.class);
	private static final ClassName $Jval = ClassName.get(Jval.class);
	private static final ClassName $Jformat = ClassName.get(Jval.class).nestedClass("Jformat");
	private static final ClassName $StringBuilder = ClassName.get(StringBuilder.class);

	@Override
	public Seq<Class<?>> classes() {
		return Seq.with(Config.class);
	}

	@Override
	public Set<? extends Element> step() {
		var configs = getElements(Config.class);

		for (var element : configs) {
			if (!(element instanceof TypeElement typeElement)) continue;

			var config = typeElement.getAnnotation(Config.class);
			if (config == null) continue;

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

			TypeSpec.Builder classBuilder = TypeSpec.classBuilder(typeElement.getSimpleName() + "Gen")
					.addAnnotation(Proc.generated("agzam4proc.apt.config.ConfigStep"))
					.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

			classBuilder.addMethod(buildLoad(filename, originalClass, fields));
			classBuilder.addMethod(buildSave(filename, originalClass, fields, fieldDocs));

			for (var field : fields) {
				classBuilder.addMethod(buildSetter(originalClass, field));
			}

			processor.write(null, classBuilder.build(), typeElement);
		}

		return none();
	}

	private MethodSpec buildLoad(String filename, ClassName originalClass, Seq<VariableElement> fields) {
		var method = MethodSpec.methodBuilder("load")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(void.class);

		method.addStatement("$T $L = $T.dataDirectory.child($S)", $Fi, "file", $Vars, filename + ".hjson");
		method.beginControlFlow("if (!$L.exists())", "file");
		method.addStatement("return");
		method.endControlFlow();

		method.addStatement("var $L = $T.read($L.readString())", "data", $Jval, "file");

		for (var field : fields) {
			String name = field.getSimpleName().toString();
			String asMethod = jvalAsMethod(field);
			String cast = fieldCast(field);
			method.beginControlFlow("if ($L.has($S))", "data", name);
			method.addStatement("$T.$L = $L$L.get($S).$L()", originalClass, name, cast, "data", name, asMethod);
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
			method.addStatement("$L.put($S, $T.valueOf($T.$L))", "obj", name, $Jval, originalClass, name);
		}

		method.addStatement("$T[] $L = $L.toString($T.formatted).split($S)", String.class, "lines", "obj", $Jformat, "\n");
		method.addStatement("$T $L = new $T()", $StringBuilder, "sb", $StringBuilder);

		method.beginControlFlow("for ($T $L : $L)", String.class, "line", "lines");
		method.addStatement("$T $L = $L.trim()", String.class, "trimmed", "line");

		for (var field : fields) {
			String name = field.getSimpleName().toString();
			String doc = fieldDocs.get(name);
			if (doc != null && !doc.isEmpty()) {
				String escaped = doc.replace("\\", "\\\\").replace("\"", "\\\"");
				method.beginControlFlow("if ($L.startsWith($S))", "trimmed", "\"" + name + "\"");
				method.addStatement("$L.append($S)", "sb", "// " + escaped + "\n");
				method.endControlFlow();
			}
		}

		method.addStatement("$L.append($L).append($S)", "sb", "line", "\n");
		method.endControlFlow();

		method.addStatement("$T.dataDirectory.child($S).writeString($L.toString(), $L)", $Vars, filename + ".hjson", "sb", false);

		return method.build();
	}

	private MethodSpec buildSetter(ClassName originalClass, VariableElement field) {
		String name = field.getSimpleName().toString();
		TypeName type = TypeName.get(field.asType());

		var method = MethodSpec.methodBuilder(name)
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addParameter(type, "value")
				.returns(void.class);

		method.addStatement("$T.$L = $L", originalClass, name, "value");
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
