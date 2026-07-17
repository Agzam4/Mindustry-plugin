package agzam4proc.api.utils.element;

import java.util.Arrays;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;

public class TypeElem extends Elem {

	private static final ObjectMap<Typepath, TypeElem> existing = ObjectMap.of();

	private static TypeElem primitive(String name) {
		Typepath path = Typepath.of("", name);
		TypeElem e = new TypeElem();
		existing.put(path, e);
		e.typepath = path;
		e.typeName = switch (name) {
			case "int" -> TypeName.INT;
			case "long" -> TypeName.LONG;
			case "float" -> TypeName.FLOAT;
			case "double" -> TypeName.DOUBLE;
			case "boolean" -> TypeName.BOOLEAN;
			case "byte" -> TypeName.BYTE;
			case "short" -> TypeName.SHORT;
			case "char" -> TypeName.CHAR;
			case "void" -> TypeName.VOID;
			default -> throw new IllegalArgumentException("Unknown primitive: " + name);
		};
		e.name = name;
		e.methods = new Seq<>();
		e.fields = new Seq<>();
		return e;
	}

	public static final TypeElem typeInt = primitive("int"), 
			typeLong = primitive("long"), 
			typeFloat = primitive("float"), 
			typeDouble = primitive("double"), 
			typeBoolean = primitive("boolean"),
			typeByte = primitive("byte"), 
			typeShort = primitive("short"), 
			typeChar = primitive("char"),
			typeVoid = primitive("void");

	public Typepath typepath;
	public TypeName typeName;
	
	public String name;
	public Seq<ExecutableElem> methods;
	public Seq<VariableElem> fields;

	private Typepath superclassPath;
	private TypeElem superclass;
	
	private int dimension;
	private TypeElem componentType;

	private TypeElem() {}

	public static TypeElem of(Class<?> clz) {
	    if (clz == int.class)     return typeInt;
	    if (clz == long.class)    return typeLong;
	    if (clz == float.class)   return typeFloat;
	    if (clz == double.class)  return typeDouble;
	    if (clz == boolean.class) return typeBoolean;
	    if (clz == byte.class)    return typeByte;
	    if (clz == short.class)   return typeShort;
	    if (clz == char.class)    return typeChar;
	    if (clz == void.class)    return typeVoid;
	    
		return of(ClassName.get(clz));
	}
	
	public static TypeElem of(TypeElement e) {
		Typepath path = Typepath.of(e);
		if (existing.containsKey(path)) return existing.get(path);
		TypeElem instance = new TypeElem();
		existing.put(path, instance);
		instance.init(e);
		return instance;
	}

	private void init(TypeElement e) {
		super.init(e);
		this.name = e.getSimpleName().toString();
		this.typepath = Typepath.of(e);
		this.typeName = TypeElem.toClassName(typepath);
		this.methods = Seq.with(ElementFilter.methodsIn(e.getEnclosedElements()))
				.map(m -> ExecutableElem.of(m));
		this.fields = Seq.with(ElementFilter.fieldsIn(e.getEnclosedElements()))
				.map(f -> VariableElem.of(f));
		TypeMirror superMirror = e.getSuperclass();
		if (superMirror.getKind() != TypeKind.NONE && !superMirror.toString().equals("java.lang.Object")) {
			this.superclassPath = Typepath.of(superMirror);
		}
	}

	public static TypeElem of(TypeMirror mirror) {
		if (mirror.getKind() == TypeKind.NONE) return null;
		return switch (mirror.getKind()) {
			case BOOLEAN -> typeBoolean;
			case BYTE -> typeByte;
			case SHORT -> typeShort;
			case INT -> typeInt;
			case LONG -> typeLong;
			case CHAR -> typeChar;
			case FLOAT -> typeFloat;
			case DOUBLE -> typeDouble;
			case VOID -> typeVoid;
			case ARRAY -> {
				int depth = 0;
				TypeMirror m = mirror;
				while(m.getKind() == TypeKind.ARRAY) {
					depth++;
					m = ((ArrayType)m).getComponentType();
				}
				var component = of(m);
				yield component != null ? arrayOf(component, depth) : null;
			}
			default -> {
				if (mirror instanceof DeclaredType dt) yield of((TypeElement) dt.asElement());
//				Log.warn("Unknow mirror type: @", mirror);
				yield null;
			}
		};
	}

