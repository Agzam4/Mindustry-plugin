package agzam4proc.api;

import javax.annotation.processing.Processor;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import com.sun.net.httpserver.HttpServer;
import agzam4proc.*;
import agzam4proc.api.ApiAnnotations.*;
import agzam4proc.api.lib.SseSource;
import agzam4proc.api.utils.*;
import agzam4proc.api.utils.JsonBuilderProcessor.GeneratedJsonBuilder;
import agzam4proc.api.utils.element.*;
import agzam4proc.api.utils.element.ExecutableElem;
import arc.struct.*;
import arc.util.Log;

@AutoService(Processor.class)
public class RouterProcessor extends BaseProcessor {

	{
		rounds = 2;
	}

	@Override
	public Seq<Class<?>> classes() {
		return Seq.with(Router.class, Dependency.class, Type.class, GeneratedJsonBuilder.class);
	}
	
	private DependenciesContext context;

	@Override
	public void onElement(ObjectMap<Class<?>, Seq<Element>> map) throws Throwable {
		if(context == null) context = new DependenciesContext(this);
		
		// Round 1
		if(round == 1) {
			// generating annotations from @Dependencies classes
			for (var dependency : map.get(Dependency.class)) {
				if (!(dependency instanceof TypeElement type)) continue;
				write("dependencies", context.addDependency(type).buildAnnotation());
			}
			// generating type classes information
			for (var e : map.get(Type.class)) {
				if (!(e instanceof TypeElement type)) continue;
				context.scheme.register(TypeElem.of(type));
//				write("dependencies", context.addDependency(type).buildAnnotation());
			}
			
			context.scheme.eachinfo(i -> {
				var b = JsonBuilderProcessor.builder("json", i);
				write("json", b.build());
			});
			
			return;
		}
		
		

		// Round 2 - generating endpoints
//		for (var generatedJsonBuilder : map.get(GeneratedJsonBuilder.class)) {
//			if(generatedJsonBuilder instanceof ExecutableElement method) {
//				GeneratedJsonBuilder builder = method.getAnnotation(GeneratedJsonBuilder.class);
//				JsonBuilderProcessor.builder(context.scheme.get(TypeName.get(builder.value()))).toString 
//				= new MethodInfo(context, TypeElem.of((TypeElement) method.getEnclosingElement()), new ExecutableElem(method));
//			}
//		}
		context.resolve();
		Log.info("Resolved!");
		Seq<ClassName> routers = Seq.with();
		
		for (var router : map.get(Router.class)) {
			if (!(router instanceof TypeElement typeElement)) continue;
			var type = TypeElem.of(typeElement);
			Log.info("Router: @", type);
			
			Router routerAnnotation = type.getAnnotation(Router.class);
			String prefixValue = (routerAnnotation != null) ? routerAnnotation.value() : "/";

			MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.returns(TypeName.VOID)
					.addParameter(ClassName.get(HttpServer.class), "server");

			boolean hasEndpoints = false;
			
			// SSE
			for (var field : type.fields) {
				var sseAnn = field.getAnnotation(Sse.class);
				if (sseAnn == null) continue;
				hasEndpoints = true;
				Log.info("@/@", prefixValue, field.name);

				if (field.type == null) throw field.error("Field \"@\" has no type", field.name);
				Element ftElem = field.type.element();
				if (ftElem == null) throw field.error("Field type \"@\" has no backing element", field.type.name);
				TypeMirror ftMirror = ftElem.asType();

				DeclaredType sseSourceType = Proc.findSuperType(ftMirror, SseSource.class.getCanonicalName());
				if (sseSourceType == null) throw field.error("Field \"@\" must inherit from SseSource", field.name);
				if (sseSourceType.getTypeArguments().isEmpty()) {
					throw field.error("Missing generic type on SseSource in field \"@\"", field.name);
				}
				TypeMirror genericType = sseSourceType.getTypeArguments().get(0);

				for (var method : field.type.methods) {
					if (!method.hasAnnotation(SseHandler.class)) continue;
					registerMethod.addCode(new SseEndpointProcessor(
							prefixValue, type, field, sseAnn,
							TypeName.get(genericType),
							new MethodInfo(context, field.type, method)
					).build());
				}
			}
			
			
			// POST
			for (var method : type.methods) {
				if(!method.hasAnnotation(Post.class)) continue;
				registerMethod.addCode(new EndpointProcessor(
						prefixValue, 
						method.getAnnotation(Post.class).value(), 
						new MethodInfo(context, type, method)).build());
				hasEndpoints = true;
			}

			if (hasEndpoints) {
				String generatedClassName = type.name + "Router";
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
