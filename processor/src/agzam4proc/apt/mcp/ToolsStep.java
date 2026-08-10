package agzam4proc.apt.mcp;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import com.google.auto.common.MoreElements;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import agzam4proc.AptError;
import agzam4proc.BaseStep;
import agzam4proc.Docs;
import agzam4proc.apt.mcp.McpAnnotations.McpTool;
import agzam4proc.utils.MoreTypeUtils;
import agzam4proc.utils.Namespace;
import agzam4proc.utils.element.TypeElem;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public class ToolsStep extends BaseStep {

	@Override
	public Seq<Class<?>> classes() {
		return Seq.with(McpTool.class);
	}

	
	@Override
	public Set<? extends Element> step() {
		var tools = getElements(McpTool.class);
		final var $Map = ClassName.get(Map.class);
		final var $BiFunc = ParameterizedTypeName.get(
				ClassName.get(BiFunction.class), 
				ClassName.get(McpTransportContext.class), 
				ClassName.get(McpSchema.CallToolRequest.class), 
				ClassName.get(CallToolResult.class)
				);

		MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("build")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(ParameterizedTypeName.get(ClassName.get(ObjectMap.class), ClassName.get(McpSchema.Tool.class), $BiFunc));

		TypeSpec.Builder classBuilder = TypeSpec.classBuilder("McpTools")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				;

		Seq<String> names = Seq.with();
		
		for (var tool : tools) {
			Namespace namespace = new Namespace();
			
			var method = MoreElements.asExecutable(tool);
			
			CodeBlock.Builder adapter = CodeBlock.builder();
			
			var _exchange = namespace.get("exchange");
			var _request = namespace.get("request");
			var _arguments = namespace.get("arguments");

			adapter.add("($L, $L) -> {\n$>", _exchange, _request);
			adapter.add("try {\n$>");
			
			String mapArgs = snakeName(method);
			
			String doc = processor.getDocComment(method);
			
			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("$T.builder($S, $T.of(\n$>", ClassName.get(McpSchema.Tool.class), mapArgs, $Map);
			builder.add("$S, $S,\n$S, $T.of(\n$>", "type", "object", "properties", $Map);
			
			var docs = Docs.parms(doc);
			Log.info("docs: @", docs);
			
			Seq<CodeBlock> args = Seq.with();
			for (var parm : method.getParameters()) {
				var type = TypeElem.of(parm.asType());
				String parmName = snakeName(parm);
				if(!args.isEmpty()) builder.add(",");
				builder.add("$S, $L", parmName, typeScheme(method, type, docs.get(parm.getSimpleName().toString())));
				args.add(MoreTypeUtils.of(type).valueOf.get(CodeBlock.of("$L.get($S).toString()", _arguments, parmName)));
			}
			builder.add("$<)\n$<))");
			builder.add(".description($S)", Docs.desc(doc));
			builder.add(".build()");


			adapter.addStatement("var $L = $L.arguments()", _arguments, _request);
			
			adapter.addStatement("var result = $T.$N($>$L$<)", method.getEnclosingElement(), method.getSimpleName(), CodeBlock.join(args, ",\n"));
			
			adapter.addStatement("return $T.builder().addTextContent($L).build()", ClassName.get(McpSchema.CallToolResult.class), "result");
			adapter.add("$<}catch($T e){\n$>", ClassName.get(Throwable.class));
			adapter.addStatement("e.printStackTrace()");
			adapter.addStatement("return $T.builder().addTextContent(e.getMessage()).isError(true).build()", ClassName.get(McpSchema.CallToolResult.class));
			adapter.add("$<}");
			adapter.add("\n$<}");
			
			classBuilder.addField(FieldSpec.builder($BiFunc, mapArgs + "Handler").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.initializer(adapter.build())
					.build());
			
			names.add(mapArgs);
			names.add(mapArgs + "Handler");
			
			methodBuilder.addStatement("var $L = $L", mapArgs, builder.build());

		}
		methodBuilder.addStatement("return $T.of($L)", ClassName.get(ObjectMap.class), names.toString(", "));

		classBuilder.addMethod(methodBuilder.build());
		
		processor.write("tools", classBuilder.build());
		
		return none();
	}
	
	private static final ObjectMap<TypeElem, String> types = ObjectMap.of(
			TypeElem.typeByte, "integer",
			TypeElem.typeShort, "integer",
			TypeElem.typeInt, "integer",
			TypeElem.typeLong, "integer",

			TypeElem.typeFloat, "number",
			TypeElem.typeDouble, "number",

			TypeElem.typeBoolean, "boolean",
			
			TypeElem.of(String.class), "string"
	);
	
	private CodeBlock typeScheme(Element element, TypeElem type, String description) {
		if(!types.containsKey(type)) throw new AptError(element, "Unsupported type \"@\"", type);
		if(type.isArray()) {
			throw new AptError(element, "Arrays not supported now");
		}
		var builder = CodeBlock.builder();
		builder.add("$T.of(\n$>", ClassName.get(Map.class));
		builder.add("$S, $S", "type", types.get(type));
		if(description != null) builder.add(",\n$S, $S", "description", description);
		builder.add("\n$<)\n");
		return builder.build();
	}
	
	
	private static String snakeName(Element e) {
		return Strings.camelToKebab(e.getSimpleName().toString()).replace('-', '_');
	}
	
	
}
