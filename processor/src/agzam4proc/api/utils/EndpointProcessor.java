package agzam4proc.api.utils;

import java.util.Objects;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.sun.net.httpserver.HttpExchange;

import agzam4proc.AptError;
import agzam4proc.api.ApiSnippets;
import agzam4proc.api.utils.Interfaces.CodeProviderContext;
import agzam4proc.api.utils.element.ExecutableElem;
import agzam4proc.api.utils.element.TypeElem;
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
		
		DagNode<VariableInit> resultNode = null;
		var resultMethod = new CallProvider(method);
		if(method.method.parms.size == 0) throw method.method.error("Endpoints must contains Dependency or @ in parameters", HttpExchange.class.getSimpleName());
		var resultType = method.returnType();
		if(!resultType.typeName.equals(TypeName.get(String.class))) {
			if(resultType == TypeElem.typeLong) {
				resultNode = buildGraph(null, new CallProvider(method), null);
				var objects = TypeElem.of(Objects.class);
				if(objects.methods.size == 0) {
					var toString = ExecutableElem.virtual("toString", TypeElem.of(String.class), objects.typeName);
					objects.addMethod(toString);
					toString.addParm(name, TypeElem.of(Object.class));
				}
				Log.info(objects.methods);
				var toStringInfo = new MethodInfo(method.context, objects, objects.methods.find(m -> m.name.equals("toString")));
				resultMethod = new CallProvider(toStringInfo);
			} else {
				Log.info("Return type not string", resultType.typepath.binary);
				var jbp = JsonBuilderProcessor.builders.get(resultType.noDimension());
				if(jbp == null) throw method.method.error("No string builders found for @", resultType.noDimension());
				resultNode = buildGraph(null, new CallProvider(method), null);
				var toStringInfo = new MethodInfo(method.context, jbp.builder, jbp.stringMethod[method.returnType().dimension()]);
				resultMethod = new CallProvider(toStringInfo);
			}
		}
		
		var graph = resultNode != null
				? DagNode.of(Seq.with(resultNode), resultMethod.asReturn())
				: buildGraph(null, resultMethod.asReturn(), null);
		
		var builders = graph.linearize((n1,n2) -> (n1.value instanceof ConsumerProvider ? 1 : 0) - (n2.value instanceof ConsumerProvider ? 1 : 0));
		
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

	public DagNode<VariableInit> buildGraph(DagNode<VariableInit> root, CallProvider varriable, String parmname) {
		var childNodes = new Seq<DagNode<VariableInit>>();
		if(root != null) childNodes.addAll(root.children);

		for (var resolver : varriable.method.resolvers) {
			if (resolver.method != null) {
				var g = buildGraph(null, new CallProvider(resolver.method), resolver.name);
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
		return DagNode.of(childNodes, varriable);
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
