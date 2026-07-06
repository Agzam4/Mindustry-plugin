package agzam4proc.api.proto;

import javax.annotation.processing.ProcessingEnvironment;

import com.squareup.javapoet.TypeName;

import agzam4proc.api.utils.DependenciesContext;
import agzam4proc.api.utils.element.TypeElem;
import agzam4proc.api.utils.element.VariableElem;
import arc.struct.Seq;
import arc.util.Strings;

public class TypescriptGenerator extends Generator {

	public TypescriptGenerator(DependenciesContext context, Seq<EndpointInfo> endpoints, ProcessingEnvironment processingEnv) {
		super(context, endpoints, processingEnv);
	}

	@Override
	public String languageName() { return "typescript"; }

	@Override
	public String fileExtension() { return "ts"; }

	@Override
	public String generate() {
		var sb = new StringBuilder();
		sb.append("// Auto-generated\n// Do not edit\n\n");

		generateTypes(sb);
		generateApi(sb);

		return sb.toString();
	}

	private void generateTypes(StringBuilder sb) {
		sb.append("// -- API Types --\n\n");

		scheme.eachinfo(info -> {
			if(isSynthetic(info.type)) return;

			sb.append("export interface ").append(info.type.name);
			if(info.superclass != null && !isSynthetic(info.superclass.type)) {
				sb.append(" extends ").append(info.superclass.type.name);
			}
			sb.append(" {\n");
			for(var f : info.fields) {
				sb.append("  ").append(f.name).append(": ").append(javaToTs(f.type)).append("\n");
			}
			sb.append("}\n\n");
		});

		sb.append("// -- Errors --\n\n");
		sb.append("export interface NetError {\n");
		sb.append("  code: number\n");
		sb.append("  message: string\n");
		sb.append("}\n\n");
	}

	private void generateApi(StringBuilder sb) {
		sb.append("// -- Helpers --\n\n");

		sb.append("async function postJson<T>(url: string, body: any = {}): Promise<[T, null] | [null, NetError]> {\n");
		sb.append("  try {\n");
		sb.append("    const res = await fetch('/api' + url, {\n");
		sb.append("      method: \"POST\",\n");
		sb.append("      headers: { \"Content-Type\": \"application/json\" },\n");
		sb.append("      body: JSON.stringify(body)\n");
		sb.append("    })\n");
		sb.append("    if (!res.ok) return [null, { code: res.status, message: res.statusText }]\n");
		sb.append("    return [await res.json(), null]\n");
		sb.append("  } catch (e) {\n");
		sb.append("    return [null, { code: 0, message: e instanceof Error ? e.message : String(e) }]\n");
		sb.append("  }\n");
		sb.append("}\n\n");

		sb.append("async function postText(url: string, body: any = {}): Promise<[string, null] | [null, NetError]> {\n");
		sb.append("  try {\n");
		sb.append("    const res = await fetch(url, {\n");
		sb.append("      method: \"POST\",\n");
		sb.append("      headers: { \"Content-Type\": \"application/json\" },\n");
		sb.append("      body: JSON.stringify(body)\n");
		sb.append("    })\n");
		sb.append("    if (!res.ok) return [null, { code: res.status, message: res.statusText }]\n");
		sb.append("    return [await res.text(), null]\n");
		sb.append("  } catch (e) {\n");
		sb.append("    return [null, { code: 0, message: e instanceof Error ? e.message : String(e) }]\n");
		sb.append("  }\n");
		sb.append("}\n\n");

		sb.append("// -- API Methods --\n\n");

		var tree = buildTree();
		sb.append("export const Api = {\n");
		renderTree(sb, tree, "  ");
		sb.append("}\n");
	}

	protected Node buildTree() {
		var tree = new Node("");
		for(var ep : endpoints) {
			String path = ep.url;
			if(path.startsWith("/")) path = path.substring(1);
			if(path.isEmpty()) continue;
			String[] segs = path.split("/");

			var cur = tree;
			for(int i = 0; i < segs.length; i++) {
				boolean last = i == segs.length - 1;
				String name = last ? Strings.kebabToCamel(segs[i]) : segs[i];
				cur = cur.child(name);
				if(last) cur.endpoint = ep;
			}
		}
		return tree;
	}

	private void renderTree(StringBuilder sb, Node node, String indent) {
		var sorted = node.children.copy();
		for(var child : sorted) {
			if(child.endpoint != null) {
				renderMethod(sb, child.endpoint, child.name, indent);
			} else {
				sb.append(indent).append(child.name).append(": {\n");
				renderTree(sb, child, indent + "  ");
				sb.append(indent).append("},\n");
			}
		}
	}

	private void renderMethod(StringBuilder sb, EndpointInfo ep, String name, String indent) {
		var args = ep.info.bodyArgs();
		var body = ep.params.select(p -> args.contains(p.name));
		String bodyArg;
		if(body.isEmpty()) {
			sb.append(indent).append(name).append(": () => ");
			bodyArg = "\")";
		} else {
			sb.append(indent).append(name).append(": (").append(createBody(body)).append(") => ");
			bodyArg = "\", body)";
		}
		if(isVoidType(ep.returnType)) {
			sb.append("postJson(\"").append(ep.url).append(bodyArg);
		} else if(isStringType(ep.returnType)) {
			sb.append("postText(\"").append(ep.url).append(bodyArg);
		} else {
			sb.append("postJson<").append(javaToTs(ep.returnType)).append(">(\"").append(ep.url).append(bodyArg);
		}
		sb.append(",\n");
	}
	
	protected String createBody(Seq<VariableElem> body) {
		if(body.size == 0) return "";
		StringBuilder sb = new StringBuilder();
		sb.append("body: {");
		for(int i = 0; i < body.size; i++) {
			var p = body.get(i);
			sb.append(" ").append(p.name).append(": ").append(javaToTs(p.type));
			if(i < body.size - 1) sb.append(";");
		}
		sb.append(" }");
		return sb.toString();
	}

	protected String javaToTs(TypeElem type) {
		if(type == null) return "void";
		if(type.isArray()) return javaToTs(type.componentType()) + "[]";
		var tn = type.typeName;
		if(tn.equals(TypeName.get(String.class))) return "string";
		if(tn.equals(TypeName.INT) || tn.equals(TypeName.INT.box())) return "number";
		if(tn.equals(TypeName.LONG) || tn.equals(TypeName.LONG.box())) return "number";
		if(tn.equals(TypeName.FLOAT) || tn.equals(TypeName.FLOAT.box())) return "number";
		if(tn.equals(TypeName.DOUBLE) || tn.equals(TypeName.DOUBLE.box())) return "number";
		if(tn.equals(TypeName.BOOLEAN) || tn.equals(TypeName.BOOLEAN.box())) return "boolean";
		if(tn.equals(TypeName.VOID)) return "void";
		return type.name;
	}

	protected boolean isStringType(TypeElem type) {
		return type != null && type.typeName.equals(TypeName.get(String.class));
	}

	protected boolean isVoidType(TypeElem type) {
		return type == null || type.typeName.equals(TypeName.VOID);
	}

	private boolean isSynthetic(TypeElem type) {
		return type.typepath != null && type.typepath.isSystemPackage();
	}

	public static class Node {
		final String name;
		EndpointInfo endpoint;
		final Seq<Node> children = new Seq<>();

		Node(String name) { this.name = name; }

		Node child(String name) {
			var found = children.find(n -> n.name.equals(name));
			if(found != null) return found;
			var n = new Node(name);
			children.add(n);
			return n;
		}
	}

}
