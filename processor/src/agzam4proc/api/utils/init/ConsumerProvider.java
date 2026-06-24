package agzam4proc.api.utils.init;

import com.squareup.javapoet.CodeBlock;
import agzam4proc.api.utils.CodeBlockBuilder;
import agzam4proc.api.utils.Interfaces.CodeProviderContext;
import arc.struct.Seq;

public class ConsumerProvider extends VariableInit {
	
	private transient CodeBlock func;
	private transient String prefname;
	private transient String name;
	private transient VariableInit[] args;
	
	public ConsumerProvider(String prefname, CodeBlock func, VariableInit ...args) {
		this.args = args;
		this.prefname = prefname;
		this.func = func;
	}
	
	@Override
	public CodeBlockBuilder builder(CodeProviderContext context, CodeBlockBuilder builder) {
		name = context.namespace.get(prefname);

        Seq<CodeBlock> argumentBlocks = Seq.with();
        for (int i = 0; i < args.length; i++) argumentBlocks.add(args[i].result());
        argumentBlocks.add(CodeBlock.of("$L -> {$>", name));
        
		builder.addCode("$L($L\n", func, CodeBlock.join(argumentBlocks, ", ")
				);
		CodeBlockBuilder inside = new CodeBlockBuilder();
		builder.add(inside);
		builder.addStatement("$<})");
		return inside;
	}

	@Override
	public CodeBlock result() {
		return CodeBlock.of("$L", name);
	}

	@Override
	public String toString() {
		return prefname + "->";
	}
	
	@Override
	public boolean eql(VariableInit other) {
		if(!(other instanceof ConsumerProvider cons)) return false;
		return cons == this;
	}
	
}
