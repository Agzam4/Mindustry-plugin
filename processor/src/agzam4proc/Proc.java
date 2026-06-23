package agzam4proc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import com.squareup.javapoet.AnnotationSpec;

public class Proc {

	public static final String module = "agzam4gen.";

	public static boolean equals(TypeElement element, Class<?> cls) {
		return element.getQualifiedName().toString().equals(cls.getCanonicalName());
	}

	public static ExecutableElement findStaticMethod(TypeElement element, String name) {
	    for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
	        boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
	        boolean nameMatches = method.getSimpleName().contentEquals(name);
	        if (isStatic && nameMatches) {
	            return method;
	        }
	    }
	    throw new AptError(element, "Static method \"@\" not found", name);	
	}

	public static AnnotationSpec target(ElementType type) {
	    return AnnotationSpec.builder(Target.class)
	            .addMember("value", "$T.$L", ElementType.class, type.name())
	            .build();
	}

	public static DeclaredType findSuperType(TypeMirror typeMirror, String targetCanonicalName) {
	    if (!(typeMirror instanceof DeclaredType)) return null;
	    DeclaredType declaredType = (DeclaredType) typeMirror;
	    TypeElement element = (TypeElement) declaredType.asElement();
	    
	    if(element.getQualifiedName().toString().equals(targetCanonicalName)) return declaredType;
	    
	    DeclaredType superType = findSuperType(element.getSuperclass(), targetCanonicalName);
	    if(superType != null) return superType;
	    
	    for(TypeMirror iface : element.getInterfaces()) {
	        DeclaredType superInterface = findSuperType(iface, targetCanonicalName);
	        if(superInterface != null) return superInterface;
	    }
	    return null;
	}

	
}
