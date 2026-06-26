package agzam4proc.api.utils;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.sun.net.httpserver.HttpExchange;

import agzam4proc.api.ApiAnnotations.Sse;
import agzam4proc.api.ApiSnippets;
import agzam4proc.api.utils.element.TypeElem;
import agzam4proc.api.utils.element.VariableElem;
import agzam4proc.api.utils.init.ConstProvider;
import agzam4proc.api.utils.init.ConsumerProvider;
import agzam4proc.api.utils.init.StringProvider;
import agzam4proc.api.utils.init.VariableInit;

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
