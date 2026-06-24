package agzam4proc.api.utils.init;

import com.squareup.javapoet.CodeBlock;

import agzam4proc.api.utils.CodeBlockBuilder;
import agzam4proc.api.utils.Interfaces.CodeProviderContext;

public class NullProvider extends VariableInit {

	public NullProvider() {}

	@Override
	public CodeBlockBuilder builder(CodeProviderContext context, CodeBlockBuilder builder) {
		return builder;
	}
	
	@Override
	public CodeBlock result() {
		return CodeBlock.of("$L", "null");
	}
	
	@Override
	public boolean eql(VariableInit other) {
		return other instanceof NullProvider;
	}

}
