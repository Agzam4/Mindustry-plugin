package agzam4proc.apt.api;

import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.google.common.collect.ImmutableSet;

import agzam4proc.BaseStep;
import agzam4proc.apt.api.ApiAnnotations.Dependency;
import agzam4proc.utils.DependenciesContext;
import agzam4proc.utils.element.TypeElem;
import arc.struct.Seq;
import arc.util.Log;

public class DependenciesStep extends BaseStep {

	private DependenciesContext context;
	
	public DependenciesStep(DependenciesContext context) {
		this.context = context;
	}
	
	
	@Override
	public Seq<Class<?>> classes() {
		return Seq.with(Dependency.class);
	}
	
	@Override
	public Seq<String> generatedClasses() {
		return context.dependencyCache.keys().toSeq().map(t -> t.binary);
	}

    private boolean updateTypes = false;
	
	@Override
	public Set<? extends Element> step() {
		if(updateTypes) {
			// XXX: cannot resolve() because element() is another?
			
			// Rebuild with correctly compiled methods
	        for (Element e : getElements(Dependency.class)) {
	            if (!(e instanceof TypeElement type)) continue;
	        	TypeElem.of(type).update(type);
	            context.addDependency(type).buildAnnotation();
	        }
			context.dependencyCache.each((t,i) -> i.resolve());
			return ImmutableSet.of();
		}
        Set<Element> dependencies = getElements(Dependency.class);
        for (Element e : dependencies) {
            if (!(e instanceof TypeElement type)) continue;
            var an = context.addDependency(type).buildAnnotation();
            processor.write("dependencies", an, type);
            Log.info("&g+ @ &lg(from @)", an.name, type.getSimpleName());
        }
        updateTypes = true;
        return dependencies;
	}

}
