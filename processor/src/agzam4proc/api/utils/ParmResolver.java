package agzam4proc.api.utils;

import javax.lang.model.element.ExecutableElement;

import com.squareup.javapoet.TypeName;

import agzam4proc.api.ApiAnnotations.CallerParm;
import agzam4proc.api.ApiAnnotations.Parm;
import agzam4proc.api.utils.element.TypeElem;
import agzam4proc.api.utils.element.Typepath;
import agzam4proc.api.utils.element.VariableElem;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Nullable;

public class ParmResolver {

	final ObjectSet<ParmResolver> require = ObjectSet.with();
	final Seq<ExecutableElement> stack = Seq.with();

	ParmResolver next;

	public @Nullable MethodInfo method;
	public @Nullable TypeName allowed;
	public boolean parm;
	public final String name;

	public ParmResolver(DependenciesContext context, VariableElem parameter, ObjectSet<TypeName> allowed) {
		var parmAnn = parameter.getAnnotation(Parm.class);
		this.name = parmAnn == null ? parameter.name : parmAnn.value();

		TypeElem paramTypeName = parameter.type;

		boolean found = false;
		for (var adPath : parameter.annotations()) {
//			Typepath adPath = ad.path;

			if (adPath.equals(Typepath.of(CallerParm.class))) {
				if (found) throw parameter.error("Parametr can contains only one dependency annotation");
				this.parm = true;
				found = true;
				continue;
			}

			if (context.hasDependency(adPath)) {
				if (found) throw parameter.error("Parametr can contains only one dependency annotation");
				var dependency = context.dependency(adPath);

				if (!dependency.methods.containsKey(paramTypeName))
					throw parameter.error(
							"Dependency \"@\" does not contain an implementation for \"@\" type",
							dependency.name,
							paramTypeName.toString()
							);

				this.method = dependency.methods.get(paramTypeName);
				this.require.addAll(this.method.resolve(allowed));
				found = true;
				continue;
			}
		}

		this.allowed = allowed.find(t -> t.equals(paramTypeName.typeName));
		if (this.allowed != null) return;

		if (!found) throw parameter.error(String.format("No provider found for \"%s\". Ensure a @Dependency exists that returns this type.", parameter.name));

	}
}
