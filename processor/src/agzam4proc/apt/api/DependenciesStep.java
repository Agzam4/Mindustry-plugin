package agzam4proc.apt.api;

import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.google.common.collect.ImmutableSet;

import agzam4proc.BaseStep;
import agzam4proc.apt.api.ApiAnnotations.Dependency;
import agzam4proc.utils.DependenciesContext;
import agzam4proc.utils.element.TypeElem;
import arc.struct.ObjectSet;
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

    private int lastAmont = -1;
    
    ObjectSet<TypeElem> generated = ObjectSet.with();
	
	@Override
	public Set<? extends Element> step() {
        Set<Element> dependencies = getElements(Dependency.class);
		if(lastAmont == dependencies.size()) {
			// XXX: cannot resolve() because element() is another?
			
			// Rebuild with correctly compiled methods
	        for (Element e : dependencies) {
	            if (!(e instanceof TypeElement type)) continue;
	        	TypeElem.of(type).update(type);
	            context.addDependency(type).buildAnnotation();
	        }
			context.dependencyCache.each((t,i) -> i.resolve());
			return ImmutableSet.of();
		}
        for (Element e : dependencies) {
            if (!(e instanceof TypeElement type)) continue;
            var el = TypeElem.of(type);
            if(generated.contains(el)) continue;
            generated.add(el);
            var an = context.addDependency(type).buildAnnotation();
            processor.write("dependencies", an, type);
            Log.info("&g+ @ &lg(from @)", an.name, type.getSimpleName());
        }
        lastAmont = dependencies.size();
        return dependencies;
	}

}
