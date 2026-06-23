package agzam4proc.api.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;

import com.squareup.javapoet.TypeName;
import com.sun.net.httpserver.HttpExchange;

import agzam4proc.BaseProcessor;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;

public class DependenciesContext {

	public final ObjectMap<TypeName, String> typeVars = ObjectMap.of(TypeName.get(HttpExchange.class), "exchange");
	public final ObjectSet<TypeName> allowedTypes = typeVars.keys().toSeq().asSet();
	
	public final String packageName;
	public final Types typeUtils;
	public ProcessingEnvironment processingEnv;
	
	private ObjectMap<String, DependencyInfo> dependencyCache = ObjectMap.of();

	
	public DependenciesContext(BaseProcessor processor) {
		this.packageName = processor.packageName;
		this.typeUtils = processor.typeUtils;
		this.processingEnv = processor.processingEnv();
	}

	public DependencyInfo addDependency(TypeElement type) {
		var info = new DependencyInfo(this, type);
		dependencyCache.put(info.annotation, info);
		dependencyCache.put(info.name, info); // FIXME
		return info;
	}

	public void resolve() {
		dependencyCache.each((t,d) -> d.resolve());
	}

	public boolean hasDependency(String typeName) {
		return dependencyCache.containsKey(typeName);
	}

	public DependencyInfo dependency(String typeName) {
		return dependencyCache.get(typeName);
	}
	
}
