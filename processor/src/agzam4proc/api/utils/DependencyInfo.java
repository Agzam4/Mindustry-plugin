package agzam4proc.api.utils;

import java.lang.annotation.ElementType;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import agzam4proc.AptError;
import agzam4proc.Proc;
import agzam4proc.api.ApiAnnotations.DependencyImpl;
import arc.struct.ObjectMap;

public class DependencyInfo {
	
	final DependenciesContext context;
	final TypeElement type;
	final ObjectMap<TypeName, MethodInfo> methods = ObjectMap.of();
	final String name;
	final String annotation;
	
	public DependencyInfo(DependenciesContext context, TypeElement type) {
		this.context = context;
		this.type = type;
		
		String name = type.getSimpleName().toString();
		if(!name.endsWith("Dependency")) throw new AptError(type, "Dependency class name must ends with \"Dependency\"");
		name = name.substring(0, name.length() - "Dependency".length());

		annotation = context.packageName + ".dependencies." + name;
		
		this.name = name;
		
		 for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
			 if(!method.getModifiers().contains(Modifier.STATIC)) continue;
			 if(method.getAnnotation(DependencyImpl.class) == null) continue;

			 var returnType = context.typeUtils.erasure(method.getReturnType());
			 var returnTypeName = TypeName.get(returnType);
			 
			 if(returnType.getKind() == TypeKind.VOID) throw new AptError(method, "Method cannot return void");
			 if(methods.containsKey(returnTypeName)) throw new AptError(method, "Duplicate of dependency return type: \"@\"", returnTypeName.toString());
			 
			 methods.put(returnTypeName, new MethodInfo(context, type, method));
		 }
		 if(methods.size == 0) throw new AptError(type, "No methods with @ annotation found", DependencyImpl.class);
	}
	
	public TypeSpec buildAnnotation() {
		String doc = context.processingEnv.getElementUtils().getDocComment(type);
		return TypeSpec.annotationBuilder(this.name)
				.addJavadoc("Auto-generated annotation based on {@link $T}$L", this.type, doc == null ? "" : "<br>\n<br>\n" + doc.trim())
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Proc.target(ElementType.PARAMETER))
				.build();
	}
	

	protected void resolve() {
		methods.each((t,m) -> m.resolve(context.allowedTypes));
	}
	
}
