package agzam4proc.api.utils;

import com.squareup.javapoet.TypeName;

import agzam4proc.api.ApiAnnotations.Type;
import agzam4proc.api.utils.element.TypeElem;
import arc.func.Cons;
import arc.struct.ObjectMap;

public class Scheme {

	private ObjectMap<TypeName, TypeInfo> schemes = ObjectMap.of();
	
	public void register(TypeElem type) {
		var info = new TypeInfo(type);
		schemes.put(type.typeName, info);
		var sup = type.superclass();
		if(sup == null || sup.getAnnotation(Type.class) == null) return;
		if(!schemes.containsKey(sup.typeName)) register(sup);
		schemes.get(sup.typeName).superclass(info);
	}
	
	public void eachinfo(Cons<TypeInfo> cons) {
		schemes.each((t,i) -> cons.get(i));
	}

	public TypeInfo get(TypeName typeName) {
		return schemes.get(typeName);
	}

	
	
}
