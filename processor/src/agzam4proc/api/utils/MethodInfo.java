package agzam4proc.api.utils;

import com.squareup.javapoet.TypeName;

import agzam4proc.api.utils.element.ExecutableElem;
import agzam4proc.api.utils.element.TypeElem;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;

public class MethodInfo implements Equality<MethodInfo> {
	
	private final ExecutableElem method;
	public final String name;
	public final TypeElem cls;
	public final DependenciesContext context;

	public final TypeElem returnType;
	public final TypeName enclosingType;
	
	public MethodInfo(DependenciesContext context, TypeElem cls, ExecutableElem method) {
		this.context = context;
		this.method = method;
		this.cls = cls;
		this.name = method.name;
		this.returnType = method.returnType;
		this.enclosingType = method.enclosingType;
	}
	
	public Seq<ParmResolver> resolvers = null;
	
	public Seq<ParmResolver> resolve(ObjectSet<TypeName> allowed) {
		if(resolvers != null) return resolvers;
		Log.info("Resolving @:@", cls.name, name);
		resolvers = new Seq<>();
		for (var parm : method.parameters()) {
			resolvers.add(new ParmResolver(context, parm, allowed));
		}
		return resolvers;
	}

	@Override
	public boolean eql(MethodInfo other) {
		return method.equals(other.method);
	}
	
}
