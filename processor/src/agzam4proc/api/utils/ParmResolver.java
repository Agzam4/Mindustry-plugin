package agzam4proc.api.utils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import com.squareup.javapoet.TypeName;

import agzam4proc.AptError;
import agzam4proc.api.ApiAnnotations.CallerParm;
import agzam4proc.api.ApiAnnotations.Parm;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Nullable;

public class ParmResolver {
	
	final ObjectSet<ParmResolver> require = ObjectSet.with();
	final Seq<ExecutableElement> stack = Seq.with();
	
	ParmResolver next;
	
	@Nullable MethodInfo method;
	@Nullable TypeName allowed;
	boolean parm;
	final String name;
	
	public ParmResolver(DependenciesContext context, VariableElement parameter, ObjectSet<TypeName> allowed) {
		this.name = parameter.getAnnotation(Parm.class) == null ? parameter.getSimpleName().toString() : parameter.getAnnotation(Parm.class).value();
        TypeName paramTypeName = TypeName.get(context.typeUtils.erasure(parameter.asType()));
        
        boolean found = false;
	    for (var annotation : parameter.getAnnotationMirrors()) { // FIXME
			TypeElement annotationElement = (TypeElement) annotation.getAnnotationType().asElement();
			String annotationClassPath = annotationElement.getQualifiedName().toString();
			
			var typeName = TypeName.get(annotationElement.asType()).toString();
			
	        if(annotationClassPath.equals(CallerParm.class.getCanonicalName())) {
				if(found) throw new AptError(annotationElement, "Parametr can contains only one dependency annotation");
				this.parm = true;
				found = true;
	        	continue;
	        }
			if(context.hasDependency(typeName)) {
				if(found) throw new AptError(annotationElement, "Parametr can contains only one dependency annotation");
				var dependency = context.dependency(typeName);

				if(!dependency.methods.containsKey(paramTypeName)) 
					throw new AptError(
							parameter, 
							"Dependency \"@\" does not contain an implementation for \"@\" type", 
							dependency.name.toString(), 
							paramTypeName.toString()
							);
				
				this.method = dependency.methods.get(paramTypeName);
				this.require.addAll(this.method.resolve(allowed));
				found = true;
				continue;
			}
	    }
	    
	    this.allowed = allowed.find(t -> t.equals(paramTypeName));
	    if(this.allowed != null) return;
	    
	    if(!found) throw new AptError(parameter, String.format(
	    		"No provider found for \"%s\". Ensure a @Dependency exists that returns this type.", 
	    		parameter.getSimpleName()
	    		));

	}
}
