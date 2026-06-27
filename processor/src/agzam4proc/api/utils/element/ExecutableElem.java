package agzam4proc.api.utils.element;


import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import agzam4proc.api.utils.CodeBlockBuilder;
import arc.struct.Seq;

public class ExecutableElem extends Elem {

	public String name;
	public TypeElem returnType;
	public TypeName enclosingType;
	public Seq<VariableElem> parms;
	
	private ExecutableElem() {}

	public static ExecutableElem of(ExecutableElement e) {
		ExecutableElem instance = new ExecutableElem();
		instance.init(e);
		return instance;
	}

	public static ExecutableElem virtual(String name, TypeElem returnType, TypeName enclosingType, Seq<VariableElem> parms) {
		ExecutableElem instance = new ExecutableElem();
		instance.name = name;
		instance.returnType = returnType;
		instance.enclosingType = enclosingType;
		instance.parms = parms != null ? parms : new Seq<>();
		return instance;
	}

	private void init(ExecutableElement e) {
		super.init(e);
		this.name = e.getSimpleName().toString();
		this.returnType = TypeElem.of(e.getReturnType());
		this.enclosingType = TypeName.get(e.getEnclosingElement().asType());
		this.parms = Seq.with(e.getParameters()).map(p -> VariableElem.of(p));
	}

	public void addStatement(String format, Object... args) {
		if (body == null) body = new CodeBlockBuilder();
		body.addStatement(format, args);
	}

	public MethodSpec build() {
		var b = MethodSpec.methodBuilder(name).addModifiers(Modifier.PUBLIC);
		if(returnType != null) b.returns(returnType.typeName);
		for(var parm : parms) {
			if(parm.type == null) continue;
			b.addParameter(parm.type.typeName, parm.name);
		}
		if(body != null) b.addCode(body.build());
		return b.build();
	}

	@Override
	public boolean equals(Object o) {
		if(o == this) return true;
		if(!(o instanceof ExecutableElem other)) return false;
		return element() != null && element().equals(other.element());
	}

	@Override
	public int hashCode() {
		return element() != null ? element().hashCode() : name.hashCode();
	}
}
