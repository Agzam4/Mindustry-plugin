package agzam4proc.apt.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.google.auto.common.MoreElements;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.sun.net.httpserver.HttpServer;

import agzam4proc.BaseStep;
import agzam4proc.Proc;
import agzam4proc.apt.api.ApiAnnotations.Post;
import agzam4proc.apt.api.ApiAnnotations.Router;
import agzam4proc.apt.api.ApiAnnotations.Sse;
import agzam4proc.apt.api.ApiAnnotations.SseHandler;
import agzam4proc.apt.api.lib.SseSource;
import agzam4proc.apt.api.proto.EndpointInfo;
import agzam4proc.apt.api.proto.ReactGenerator;
import agzam4proc.apt.api.proto.TypescriptGenerator;
import agzam4proc.utils.DependenciesContext;
import agzam4proc.utils.MethodInfo;
import agzam4proc.utils.element.TypeElem;
import arc.struct.Seq;
import arc.util.Log;

public class RouterStep extends BaseStep {

	private DependenciesContext context;
	private Seq<ClassName> routers = Seq.with();
	private Seq<EndpointInfo> endpointInfos = new Seq<>();
	
	public RouterStep(DependenciesContext context) {
		this.context = context;
	}
	
	@Override
	public Seq<Class<?>> classes() {
		return Seq.with(Router.class);
	}
	
	@Override
	public Seq<String> generatedClasses() {
		return context.dependencyCache.keys().toSeq().map(t -> t.binary);
	}

	@Override
	public Set<? extends Element> step() {
		Set<Element> deferredElements = new HashSet<>();
		Set<Element> routers = getElements(Router.class);

		for (var d : context.dependencyCache) {
			if(processor.processingEnv().getElementUtils().getTypeElement(d.key.type) == null) return all();
		}

		for (Element element : routers) {
			try {

				TypeElement typeElement = MoreElements.asType(element);
				var type = TypeElem.of(typeElement);

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
					String epMethodName = EndpointProcessor.endpointName(method.getAnnotation(Post.class).value(), method.name);
					String epUrl = prefixValue + "/" + epMethodName;
					var info = new MethodInfo(context, type, method);
					endpointInfos.add(new EndpointInfo(epUrl, method.returnType, info, method.parms));
					var ep = new EndpointProcessor(
							prefixValue, 
							method.getAnnotation(Post.class).value(), 
							info);
					registerMethod.addCode(ep.build());
					hasEndpoints = true;
				}

				if (hasEndpoints) {
					String generatedClassName = type.name + "Router";
					TypeSpec routerSpec = TypeSpec.classBuilder(generatedClassName)
							.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
							.addMethod(registerMethod.build())
							.build();

					processor.write(null, routerSpec, element); // XXX: add dependencies?
					Log.info("&lg+ Router: @", routerSpec.name);
					this.routers.add(ClassName.get(processor.packageName /* + ".routers" */, generatedClassName));
				}
			
			} catch (Throwable e) {
				Log.warn(e.getMessage());
				return routers;
			}
		}

		if (deferredElements.isEmpty() && !this.routers.isEmpty()) {
			MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.returns(TypeName.VOID)
					.addParameter(ClassName.get(HttpServer.class), "server");
	
			for (var element : this.routers) {
				registerMethod.addStatement("$T.register(server)", element);
			}
//			
			CodeBlock.Builder b = CodeBlock.builder().add("new Class<?>[]{\n");
			boolean first = true;
			for (var element : this.routers) {
				if (!first) b.add(",\n");
				b.add("  $T.class", element);
				first = false;
			}
	//
			b.add("\n}");
			TypeName classAny = ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class));
			TypeSpec type = TypeSpec.classBuilder("Routers")
					.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
					.addMethod(registerMethod.build())
					.addField(FieldSpec.builder(ArrayTypeName.of(classAny), "routers")
							.addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
							.initializer(b.build())
							.build()).build();
	
			processor.write(null, type);
	
			try {
				new TypescriptGenerator(context, endpointInfos, context.processingEnv).write();
			} catch (Exception e) {
				Log.err("Failed to generate TypeScript API", e);
			}
			try {
				new ReactGenerator(context, endpointInfos, context.processingEnv).write();
			} catch (Exception e) {
				Log.err("Failed to generate React hooks", e);
			}
	    }

		return deferredElements; 
	}

}
