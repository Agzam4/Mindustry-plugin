package agzam4proc.apt.api;

import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

import agzam4proc.BaseStep;
import agzam4proc.apt.api.ApiAnnotations.Type;
import agzam4proc.utils.Scheme;
import agzam4proc.utils.element.TypeElem;
import arc.struct.Seq;
import arc.util.Log;

public class SchemeStep extends BaseStep {

	private Scheme scheme;
	
	public SchemeStep(Scheme scheme) {
		this.scheme = scheme;
	}
	
	@Override
	public Seq<Class<?>> classes() {
		return Seq.with(Type.class);
	}

	@Override
	public Set<? extends Element> step() {
		for (Element e : getElements(Type.class)) {
			if (!(e instanceof TypeElement type)) continue;
			scheme.register(TypeElem.of(type));
		}
		scheme.eachinfo(i -> {
			var b = JsonBuilderProcessor.builder(processor.packageName + ".json", i);
			b.write("json", processor); 
//			Log.info("&lg+ @", b.type.name);
		});
		Log.info("&lg@ json builders generated", scheme.size());
		return ImmutableSet.of();
	}
	
	
}
