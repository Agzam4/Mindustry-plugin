package agzam4proc.api.proto;

import java.io.IOException;
import java.nio.file.*;

import javax.annotation.processing.ProcessingEnvironment;

import com.sun.net.httpserver.HttpExchange;

import arc.struct.Seq;
import arc.util.Strings;
import agzam4proc.api.utils.Scheme;
import agzam4proc.api.utils.element.TypeElem;

public class ReactGenerator extends TypescriptGenerator {

	private final ProcessingEnvironment processingEnv;

	public ReactGenerator(Scheme scheme, Seq<EndpointInfo> endpoints, ProcessingEnvironment processingEnv) {
		super(scheme, endpoints, processingEnv);
		this.processingEnv = processingEnv;
	}

	@Override
	public String languageName() { return "react"; }

	@Override
	public void write() throws IOException {
		String outDir = processingEnv.getOptions().get("typescriptOutDir");
		if (outDir == null) return;
		var path = Path.of(outDir, "api-hooks.ts");
		Files.createDirectories(path.getParent());
		Files.writeString(path, generate());
	}

	@Override
	public String generate() {
		var sb = new StringBuilder();
		sb.append("// Auto-generated\n// Do not edit\n\n");
		sb.append("import { useState, useCallback } from 'react'\n");

		var usedTypes = new Seq<String>();
		for (var ep : endpoints) {
			collectType(usedTypes, ep.returnType);
		}
		if (!usedTypes.contains("NetError")) usedTypes.add("NetError");

		sb.append("import { Api } from './api'\n");
		sb.append("import type { ").append(usedTypes.toString(", ")).append(" } from './api'\n\n");

		var tree = buildTree();
		sb.append("export const ApiHooks = {\n");
		renderHooksTree(sb, tree, "  ");
		sb.append("}\n");

		return sb.toString();
	}

	private boolean isPrimitiveTsType(String tsType) {
		return tsType.equals("string") || tsType.equals("number") || tsType.equals("boolean") || tsType.equals("void") || tsType.equals("null");
	}

	private void collectType(Seq<String> types, agzam4proc.api.utils.element.TypeElem type) {
		if (type == null) return;
		String tsType = javaToTs(type);
		if (tsType == null || isPrimitiveTsType(tsType)) return;
		String base = tsType.endsWith("[]") ? tsType.substring(0, tsType.length() - 2) : tsType;
		if (!types.contains(base)) types.add(base);
	}

	private void renderHooksTree(StringBuilder sb, Node node, String indent) {
		var sorted = node.children.copy();
		for (var child : sorted) {
			if (child.endpoint != null) {
				renderHook(sb, child.endpoint, child.name, indent);
			} else {
				sb.append(indent).append(child.name).append(": {\n");
				renderHooksTree(sb, child, indent + "  ");
				sb.append(indent).append("},\n");
			}
		}
	}

	private void renderHook(StringBuilder sb, EndpointInfo ep, String name, String indent) {
		String hookName = "use" + Strings.capitalize(name);
		String tsReturnType = javaToTs(ep.returnType);
		boolean isVoid = isVoidType(ep.returnType);
		boolean isString = isStringType(ep.returnType);

		String apiPath = buildApiPath(ep.url);

		sb.append(indent).append(hookName).append(": () => {\n");
		String inner = indent + "  ";

		if (!isVoid) {
			String dataType = isString ? "string" : tsReturnType;
			sb.append(inner).append("const [data, setData] = useState<").append(dataType).append(" | null>(null)\n");
		}
		sb.append(inner).append("const [error, setError] = useState<NetError | null>(null)\n");
		sb.append(inner).append("const [loading, setLoading] = useState(false)\n");


		var body = ep.params.select(p -> p.type != TypeElem.of(HttpExchange.class));
//		if(body.isEmpty()) {
//			sb.append(indent).append(name).append(": () => ");
//			bodyArg = "\")";
//		} else {
//			sb.append(indent).append(name).append(": (body: {");
//			for(int i = 0; i < body.size; i++) {
//				var p = body.get(i);
//				sb.append(" ").append(p.name).append(": ").append(javaToTs(p.type));
//				if(i < body.size - 1) sb.append(";");
//			}
//			sb.append(" }) => ");
//			bodyArg = "\", body)";
//		}
//		if(isVoidType(ep.returnType)) {
//			sb.append("postJson(\"").append(ep.url).append(bodyArg);
//		} else if(isStringType(ep.returnType)) {
//			sb.append("postText(\"").append(ep.url).append(bodyArg);
//		} else {
//			sb.append("postJson<").append(javaToTs(ep.returnType)).append(">(\"").append(ep.url).append(bodyArg);
//		}
//		sb.append(",\n");
	

		String bodyArg;
		if(body.isEmpty()) {
			bodyArg = "()\n";
			sb.append(inner).append("const execute = useCallback(async () => {\n");
		} else {
			bodyArg = "(body)\n";
			sb.append(inner).append("const execute = useCallback(async (body: {");
			for (int i = 0; i < body.size; i++) {
				var p = body.get(i);
				sb.append(" ").append(p.name).append(": ").append(javaToTs(p.type));
				if (i < body.size - 1) sb.append(";");
			}
			sb.append(" }) => {\n");
		}
		sb.append(inner).append("  setLoading(true)\n");

		if (isVoid) {
			sb.append(inner).append("  const [_, err] = await Api.").append(apiPath).append(bodyArg);
		} else {
			sb.append(inner).append("  const [res, err] = await Api.").append(apiPath).append(bodyArg);
		}

		sb.append(inner).append("  if (err) setError(err)\n");
		if (!isVoid) {
			sb.append(inner).append("  else setData(res)\n");
		}
		sb.append(inner).append("  setLoading(false)\n");

		if (isVoid) {
			sb.append(inner).append("  return { error: err }\n");
		} else {
			sb.append(inner).append("  return { data: res, error: err }\n");
		}

		sb.append(inner).append("}, [])\n");

		if (isVoid) {
			sb.append(inner).append("return { error, loading, execute }\n");
		} else {
			sb.append(inner).append("return { data, error, loading, execute }\n");
		}

		sb.append(indent).append("},\n");
	}

	private String buildApiPath(String url) {
		String path = url;
		if (path.startsWith("/")) path = path.substring(1);
		String[] segs = path.split("/");
		var parts = new Seq<String>();
		for (int i = 0; i < segs.length; i++) {
			boolean last = i == segs.length - 1;
			parts.add(last ? Strings.kebabToCamel(segs[i]) : segs[i]);
		}
		return parts.toString(".");
	}
}
