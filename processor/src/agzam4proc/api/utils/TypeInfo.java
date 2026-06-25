package agzam4proc.api.utils;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

import com.squareup.javapoet.TypeName;

import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;

public class TypeInfo {

	public final TypeElement type;
	public final TypeName typeName;
	
	public TypeInfo superclass;
	public final ObjectMap<TypeName, TypeInfo> subclasses = ObjectMap.of();
	public final Seq<VariableElement> fields;
	public final Seq<VariableElement> allfields;
	
	public TypeInfo(TypeElement type) {
		this.type = type;
		this.typeName = TypeName.get(type.asType());
		this.fields = Seq.with(ElementFilter.fieldsIn(type.getEnclosedElements()))
				.select(f -> !f.getModifiers().contains(Modifier.STATIC) && !f.getModifiers().contains(Modifier.TRANSIENT));
		this.allfields = Seq.with(fields);
	}

	public void superclass(TypeInfo info) {
		info.superclass = this;
		subclasses.put(info.typeName, info);
	}
	
	public void eachfield(Cons<? super VariableElement> e) {
		fields.each(e);
		if(superclass != null) superclass.eachfield(e);
	}
	
}
