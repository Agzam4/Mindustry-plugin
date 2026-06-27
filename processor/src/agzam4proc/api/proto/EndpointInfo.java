package agzam4proc.api.proto;

import agzam4proc.api.utils.element.*;
import arc.struct.Seq;

public class EndpointInfo {

	public final String url;
	public final TypeElem returnType;
	public final Seq<VariableElem> params;

	public EndpointInfo(String url, TypeElem returnType, Seq<VariableElem> params) {
		this.url = url;
		this.returnType = returnType;
		this.params = params;
	}

}
