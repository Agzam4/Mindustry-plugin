package agzam4proc.api.utils;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;

import com.squareup.javapoet.TypeName;

import agzam4proc.api.ApiAnnotations.Type;
import arc.func.Cons;
import arc.struct.ObjectMap;

public class Scheme {

	private ObjectMap<TypeName, TypeInfo> schemes = ObjectMap.of();
	private Types typeUtils;
	
	public Scheme(Types typeUtils) {
		this.typeUtils = typeUtils;
	}
	
	public void register(TypeElement type) {
		var info = new TypeInfo(type);
		schemes.put(TypeName.get(type.asType()), info);
		if(typeUtils.asElement(type.getSuperclass()) instanceof TypeElement superclass) {
			if(superclass.getAnnotation(Type.class) == null) return;
			var supertype = TypeName.get(superclass.asType());
			if(!schemes.containsKey(supertype)) register(superclass);
			schemes.get(supertype).superclass(info);
		}
	}
	
	public void eachinfo(Cons<TypeInfo> cons) {
		schemes.each((t,i) -> cons.get(i));
	}
	
	
}
