package agzam4proc.api.utils.element;

import java.lang.annotation.Annotation;

import javax.lang.model.element.Element;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import arc.struct.ObjectMap;
import arc.util.Nullable;

/**
 * Abstraction for real (compiled) and virtual (uncompiled) annotations<br>
 * Can be compared using {@code ==}
 * @param <A> the annotation type
 */
public class AnnotationElem<A extends Annotation> extends Elem {

	private static ObjectMap<Class<? extends Annotation>, AnnotationElem<?>> exsisting = ObjectMap.of();
	private static ObjectMap<TypeName, AnnotationElem<? extends Annotation>> virtual = ObjectMap.of();
	
	public A value;
//	public TypeName typeName; // Typepath instead
	public Typepath path;
	
	private AnnotationElem(Element e, A value) {
		init(e); // FIXME
		this.value = value;
//		this.typeName = TypeName.get(e.asType());
		this.path = Typepath.of(e.asType());
	}

	private AnnotationElem(TypeName type, Typepath path) {
//		init(null); XXX
//		this.typeName = type;
		this.path = path;
	}

	protected AnnotationElem() {
		init(null);
	}
	
	
	@Nullable
	@SuppressWarnings("unchecked")
	public Class<A> annotationClass() {
		if(value == null) return null;
		return (Class<A>) value.getClass();
	}

	/**
	 * Real annotation by element and value
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> AnnotationElem<A> of(Element e, A value) {
		if(exsisting.containsKey(value.getClass())) return (AnnotationElem<A>) exsisting.get(value.getClass());
		AnnotationElem<A> a = new AnnotationElem<A>(e, value);
		exsisting.put(value.getClass(), a);
		return a;
	}

	/**
	 * Virtual annotation by type name
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> AnnotationElem<A> of(String packageName, String simpleName, String... simpleNames) {
		var type = ClassName.get(packageName, simpleName, simpleNames);
		if(virtual.containsKey(type)) return (AnnotationElem<A>) virtual.get(type);
		var path = Typepath.of(packageName, join(simpleName, simpleNames));
		AnnotationElem<A> a = new AnnotationElem<A>(type, path);
		virtual.put(type, a);
		virtual.put(ClassName.get("", simpleName), a);
		return a;
	}

	private static String[] join(String first, String... rest) {
		String[] result = new String[1 + rest.length];
		result[0] = first;
		System.arraycopy(rest, 0, result, 1, rest.length);
		return result;
	}

	/**
	 * Virtual annotation by type name
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> AnnotationElem<A> virtual(String simpleName) {
		var type = ClassName.get("", simpleName);
		return (AnnotationElem<A>) virtual.get(type);
	}
	
}
