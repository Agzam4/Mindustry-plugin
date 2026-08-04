package agzam4proc.apt.api;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.sun.net.httpserver.HttpExchange;

import agzam4proc.apt.api.ApiAnnotations.Sse;
import agzam4proc.utils.DagNode;
import agzam4proc.utils.MethodInfo;
import agzam4proc.utils.element.TypeElem;
import agzam4proc.utils.element.VariableElem;
import agzam4proc.utils.init.ConstProvider;
import agzam4proc.utils.init.ConsumerProvider;
import agzam4proc.utils.init.StringProvider;
import agzam4proc.utils.init.VariableInit;

public class SseEndpointProcessor extends EndpointProcessor {

	public SseEndpointProcessor(String prefixValue, TypeElem routerType, VariableElem field, Sse sse, TypeName object, MethodInfo method) {
		super(prefixValue, sse.value(), method);

		DagNode<VariableInit> exchange = new DagNode<>(new ConsumerProvider(
				"exchange",
				CodeBlock.of("$T.registerSseEndpoint", TypeName.get(ApiSnippets.class)),
				new ConstProvider("server"),
				new StringProvider(prefixValue + "/" + name)
		));

		DagNode<VariableInit> message = DagNode.of(new ConsumerProvider(
				"message",
				CodeBlock.of("$T.$L.register", routerType.typeName, field.name),
				exchange.value
		), exchange);

		envVariables.put(object, message);
		envVariables.put(TypeName.get(HttpExchange.class), exchange);
	}

}