	public static TypeElem of(String packageName, String simpleName, String... simpleNames) {
		Typepath path = simpleNames.length == 0
				? Typepath.of(packageName, simpleName)
				: Typepath.of(packageName, join(simpleName, simpleNames));
		if (existing.containsKey(path)) return existing.get(path);
		TypeElem instance = new TypeElem();
		existing.put(path, instance);
		instance.typepath = path;
		instance.typeName = TypeElem.toClassName(path);
		instance.name = path.simpleName;
		instance.methods = new Seq<>();
		instance.fields = new Seq<>();
		return instance;
	}

	public static TypeElem of(ClassName className) {
		Typepath path = Typepath.of(className);
		if (existing.containsKey(path)) return existing.get(path);
		TypeElem instance = new TypeElem();
		existing.put(path, instance);
		instance.typepath = path;
		instance.typeName = className;
		instance.name = path.simpleName;
		instance.methods = new Seq<>();
		instance.fields = new Seq<>();
		return instance;
	}

	public static TypeElem virtual(String simpleName) {
		for (var entry : existing) {
			if (entry.value.name.equals(simpleName) && entry.value.isVirtual()) return entry.value;
		}
		return null;
	}

	public TypeElem superclass() {
		if (superclass != null) return superclass;
		if (superclassPath == null) return null;
		if (existing.containsKey(superclassPath)) {
			superclass = existing.get(superclassPath);
		}
		return superclass;
	}

	public void superclass(TypeElem parent) {
		this.superclass = parent;
		this.superclassPath = parent.typepath;
	}

	public boolean isArray() { return componentType != null; }

	public TypeElem componentType() { return componentType; }
	
	public TypeElem noDimension() { return componentType == null ? this : componentType; }

	/**
	 * @param component
	 * @param depth - <code>T</code> for <code>0</code>, <code>T[]</code> for <code>1</code>, <code>T[][]</code> for <code>2</code>
	 * @return
	 */
	public static TypeElem arrayOf(TypeElem component, int depth) {
		if(depth <= 0) return component;
		Typepath path = Typepath.arrayOf(component.typepath, depth);
		if(existing.containsKey(path)) return existing.get(path);
		var elem = new TypeElem();
		existing.put(path, elem);
		elem.typepath = path;
		elem.typeName = arrayTypeName(component.typeName, depth);
		elem.name = component.name + "[]".repeat(depth);
		elem.dimension = depth;
		elem.componentType = depth > 1 ? arrayOf(component, depth - 1) : component;
		elem.methods = new Seq<>();
		elem.fields = new Seq<>();
		return elem;
	}

	private static TypeName arrayTypeName(TypeName component, int depth) {
		return depth <= 0 ? component : ArrayTypeName.of(arrayTypeName(component, depth - 1));
	}

	public VariableElem field(String name) {
		return fields.find(f -> f.name.equals(name));
	}

	public ExecutableElem method(String name) {
		return methods.find(m -> m.name.equals(name));
	}

	public void addMethod(ExecutableElem method) {
		methods.add(method);
	}

	public void addField(VariableElem field) {
		fields.add(field);
	}

	private static String[] join(String first, String... rest) {
		String[] result = new String[1 + rest.length];
		result[0] = first;
		System.arraycopy(rest, 0, result, 1, rest.length);
		return result;
	}

	private static ClassName toClassName(Typepath path) {
		if(path.enclosing.length == 0) return ClassName.get(path.packageName, path.simpleName);
		String[] all = java.util.Arrays.copyOf(path.enclosing, path.enclosing.length + 1);
		all[path.enclosing.length] = path.simpleName;
		return ClassName.get(path.packageName, all[0], Arrays.copyOfRange(all, 1, all.length));
	}

	public String packageName() {
		return typepath.packageName;
	}

	public TypeSpec build() {
		if(element() != null && element().getKind() == ElementKind.ANNOTATION_TYPE) {
			var b = TypeSpec.annotationBuilder(name).addModifiers(Modifier.PUBLIC);
			for(var method : methods) b.addMethod(method.build());
			return b.build();
		}
		var b = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
		TypeElem sup = superclass();
		if(sup != null) b.superclass(sup.typeName);
		for(var field : fields) {
			// XXX: generate call of real for non-virtual?
			if(field.isVirtual()) b.addField(field.build());
		}
		for (var method : methods) {
			// XXX: generate call of real for non-virtual?
			if(method.isVirtual()) b.addMethod(method.build());
		}
		return b.build();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + typepath.binary + ":" + "[]".repeat(dimension) + ")";
	}
	
	public int dimension() {
		return dimension;
	}
	
}
