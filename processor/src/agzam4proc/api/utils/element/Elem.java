package agzam4proc.api.utils.element;

import java.lang.annotation.Annotation;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import agzam4proc.AptError;
import agzam4proc.api.utils.CodeBlockBuilder;
import agzam4proc.api.utils.Namespace;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;

/**
 * Abstraction for real (compiled) and virtual (uncompiled) annotations
 */
public class Elem {

	private Element element;

	protected ObjectSet<Typepath> annotations = ObjectSet.with();
	protected ObjectSet<Modifier> modifiers = ObjectSet.with();
	
	/** Generated body (for virtual elements) */
	public CodeBlockBuilder body;
	/** Namespace for body (for virtual elements) */
	public Namespace namespace = new Namespace();

	protected Elem() {}

	/**
	 * Must be called by subclass {@code of()} AFTER the instance is placed in the cache<br>
	 * This prevents stackoverflow error {@code init()} itself calls {@code of()} for related elements (superclass, field types, etc.)
	 */
	protected void init(Element element) {
		Objects.requireNonNull(element);
		this.element = element;
		this.modifiers = ObjectSet.with(Seq.with(element.getModifiers()));

		for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
			Typepath path = Typepath.of(mirror.getAnnotationType());
			if (path == null) {
				Log.warn("Cannot resolve annotation type path for @ on @", mirror.getAnnotationType(), element);
				continue;
			}
			if (annotations.contains(path)) continue;
			annotations.add(path);
		}
	}

	public void addStatement(String format, Object... args) {
		if (body == null) body = new CodeBlockBuilder();
		body.addStatement(format, args);
	}

	public void addCode(String format, Object... args) {
		if (body == null) body = new CodeBlockBuilder();
		body.addCode(format, args);
	}
	
	public Element element() {
		return element;
	}
	
	public boolean isVirtual() {
		return element == null;
	}

	public boolean hasAnnotation(Typepath path) {
		return annotations.contains(path);
	}

	public boolean hasAnnotation(Class<? extends Annotation> a) {
		return element != null && element.getAnnotation(a) != null;
	}

	public <A extends Annotation> A getAnnotation(Class<A> a) {
		if (element == null) return null;
		return element.getAnnotation(a);
	}

	public Seq<Typepath> annotations() {
		return annotations.toSeq();
	}

	public boolean hasModifiers(Modifier... m) {
		for (int i = 0; i < m.length; i++)
			if (modifiers.contains(m[i])) return true;
		return false;
	}

	public Elem addModifiers(Modifier... m) {
		for (int i = 0; i < m.length; i++) modifiers.add(m[i]);
		return this;
	}
	
	public AptError error(String message, Object... args) {
		return new AptError(element, message, args);
	}

}
