package agzam4proc.api.utils.element;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import com.squareup.javapoet.FieldSpec;

public class VariableElem extends Elem {

	public String name;
	public TypeElem type;

	private VariableElem() {}

	public static VariableElem of(VariableElement e) {
		VariableElem instance = new VariableElem();
		instance.init(e);
		return instance;
	}

	public static VariableElem virtual(String name, TypeElem type) {
		VariableElem instance = new VariableElem();
		instance.name = name;
		instance.type = type;
		return instance;
	}

	private void init(VariableElement e) {
		super.init(e);
		this.name = e.getSimpleName().toString();
		this.type = TypeElem.of(e.asType());
	}

	public FieldSpec build() {
		var b = FieldSpec.builder(type.typeName, name).addModifiers(Modifier.PUBLIC);
		return b.build();
	}
}
