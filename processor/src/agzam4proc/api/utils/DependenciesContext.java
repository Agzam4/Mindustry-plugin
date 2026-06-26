package agzam4proc.api.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;

import com.squareup.javapoet.TypeName;
import com.sun.net.httpserver.HttpExchange;

import agzam4proc.BaseProcessor;
import agzam4proc.api.utils.element.*;
import arc.struct.*;

public class DependenciesContext {

	public final ObjectMap<TypeName, String> typeVars = ObjectMap.of(TypeName.get(HttpExchange.class), "exchange");
	public final ObjectSet<TypeName> allowedTypes = typeVars.keys().toSeq().asSet();

	public final String packageName;
	public final Types typeUtils;
	public ProcessingEnvironment processingEnv;
	public Scheme scheme;

	private ObjectMap<Typepath, DependencyInfo> dependencyCache = ObjectMap.of();


	public DependenciesContext(BaseProcessor processor) {
		this.packageName = processor.packageName;
		this.typeUtils = processor.typeUtils;
		this.processingEnv = processor.processingEnv();
		this.scheme = new Scheme(typeUtils);
	}

	public DependencyInfo addDependency(TypeElement type) {
		var info = new DependencyInfo(this, TypeElem.of(type));
		dependencyCache.put(info.annotation.path, info);
		return info;
	}

	public void resolve() {
		dependencyCache.each((t,d) -> d.resolve());
	}

	public boolean hasDependency(AnnotationElem<?> annotation) {
		return dependencyCache.containsKey(annotation.path);
	}

	public DependencyInfo dependency(AnnotationElem<?> annotation) {
		return dependencyCache.get(annotation.path);
	}

	public boolean hasDependency(Typepath path) {
		return dependencyCache.containsKey(path);
	}

	public DependencyInfo dependency(Typepath path) {
		return dependencyCache.get(path);
	}

}
