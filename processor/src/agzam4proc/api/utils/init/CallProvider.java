package agzam4proc.api.utils.init;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import agzam4proc.api.utils.CodeBlockBuilder;
import agzam4proc.api.utils.Interfaces.CodeProviderContext;
import agzam4proc.api.utils.MethodInfo;
import arc.struct.Seq;

public class CallProvider extends VariableInit {

	private transient String name;
	private transient boolean asReturn;
	
	public final MethodInfo method;
	private transient String prefname;
	
	public CallProvider(MethodInfo method) {
		this.method = method;
        String baseName = method.cls.getSimpleName().toString();
        baseName = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);
        if(baseName.endsWith("Dependency")) baseName = baseName.substring(0, baseName.length() - "Dependency".length());
        this.prefname = baseName;
	}
	
	public CallProvider asReturn() {
		asReturn = true;
		return this;
	}
	
	
	@Override
	public CodeBlockBuilder builder(CodeProviderContext context, CodeBlockBuilder builder) {
		var node = context.node;

        StringBuilder nameBuilder = new StringBuilder(prefname);
        for(int i = 0; i < node.children.size; i++) {
        	if(node.children.get(i).value instanceof StringProvider string) nameBuilder.append(toCamelCase(string.value));
        }
        
        this.name = context.namespace.get(nameBuilder.toString());

        // building arguments
        Seq<CodeBlock> argumentBlocks = Seq.with();
        for (int j = 0; j < node.children.size; j++) {
            var child = node.children.get(j);
            argumentBlocks.add(child.value.result());
        }

        var returnType = method.method.getReturnType();
        var enclosingType = method.method.getEnclosingElement().asType();

        String currentVarName = this.name;
        if(asReturn) {
	        builder.addStatement("return $T.$N($L)", 
	                TypeName.get(enclosingType),
	                method.name,
	                CodeBlock.join(argumentBlocks, ", ")
	        );
        	return builder;
        }
        builder.addStatement("$T $N = $T.$N($L)", 
                TypeName.get(returnType), 
                currentVarName,
                TypeName.get(enclosingType),
                method.name,
                CodeBlock.join(argumentBlocks, ", ")
        );
		return builder;
	}
	
	@Override
	public CodeBlock result() {
		return CodeBlock.of("$L", name);
	}
	
	@Override
	public boolean eql(VariableInit other) {
		if(!(other instanceof CallProvider o)) return false;
		return o.method.method.equals(method.method);
	}

	@Override
	public String toString() {
		return (asReturn ? "r-" : "") + prefname;
	}
	
}
