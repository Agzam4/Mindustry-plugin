package agzam4proc.api.utils;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.TypeName;

import agzam4proc.api.utils.element.TypeElem;
import agzam4proc.api.utils.element.VariableElem;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;

public class TypeInfo {

	public final TypeElem type;
	public final TypeName typeName;
	
	public TypeInfo superclass;
	public final ObjectMap<TypeName, TypeInfo> subclasses = ObjectMap.of();
	public final Seq<VariableElem> fields;
	public final Seq<VariableElem> allfields;
	
	public TypeInfo(TypeElem type) {
		this.type = type;
		this.typeName = type.typeName;
		this.fields = type.fields.select(f -> !f.hasModifiers(Modifier.STATIC) && !f.hasModifiers(Modifier.TRANSIENT));
		this.allfields = Seq.with(fields);
	}

	public void superclass(TypeInfo info) {
		info.superclass = this;
		subclasses.put(info.typeName, info);
	}
	
	public void eachfield(Cons<? super VariableElem> e) {
		fields.each(e);
		if(superclass != null) superclass.eachfield(e);
	}
	
}
