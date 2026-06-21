package agzam4proc.api;

import java.lang.annotation.ElementType;
import javax.annotation.processing.Processor;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import agzam4proc.AptError;
import agzam4proc.BaseProcessor;
import agzam4proc.Proc;
import agzam4proc.api.ApiAnnotations.*;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;

@AutoService(Processor.class)
public class RouterProcessor extends BaseProcessor {

	{
		rounds = 2;
	}

	@Override
	public Seq<Class<?>> classes() {
		return Seq.with(Router.class, Dependency.class);
	}
	
	class DependencyInfo {
		
		final TypeElement type;
		final ObjectMap<TypeName, MethodInfo> methods = ObjectMap.of();
		final String name;
		final String annotation;
		
		public DependencyInfo(TypeElement type) {
			this.type = type;
			
			String name = type.getSimpleName().toString();
			if(!name.endsWith("Dependency")) throw new AptError(type, "Dependency class name must ends with \"Dependency\"");
			name = name.substring(0, name.length() - "Dependency".length());

			annotation = packageName + ".dependencies." + name;
//			this.annotation = ClassName.get(packageName + ".dependencies", name);
			
			this.name = name;
			
			 for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
				 if(!method.getModifiers().contains(Modifier.STATIC)) continue;
				 if(!method.getSimpleName().contentEquals("depends")) continue;

				 var returnType = typeUtils.erasure(method.getReturnType());
				 var returnTypeName = TypeName.get(returnType);
				 
				 if(returnType.getKind() == TypeKind.VOID) throw new AptError(method, "Method \"depends\" cannot return void");
				 if(methods.containsKey(returnTypeName)) throw new AptError(method, "Duplicate of dependency return type: \"@\"", returnTypeName.toString());
				 
				 methods.put(returnTypeName, new MethodInfo(type, method));
			 }
			 if(methods.size == 0) throw new AptError(type, "");
		}
		
		public TypeSpec buildAnnotation() {
			return TypeSpec.annotationBuilder(this.name)
					.addModifiers(Modifier.PUBLIC)
					.addAnnotation(Proc.target(ElementType.PARAMETER))
					.build();
		}
		

