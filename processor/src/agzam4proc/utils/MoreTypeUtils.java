package agzam4proc.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import agzam4proc.utils.element.TypeElem;
import arc.func.Func;
import arc.struct.ObjectMap;

public enum MoreTypeUtils {

	typeByte {{
	    typeElem = TypeElem.typeByte; 
	    schemeType = "integer";
	    valueOf = o -> CodeBlock.of("$T.valueOf($L)", ClassName.get(Byte.class), o);
	}}, 
	typeShort {{
	    typeElem = TypeElem.typeShort; 
	    schemeType = "integer"; 
	    valueOf = o -> CodeBlock.of("$T.valueOf($L)", ClassName.get(Short.class), o);
	}}, 
	typeInt {{
	    typeElem = TypeElem.typeInt; 
	    schemeType = "integer"; 
	    valueOf = o -> CodeBlock.of("$T.valueOf($L)", ClassName.get(Integer.class), o);
	}}, 
	typeLong {{
	    typeElem = TypeElem.typeLong; 
	    schemeType = "integer"; 
	    valueOf = o -> CodeBlock.of("$T.valueOf($L)", ClassName.get(Long.class), o);
	}}, 
	typeFloat {{
	    typeElem = TypeElem.typeFloat; 
	    schemeType = "number"; 
	    valueOf = o -> CodeBlock.of("$T.valueOf($L)", ClassName.get(Float.class), o);
	}}, 
	typeDouble {{
	    typeElem = TypeElem.typeDouble; 
	    schemeType = "number"; 
	    valueOf = o -> CodeBlock.of("$T.valueOf($L)", ClassName.get(Double.class), o);
	}}, 
	typeBoolean {{
	    typeElem = TypeElem.typeBoolean; 
	    schemeType = "boolean"; 
	    valueOf = o -> CodeBlock.of("$T.valueOf($L)", ClassName.get(Boolean.class), o);
	}}, 
	typeString {{
	    typeElem = TypeElem.of(String.class); 
	    schemeType = "string"; 
	    valueOf = o -> CodeBlock.of("$T.valueOf($L)", ClassName.get(String.class), o);
	}};

	public TypeElem typeElem;
	public String schemeType;
	public Func<Object,CodeBlock> valueOf;

	public static MoreTypeUtils of(TypeElem elem) {
		for (var e : values()) {
			if(e.typeElem == elem) return e;
		}
		return null;
	}
	
}
