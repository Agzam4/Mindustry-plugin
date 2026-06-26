package agzam4proc.api.utils.element;

import java.util.Arrays;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.squareup.javapoet.ClassName;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;

public final class Typepath {

	private static final ObjectMap<String, Typepath> cache = ObjectMap.of();

	/**
	 * Fallback cache for generated annotations (keyed without no package)<br>
	 * Populated when {@link #of(String, String...)} registers a type with a non-empty package<br>
	 * Looked up when {@link #of(TypeElement)} encounters unknown non-package/non-type (like <code>Symtab$6</code>) element<br>
	 * TODO: not lazy populating: when file created
	 */
	private static final ObjectMap<String, Typepath> simpleCache = ObjectMap.of();

	public final String type; // package.Class1.Class
	public final String binary; // package.Class1$Class
	public final String packageName; // package
	public final String simpleName; // Class
	public final String[] enclosing; // [Class1]

	private Typepath(String packageName, String[] classes) {
		this.packageName = packageName;
		this.simpleName = classes[classes.length - 1];
		this.enclosing = classes.length > 1 ? Arrays.copyOf(classes, classes.length - 1) : new String[0];
		String prefix = packageName.isEmpty() ? "" : packageName + ".";
		this.type = prefix + String.join(".", classes);
		this.binary = prefix + String.join("$", classes);
//		if(!isSystemPackage()) Log.info("New Typepath: @, @ (@)", packageName, classes, binary);
	}

	public static Typepath of(String packageName, String... classes) {
		if (classes.length == 0) throw new IllegalArgumentException("Need at least one class name");
		String key = packageName.isEmpty() ? String.join(".", classes) : packageName + "." + String.join(".", classes);
		if (cache.containsKey(key)) return cache.get(key);
		var tp = new Typepath(packageName, classes);
		cache.put(key, tp);
		if (!packageName.isEmpty() && !simpleCache.containsKey(tp.simpleName)) {
			simpleCache.put(tp.simpleName, tp);
		}
		return tp;
	}

	public static Typepath of(TypeElement type) {
		Element current = type;
		Seq<String> classes = new Seq<>();
		while (true) {
			if (current instanceof TypeElement te) {
				classes.insert(0, te.getSimpleName().toString());
				current = current.getEnclosingElement();
				continue;
			}
			if (current instanceof PackageElement pkg) {
				return of(pkg.getQualifiedName().toString(), classes.toArray(String.class));
			}
			// unknown element - generated annotation (not on classpath)
			String simple = classes.isEmpty() ? null : classes.get(classes.size - 1);
			if (simple != null && simpleCache.containsKey(simple)) {
				return simpleCache.get(simple);
			}
			Log.warn("Unknown element type: " + current.getClass());
			return null;
		}
	}

	public static Typepath of(Class<?> clz) {
		if (clz.isPrimitive()) return of("", clz.getName());
		String pkg = clz.getPackageName();
		Seq<String> classNames = new Seq<>();
		Class<?> current = clz;
		while (current != null && !current.isPrimitive()) {
			classNames.insert(0, current.getSimpleName());
			current = current.getEnclosingClass();
		}
		return of(pkg, classNames.toArray(String.class));
	}

	public static Typepath of(TypeMirror mirror) {
		if(mirror instanceof DeclaredType dt) return of((TypeElement) dt.asElement());
		if(mirror.getKind() == TypeKind.NONE) return null;
		if(mirror.getKind().isPrimitive()) return of("", mirror.toString());
		throw new IllegalArgumentException("Unsupported TypeMirror: " + mirror.getKind() + " " + mirror);
	}

	public static Typepath of(ClassName cn) {
		String[] names = cn.simpleNames().toArray(new String[0]);
		return of(cn.packageName(), names);
	}

	public ClassName toClassName() {
		if(enclosing.length == 0) return ClassName.get(packageName, simpleName);
		String[] all = Arrays.copyOf(enclosing, enclosing.length + 1);
		all[enclosing.length] = simpleName;
		return ClassName.get(packageName, all[0], Arrays.copyOfRange(all, 1, all.length));
	}

	public boolean isNested() {
		return enclosing.length > 0;
	}

	@Override
	public boolean equals(Object o) {
		return o == this;
	}

	@Override
	public int hashCode() {
		return type.hashCode();
	}

	@Override
	public String toString() {
		return type;
	}
	
	public boolean isSystemPackage() {
		return packageName.startsWith("java.") 
				|| packageName.startsWith("sun.") 
				|| packageName.startsWith("jdk.")
				|| packageName.startsWith("com.sun.") 
				|| packageName.startsWith("arc.");
	}
	
}
