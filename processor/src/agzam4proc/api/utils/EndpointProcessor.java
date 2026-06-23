package agzam4proc.api.utils;

import java.util.Objects;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.sun.net.httpserver.HttpExchange;

import agzam4proc.api.ApiSnippets;
import agzam4proc.api.utils.Interfaces.CodeProvider;
import agzam4proc.api.utils.Interfaces.CodeProviderContext;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;

public class EndpointProcessor {

	protected final ObjectMap<TypeName, DagNode<VarriableInit, ConstantValue>> envVariables = ObjectMap.of();

	protected final MethodInfo method;
	protected final String name;

	public EndpointProcessor(String prefixValue, String methodName, MethodInfo method) {
		this.method = method;
		this.name = endpointName(methodName, method.name);

		envVariables.put(
				TypeName.get(HttpExchange.class), 
				DagNode.<VarriableInit, ConstantValue>constant(
						new VarriableInit(new CodeProvider() {
							
							@Override
							public CodeBlockBuilder build(CodeProviderContext context, CodeBlockBuilder builder) {
								builder.addCode("$T.registerEndpoint(server, $S, $L -> {$>\n", 
										TypeName.get(ApiSnippets.class), 
										prefixValue + "/" + name,
										context.node.value.value
										);
								CodeBlockBuilder inside = new CodeBlockBuilder();
								builder.add(inside);
								builder.addStatement("$<})");
								return inside;
							}
						}), 
						ConstantValue.varriable("exchange"))
				);
		method.resolve(envVariables.keys().toSeq().asSet());
	}

	public CodeBlock build() {
		var root = new CodeBlockBuilder();
		var current = root;
		var graph = buildGraph(new VarriableInit(method, true), null);
		var builders = graph.linearize();
		
		Log.info("Building...");
		
		ObjectMap<String, Integer> namespace = ObjectMap.of("server", 1);
		
		for (int i = 0; i < builders.size; i++) {
			if(builders.get(i).payload == null) continue;
			var context = new CodeProviderContext(builders.get(i), namespace);
			current = builders.get(i).payload.build(context, current);
			
		}
		
		var block = root.build();
		return block;
	}

	public DagNode<VarriableInit, ConstantValue> buildGraph(VarriableInit varriable, String parmname) {
//		DagNode<VarriableInit, ConstantValue> current = DagNode.branch(varriable);
		var childNodes = new Seq<DagNode<VarriableInit,ConstantValue>>();

		for (var resolver : varriable.info.resolvers) {
			if (resolver.method != null) {
				var g = buildGraph(new VarriableInit(resolver.method, false), resolver.name);
				Objects.requireNonNull(g);
				childNodes.add(g);
				continue;
			}
			if (resolver.parm) {
				childNodes.add(DagNode.constant(ConstantValue.string(parmname)));
				continue;
			}
			if (resolver.allowed != null) {
				var node = envVariables.get(resolver.allowed);
				Objects.requireNonNull(node);
				childNodes.add(node);
				continue;
			}
			Log.warn("Not found parm handler");
			childNodes.add(DagNode.constant(ConstantValue.none()));
		}

		return DagNode.of(childNodes, varriable);//current.link(childNodes);
	}


	private String endpointName(String force, String s) {
		if(!force.isEmpty()) return force;
		StringBuilder result = new StringBuilder(s.length() + 1);
		for(int i = 0; i < s.length(); i++){
			char c = s.charAt(i);
			if(i > 0 && Character.isUpperCase(s.charAt(i))){
				result.append('-');
			}
			result.append(Character.toLowerCase(c));
		}
		return result.toString();
	}
}
