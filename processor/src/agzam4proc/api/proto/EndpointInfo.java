package agzam4proc.api.proto;

import java.lang.reflect.Method;

import agzam4proc.api.utils.MethodInfo;
import agzam4proc.api.utils.element.*;
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
