package agzam4proc.apt.api.proto;

import java.lang.reflect.Method;

import agzam4proc.utils.MethodInfo;
import agzam4proc.utils.element.*;
import arc.struct.Seq;

public class EndpointInfo {

	public final String url;
	public final TypeElem returnType;
	public final Seq<VariableElem> params;
	
	public final MethodInfo info;

	public EndpointInfo(String url, TypeElem returnType, MethodInfo info, Seq<VariableElem> params) {
		this.url = url;
		this.returnType = returnType;
		this.params = params;
		this.info = info;
	}

}
