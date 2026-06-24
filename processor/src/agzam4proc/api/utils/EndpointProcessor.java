package agzam4proc.api.utils;

import java.util.Objects;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.sun.net.httpserver.HttpExchange;

import agzam4proc.api.ApiSnippets;
import agzam4proc.api.utils.Interfaces.CodeProviderContext;
import agzam4proc.api.utils.init.*;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;

public class EndpointProcessor {

	protected final ObjectMap<TypeName, DagNode<VariableInit>> envVariables = ObjectMap.of();

	protected final MethodInfo method;
	protected final String name;
	
	protected boolean debug = false;

	public EndpointProcessor(String prefixValue, String methodName, MethodInfo method) {
		this.method = method;
		this.name = endpointName(methodName, method.name);

		envVariables.put(
				TypeName.get(HttpExchange.class),
				new DagNode<VariableInit>(new ConsumerProvider(
						"exchange",
						CodeBlock.of("$T.registerEndpoint", TypeName.get(ApiSnippets.class)),
						new ConstProvider("server"), 
						new StringProvider(prefixValue + "/" + name)
				))
		);
		
	}

	public CodeBlock build() {
		method.resolve(envVariables.keys().toSeq().asSet());
		var root = new CodeBlockBuilder();
		var current = root;
		var graph = buildGraph(new CallProvider(method).asReturn(), null);
		
		var builders = graph.linearize((n1,n2) -> (n1.value instanceof ConsumerProvider ? 1 : 0) - (n2.value instanceof ConsumerProvider ? 1 : 0));
		
//		Func2<Integer, DagNode<VariableInit>, Integer> comp = (score, node) -> score + (node.value instanceof ConsumerProvider ? 1 : 0);
//		var builders = graph.linearize((n1,n2) -> {
//			if(debug) {
//				 Log.info("@ (@) vs @ (@)", n1, n1.reduce(0, comp), n2, n2.reduce(0, comp));
//			}
//			return 
//					n1.reduce(0, comp) -
//					n2.reduce(0, comp);
//		}
//				);
		
		if(debug) Log.info("Building...");
		if(debug) Log.info("Seq: @", builders.toString(": ", n -> n.value.toString()));
		if(debug) Log.info("Seq: @", builders.toString());
		
		var namespace = Namespace.of("server");
		
		for (int i = 0; i < builders.size; i++) {
			var context = new CodeProviderContext(builders.get(i), namespace);
			if(debug) Log.info("@. @ (@)", i+1, builders.get(i).value, builders.get(i).value.hashCode());
			current = builders.get(i).value.builder(context, current);
		}
		
		var block = root.build();
		return block;
	}

	public DagNode<VariableInit> buildGraph(CallProvider varriable, String parmname) {
//		DagNode<VarriableInit, ConstantValue> current = DagNode.branch(varriable);
		var childNodes = new Seq<DagNode<VariableInit>>();

		for (var resolver : varriable.method.resolvers) {
			if (resolver.method != null) {
				var g = buildGraph(new CallProvider(resolver.method), resolver.name);
				Objects.requireNonNull(g);
				childNodes.add(g);
				continue;
			}
			if (resolver.parm) {
				childNodes.add(new DagNode<>(new StringProvider(parmname)));
				continue;
			}
			if (resolver.allowed != null) {
				var node = envVariables.get(resolver.allowed);
				Objects.requireNonNull(node);
				childNodes.add(node);
				continue;
			}
			Log.warn("Not found parm handler");
			childNodes.add(new DagNode<>(new NullProvider()));
		}

		return DagNode.of(childNodes, varriable);//current.link(childNodes);
	}


	public static String endpointName(String force, String s) {
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
