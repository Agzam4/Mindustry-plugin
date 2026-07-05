package agzam4proc.api.utils;

import com.squareup.javapoet.TypeName;

import agzam4proc.api.utils.element.ExecutableElem;
import agzam4proc.api.utils.element.TypeElem;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;

public class MethodInfo implements Equality<MethodInfo> {
	
	public final ExecutableElem method;
	public final TypeElem cls;
	public final DependenciesContext context;

	public final String name;
	
	public MethodInfo(DependenciesContext context, TypeElem cls, ExecutableElem method) {
		this.context = context;
		this.method = method;
		this.cls = cls;
		this.name = method.name;
	}
	
	public Seq<ParmResolver> resolvers = null;
	
	public Seq<ParmResolver> resolve(ObjectSet<TypeName> allowed) {
		if(resolvers != null) return resolvers;
		Log.info("Resolving @:@", cls.name, name);
		resolvers = new Seq<>();
		for (var parm : method.parms) {
			resolvers.add(new ParmResolver(context, parm, allowed));
		}
		return resolvers;
	}
	
	public ObjectSet<String> bodyArgs() {
		ObjectSet<String> bodyArgs = ObjectSet.with();
		for (var r : resolvers) {
			if(r.body) bodyArgs.add(r.name);
			if(r.method != null) {
				bodyArgs.addAll(r.method.bodyArgs());
			}
		}
		return bodyArgs;
	}
	

	@Override
	public boolean eql(MethodInfo other) {
		return method.equals(other.method);
	}
	
	public TypeName enclosingType() {
		return method.enclosingType;
	}

	public TypeElem returnType() {
		return method.returnType;
	}
	
}
