package agzam4proc.utils.init;

import com.squareup.javapoet.CodeBlock;

import agzam4proc.utils.CodeBlockBuilder;
import agzam4proc.utils.Interfaces.CodeProviderContext;

public class StringProvider extends VariableInit {

	String value;
	
	public StringProvider(String value) {
		this.value = value;
	}

	@Override
	public CodeBlockBuilder builder(CodeProviderContext context, CodeBlockBuilder builder) {
//		builder.addCode("$S", this.value);
		return builder;
	}
	
	@Override
	public CodeBlock result() {
		return CodeBlock.of("$S", value);
	}
	
	@Override
	public boolean eql(VariableInit other) {
		if(!(other instanceof StringProvider s)) return false;
		return s.value.equals(value);
	}

}
