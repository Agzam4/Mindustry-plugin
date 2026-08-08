package agzam4proc.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import agzam4proc.Proc;
import agzam4proc.apt.api.ApiAnnotations.DependencyImpl;
import agzam4proc.apt.api.ApiAnnotations.GeneratedDependency;
import agzam4proc.apt.api.ApiAnnotations.RequireBody;
import agzam4proc.utils.element.AnnotationElem;
import agzam4proc.utils.element.TypeElem;
import arc.struct.ObjectMap;

public class DependencyInfo {
	
	public final DependenciesContext context;
	public TypeElem type;
	public final ObjectMap<TypeElem, MethodInfo> methods = ObjectMap.of();
	public String name;
	public AnnotationElem<?> annotation;
	public boolean requireBody;
	
	public DependencyInfo(DependenciesContext context, TypeElem type) {
		this.context = context;
		init(type);
	}
	
	public void init(TypeElem type) {
		this.type = type;
		String name = type.name;
		if(!name.endsWith("Dependency")) throw type.error("Dependency class name must ends with \"Dependency\"");
		name = name.substring(0, name.length() - "Dependency".length());

		annotation = AnnotationElem.of(context.packageName + ".dependencies", name);
		
		this.name = name;
		this.requireBody = type.hasAnnotation(RequireBody.class);
		
		 for (var method : type.methods) {
			 if(!method.hasModifiers(Modifier.STATIC)) continue;
			 if(!method.hasAnnotation(DependencyImpl.class)) continue;

//			 var returnType =; //context.typeUtils.erasure(method.getReturnType());
			 var returnTypeName =  method.returnType; //TypeName.get(returnType);
			 
			 if(returnTypeName.typeName.equals(TypeName.VOID)) throw method.error("Method cannot return void");
			 if(methods.containsKey(returnTypeName)) throw method.error("Duplicate of dependency return type: \"@\"", returnTypeName.toString());
			 
			 methods.put(returnTypeName, new MethodInfo(context, type, method));
		 }
		 if(methods.size == 0) throw type.error("No methods with @ annotation found", DependencyImpl.class);
	}
	
	public TypeSpec buildAnnotation() {
		String doc = context.processingEnv.getElementUtils().getDocComment(type.element());
		return TypeSpec.annotationBuilder(this.name)
				.addJavadoc("Auto-generated annotation based on {@link $T}$L", this.type.typeName, doc == null ? "" : "<br>\n<br>\n" + doc.trim())
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Proc.target(ElementType.PARAMETER))
				.addAnnotation(AnnotationSpec.builder(GeneratedDependency.class) .addMember("value", "$T.class",this.type.typeName).build())
				.build();
	}
	

	public void resolve() {
		methods.each((t,m) -> m.resolve(context.allowedTypes));
	}
	
}
