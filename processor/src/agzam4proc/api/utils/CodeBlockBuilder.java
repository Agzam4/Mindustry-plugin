package agzam4proc.api.utils;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;

import arc.struct.Seq;

public class CodeBlockBuilder {

	Builder root = CodeBlock.builder();
	Seq<Runnable> actions = Seq.with();
	
	
	public void addCode(String format, Object ...args) {
		validate(format, args);
		actions.add(() -> root.add(CodeBlock.of(format, args)));
	}
	
	public void addStatement(String format, Object... args) {
		validate(format, args);
		actions.add(() -> root.addStatement(format, args));
	}

	public void add(CodeBlockBuilder builder) {
		actions.add(() -> root.add(builder.build()));
	}

	public CodeBlock build() {
		for (var action : actions) action.run();
		return root.build();
	}
	
	private void validate(String format, Object ...args) {
		CodeBlock.of(format, args);
		int amount = 0;
		final String allowed = "TNLS";
		String fs = "";
		for (int i = 0; i < format.length()-1; i++) {
			if(format.charAt(i) != '$') continue;
			if(allowed.indexOf(format.charAt(i+1)) == -1) continue;
			fs += format.charAt(i+1);
			amount++;
		}
		if(amount != args.length) throw new RuntimeException("Formats: " + fs + " arguments: " + args.length + " (" + format + ")");
	}
	
}
