package agzam4proc.api.utils.init;

import com.squareup.javapoet.CodeBlock;

import agzam4proc.api.utils.CodeBlockBuilder;
import agzam4proc.api.utils.Interfaces.CodeProviderContext;

public class ConstProvider extends VariableInit {

	String value;
	
	public ConstProvider(String value) {
		this.value = value;
	}

	@Override
	public CodeBlockBuilder builder(CodeProviderContext context, CodeBlockBuilder builder) {
//		builder.addCode("$S", this.value);
		return builder;
	}
	
	@Override
	public CodeBlock result() {
		return CodeBlock.of("$L", value);
	}
	
	@Override
	public boolean eql(VariableInit other) {
		if(!(other instanceof ConstProvider s)) return false;
		return s.value.equals(value);
	}

}