		protected void resolve() {
			methods.each((t,m) -> m.resolve());
		}
	}
	
	class MethodInfo {
		
		final ExecutableElement method;
		final String name;
		final TypeElement cls;
		
		public MethodInfo(TypeElement cls, ExecutableElement method) {
			this.method = method;
			this.cls = cls;
			this.name = method.getSimpleName().toString();
		}
		
		Seq<ParmResolver> resolvers = null;
		
		protected Seq<ParmResolver> resolve() {
			if(resolvers != null) return resolvers;
			Log.info("Resolving @:@", cls.getSimpleName(), name);
			resolvers = new Seq<>();
			for (var parm : method.getParameters()) {
				Log.info("- @", parm.getSimpleName());
				resolvers.add(new ParmResolver(parm));
			}
			return resolvers;
		}
		
	}
	
	class ParmResolver {
		
		final ObjectSet<ParmResolver> require = ObjectSet.with();
		
		
		final Seq<ExecutableElement> stack = Seq.with();
		
		ParmResolver next;
		
		@Nullable MethodInfo method;
		boolean exchange, parm;
		
		public ParmResolver(VariableElement parameter) {
            TypeName paramTypeName = TypeName.get(typeUtils.erasure(parameter.asType()));

            boolean found = false;
		    for (var annotation : parameter.getAnnotationMirrors()) { // FIXME
				TypeElement annotationElement = (TypeElement) annotation.getAnnotationType().asElement();
				String annotationClassPath = annotationElement.getQualifiedName().toString();
				
				var typeName = TypeName.get(annotationElement.asType()).toString();
				
		        if(annotationClassPath.equals(Parm.class.getCanonicalName())) {
					if(found) throw new AptError(annotationElement, "Parametr can contains only one dependency annotation");
					Parm parm = parameter.getAnnotation(Parm.class);
					// TODO
					found = true;
		        	continue;
		        }
				if(dependencyCache.containsKey(typeName)) {
					if(found) throw new AptError(annotationElement, "Parametr can contains only one dependency annotation");
					var dependency = dependencyCache.get(typeName);

					if(!dependency.methods.containsKey(paramTypeName)) 
						throw new AptError(
								annotationElement, 
								"Dependency \"@\" does not contain an implementation for \"@\" type", 
								dependency.name.toString(), 
								paramTypeName.toString()
								);
					
					this.method = dependency.methods.get(paramTypeName);
					this.require.addAll(this.method.resolve());
					found = true;
					continue;
				}
		    }
		    
		    if(TypeName.get(HttpExchange.class).equals(paramTypeName)) {
		    	this.exchange = true;
		    	return;
		    }
		    if(!found) throw new AptError(parameter, String.format(
		    		"No provider found for \"%s\". Ensure a @Dependency exists that returns this type.", 
		    		parameter.getSimpleName()
		    		));

//			Seq<TypeName> resolvedTypes = Seq.with();
//			TypeName currentType = TypeName.get(parameter.asType());
//			while (true) {
//				if(!dependencyCache.containsKey(currentType)) throw new AptError(parameter, "Parameter must ", null);
//				// Searching dependencies loops
//				int index = resolvedTypes.indexOf(currentType);
//				if(index != -1) {
//					resolvedTypes.removeRange(0, index);
//					throw new AptError(parameter, currentType, null);
//				}
//			}
		}
	}
	

	class EndpointInfo {
		
		final String name;
		private MethodInfo method;

		public EndpointInfo(TypeElement type, ExecutableElement method) {
			var endpoint = method.getAnnotation(Post.class);
			
			String methodName = method.getSimpleName().toString();
			this.name = endpointName(endpoint.value(), methodName);
			
			
			this.method = new MethodInfo(type, method);
			this.method.resolve();
		}
		

		protected CodeBlock resolve(CodeBlock.Builder builder) {
			ObjectMap<String, ParmResolver> context = ObjectMap.of(); // name, resolver
			return statement(context, builder, method);
		}
		

		protected CodeBlock statement(ObjectMap<String, ParmResolver> context, CodeBlock.Builder builder, MethodInfo info) {
			String name = method.name + context.size;
			context.put(name, null);

			Seq<CodeBlock> parms = Seq.with();
			for (var resolver : info.resolvers) {
				if(resolver.method != null) {
					parms.add(statement(context, builder, resolver.method));
					continue;
				}
				parms.add(CodeBlock.of("null"));
			}

		    var returnType = info.method.getReturnType(); 
		    var enclosingType = info.method.getEnclosingElement().asType();
		    
			builder.addStatement("$T $N = $T.$N($L)", 
					TypeName.get(returnType), 
					name,
					TypeName.get(enclosingType),
					info.name,
					CodeBlock.join(parms, ",")
					);
			return CodeBlock.of("$L", name);
		}
	}
	

	ObjectMap<String, DependencyInfo> dependencyCache = ObjectMap.of();

	@Override
	public void onElement(ObjectMap<Class<?>, Seq<Element>> map) throws Throwable {
		// Round 1 - generating annotations from @Dependencies classes
		if(round == 1) {
			for (var dependency : map.get(Dependency.class)) {
				if (!(dependency instanceof TypeElement type)) continue;

				var info = new DependencyInfo(type);

				dependencyCache.put(info.annotation, info);
				dependencyCache.put(info.name, info); // FIXME
				write("dependencies", info.buildAnnotation());
			}
			return;
		}

		// Round 2 - generating endpoints
		Log.info("Resolving...");
		dependencyCache.each((t,d) -> d.resolve());
		Log.info("Resolved!");
		
		for (var router : map.get(Router.class)) {
			if (!(router instanceof TypeElement type)) continue;
			Router routerAnnotation = type.getAnnotation(Router.class);
			String prefixValue = (routerAnnotation != null) ? routerAnnotation.value() : "/";

			MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.returns(TypeName.VOID)
					.addParameter(ClassName.get(HttpServer.class), "server");

			boolean hasEndpoints = false;
			
			for (var method : ElementFilter.methodsIn(type.getEnclosedElements())) {
				var endpoint = method.getAnnotation(Post.class);
				if(endpoint == null) continue;
				hasEndpoints = true;

				EndpointInfo info = new EndpointInfo(type, method);
				Log.info("/@", info.name);

				
				registerMethod.addComment("Endpoint: " + info.name);
				registerMethod.addCode("$T.registerEndpoint(server, $S, exchange -> {\n", TypeName.get(ApiSnippets.class),  prefixValue + "/" + info.name);
				CodeBlock.Builder builder = CodeBlock.builder();
				var varname = info.resolve(builder);
				registerMethod.addCode(builder.build());
				registerMethod.addStatement("return $L", varname);
				registerMethod.addStatement("})");
			}

			if (hasEndpoints) {
				String generatedClassName = type.getSimpleName().toString() + "Router";
				TypeSpec routerSpec = TypeSpec.classBuilder(generatedClassName)
						.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
						.addMethod(registerMethod.build())
						.build();

				write(routerSpec);
			}
		}

	}

	@Override
	public void onElements(Seq<Element> elements) throws Throwable {}


	public static String endpointName(String force, String s) {
		if(force != null) return force;
		StringBuilder result = new StringBuilder(s.length() + 1);
		for(int i = 0; i < s.length(); i++){
			char c = s.charAt(i);
			if(i > 0 && Character.isUpperCase(s.charAt(i))){
				result.append('/');
			}
			result.append(Character.toLowerCase(c));
		}
		return result.toString();
	}


}
