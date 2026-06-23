package agzam4proc.api.utils;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;

import arc.struct.Seq;
import arc.util.Log;

public class CodeBlockBuilder {

	Builder root = CodeBlock.builder();
	Seq<Runnable> actions = Seq.with();
	
	
	public void addCode(String format, Object ...args) {
		actions.add(() -> root.add(CodeBlock.of(format, args)));
	}
	
	public void addStatement(String format, Object... args) {
		actions.add(() -> root.addStatement(format, args));
	}

	public void add(CodeBlockBuilder builder) {
		actions.add(() -> root.add(builder.build()));
	}

	public CodeBlock build() {
		for (var action : actions) action.run();
		return root.build();
	}
}
