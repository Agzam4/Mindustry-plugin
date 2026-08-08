package agzam4proc;

import java.util.Set;

import javax.lang.model.element.Element;

import com.google.auto.common.BasicAnnotationProcessor.Step;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

import arc.struct.Seq;
import arc.util.Log;

public abstract class BaseStep implements Step {

	public BaseProcessor processor;

	public abstract Seq<Class<?>> classes();
	
	public Seq<String> generatedClasses() {
		return Seq.with();
	}

	@Override
	public Set<String> annotations() {
		var s = generatedClasses().addAll(classes().map(c -> c.getCanonicalName()));
//		Log.info("@ target annotations: &lb@", getClass().getSimpleName(), s);
		return Set.of(s.toArray(String.class));
	}
	
	private ImmutableSetMultimap<String, Element> elementsByAnnotation;

	@Override
	public final Set<? extends Element> process(ImmutableSetMultimap<String, Element> elementsByAnnotation) {
		Log.info("&lm=== @ (@) ===", getClass().getSimpleName(), elementsByAnnotation.size());
		this.elementsByAnnotation = elementsByAnnotation;
		try {
			return step();
		} catch (AptError e) {
			processor.err(e.element, e);
			throw e;
		} catch (Throwable e) {
			processor.err(null, e);
			throw e;
		}
	}


	public Set<? extends Element> step() {
		return null;
	}

	protected ImmutableSet<Element> getElements(Class<?> clz) {
		return elementsByAnnotation.get(clz.getCanonicalName());
	}
	
	protected ImmutableSet<Element> getElements(String s) {
		return elementsByAnnotation.get(s);
	}
	
	protected ImmutableSet<Element> all() {
		Seq<Element> all = new Seq<>();
		elementsByAnnotation.forEach((k,v) -> all.add(v));
		return ImmutableSet.copyOf(all.toArray(Element.class));
		
	}
}
