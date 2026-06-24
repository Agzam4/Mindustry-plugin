package agzam4proc.api;

import javax.annotation.processing.Processor;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import com.sun.net.httpserver.HttpServer;
import agzam4proc.*;
import agzam4proc.api.ApiAnnotations.*;
import agzam4proc.api.lib.SseSource;
import agzam4proc.api.utils.*;
import arc.struct.*;
import arc.util.Log;

@AutoService(Processor.class)
public class RouterProcessor extends BaseProcessor {

	{
		rounds = 2;
	}

	@Override
	public Seq<Class<?>> classes() {
		return Seq.with(Router.class, Dependency.class);
	}
	
	private DependenciesContext context;

	@Override
	public void onElement(ObjectMap<Class<?>, Seq<Element>> map) throws Throwable {
		if(context == null) context = new DependenciesContext(this);
		
		// Round 1 - generating annotations from @Dependencies classes
		if(round == 1) {
			for (var dependency : map.get(Dependency.class)) {
				if (!(dependency instanceof TypeElement type)) continue;
				write("dependencies", context.addDependency(type).buildAnnotation());
			}
			return;
		}

		// Round 2 - generating endpoints
		context.resolve();
		
		Seq<ClassName> routers = Seq.with();
		
		for (var router : map.get(Router.class)) {
			if (!(router instanceof TypeElement type)) continue;
			Router routerAnnotation = type.getAnnotation(Router.class);
			String prefixValue = (routerAnnotation != null) ? routerAnnotation.value() : "/";

			MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.returns(TypeName.VOID)
					.addParameter(ClassName.get(HttpServer.class), "server");

			boolean hasEndpoints = false;
			
			// SSE
			for (var field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
				var endpoint = field.getAnnotation(Sse.class);
				if (endpoint == null) continue;
				hasEndpoints = true;
				Log.info("@/@", prefixValue, field.getSimpleName());

				if (field.asType() instanceof DeclaredType declaredType) {
				    DeclaredType sseSourceType = Proc.findSuperType(field.asType(), SseSource.class.getCanonicalName());
				    
				    if (sseSourceType == null) {
				        throw new AptError(field, "Field must inherit from SseSource");
				    }

				    if (sseSourceType.getTypeArguments().isEmpty()) {
				        throw new AptError(field, "Missing generic type on SseSource");
				    }
				    TypeMirror genericType = sseSourceType.getTypeArguments().get(0);

				    TypeElement fieldTypeElement = (TypeElement) declaredType.asElement();
				    var methods = ElementFilter.methodsIn(fieldTypeElement.getEnclosedElements());

				    for (ExecutableElement methodElement : methods) {
				        if(methodElement.getAnnotation(SseHandler.class) == null) continue;
						registerMethod.addCode(new SseEndpointProcessor(prefixValue, type, field, field.getAnnotation(Sse.class), TypeName.get(genericType), new MethodInfo(context, type, methodElement)).build());
				    }
				    
//						Log.info("@<@> @", fieldTypeElement, genericType, methodElement.getSimpleName());
//				            
//				            String exchange = typeVars.get(TypeName.get(HttpExchange.class));
//				            EndpointInfo info = new EndpointInfo(endpoint.value(), type, methodElement, ObjectMap.of(
//				                    TypeName.get(HttpExchange.class), exchange,
//				                    TypeName.get(genericType), "message"
//				            ));
//
//				            registerMethod.addComment("POST " + info.name);
//				            registerMethod.addCode("$T.registerSseEndpoint(server, $S, $L -> {\n", 
//				                    TypeName.get(ApiSnippets.class), prefixValue + "/" + info.name, exchange);
//
//				            registerMethod.addCode("$T.$L.register($L, message -> {\n", type, field.getSimpleName(), exchange);
//
//				            CodeBlock.Builder builder = CodeBlock.builder();
//				            var varname = info.resolve(builder);
//				            registerMethod.addCode(builder.build());
//							registerMethod.addStatement("return $L", varname);
//
//				            registerMethod.addStatement("})");
//				            registerMethod.addStatement("})");
//				            
//				        }
//				    }
				}
			}
			
			
			// POST
			for (var method : ElementFilter.methodsIn(type.getEnclosedElements())) {
				var endpoint = method.getAnnotation(Post.class);
				if(endpoint == null) continue;
				registerMethod.addCode(new EndpointProcessor(prefixValue, endpoint.value(), new MethodInfo(context, type, method)).build());
				hasEndpoints = true;
			}

			if (hasEndpoints) {
				String generatedClassName = type.getSimpleName().toString() + "Router";
				TypeSpec routerSpec = TypeSpec.classBuilder(generatedClassName)
						.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
						.addMethod(registerMethod.build())
						.build();

				write(routerSpec);
				routers.add(ClassName.get(packageName, generatedClassName));
			}
		}
		// Creating list of routers
		MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(TypeName.VOID)
				.addParameter(ClassName.get(HttpServer.class), "server");

		for (var element : routers) {
			registerMethod.addStatement("$T.register(server)", element);
		}
		
		CodeBlock.Builder b = CodeBlock.builder().add("new Class<?>[]{\n");
		boolean first = true;
		for (var element : routers) {
			if (!first) b.add(",\n");
			b.add("  $T.class", element);
			first = false;
		}

		b.add("\n}");
		TypeName classAny = ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class));
		TypeSpec type = TypeSpec.classBuilder("Routers")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addMethod(registerMethod.build())
				.addField(FieldSpec.builder(ArrayTypeName.of(classAny), "routers")
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
						.initializer(b.build())
						.build()).build();

		write(type);

	}

	@Override
	public void onElements(Seq<Element> elements) throws Throwable {}


}
