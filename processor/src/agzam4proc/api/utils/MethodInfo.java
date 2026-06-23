package agzam4proc.api.utils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import com.squareup.javapoet.TypeName;

import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;

public class MethodInfo {
	
	final ExecutableElement method;
	final String name;
	final TypeElement cls;
	private DependenciesContext context;
	
	public MethodInfo(DependenciesContext context, TypeElement cls, ExecutableElement method) {
		this.context = context;
		this.method = method;
		this.cls = cls;
		this.name = method.getSimpleName().toString();
	}
	
	Seq<ParmResolver> resolvers = null;
	
	protected Seq<ParmResolver> resolve(ObjectSet<TypeName> allowed) {
		if(resolvers != null) return resolvers;
		Log.info("Resolving @:@", cls.getSimpleName(), name);
		resolvers = new Seq<>();
		for (var parm : method.getParameters()) {
			resolvers.add(new ParmResolver(context, parm, allowed));
		}
		return resolvers;
	}
	
}
