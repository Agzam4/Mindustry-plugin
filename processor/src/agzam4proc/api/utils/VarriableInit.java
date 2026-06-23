package agzam4proc.api.utils;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import agzam4proc.api.utils.Interfaces.CodeProvider;
import agzam4proc.api.utils.Interfaces.CodeProviderContext;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;

public class VarriableInit {
	
	String name = "<unset>";
	@Nullable MethodInfo info;

	public VarriableInit(String name, MethodInfo info) {
		this.name = name;
		this.info = info;
	}
	
	public VarriableInit(MethodInfo info, boolean returnValue) {
		this.info = info;
		this.codeProvider = new CodeProvider() {
			
			@Override
			public CodeBlockBuilder build(CodeProviderContext context, CodeBlockBuilder builder) {
				var node = context.node;
		        if(node.isConstant()) return builder;

		        String baseName = info.cls.getSimpleName().toString();
		        baseName = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);
		        if (baseName.endsWith("Dependency")) {
		            baseName = baseName.substring(0, baseName.length() - "Dependency".length());
		        }
		        
		        StringBuilder nameWithConstants = new StringBuilder(baseName);
		        if (node.children != null) {
		            for (int j = 0; j < node.children.size; j++) {
		                var child = node.children.get(j);
		                if (child.isConstant() && child.value.isString()) {
		                    String constStr = child.value.value;
		                    if (!constStr.isEmpty() && !constStr.equals("ROOT")) {
		                    	 nameWithConstants.append(toCamelCase(constStr));
		                    }
		                }
		            }
		        }
		        
		        String derivedName = nameWithConstants.toString();

		        // name contains duplicates -> name<number>
		        String finalVarName = derivedName;
		        if (context.namespace.containsKey(derivedName)) {
		            int count = context.namespace.get(derivedName) + 1;
		            context.namespace.put(derivedName, count);
		            finalVarName = derivedName + count;
		        } else {
		        	context.namespace.put(derivedName, 1);
		        }
		        node.payload.name = finalVarName;

		        // building arguments
		        Seq<CodeBlock> argumentBlocks = Seq.with();
		        for (int j = 0; j < node.children.size; j++) {
		            var child = node.children.get(j);
		            if(child.isConstant()) {
	                    argumentBlocks.add(child.value.asCode());
	                    continue;
		            }
		            argumentBlocks.add(CodeBlock.of("$L", child.payload.name));
		        }

		        var returnType = info.method.getReturnType();
		        var enclosingType = info.method.getEnclosingElement().asType();

		        String currentVarName = node.payload.name;
		        if(returnValue) {
			        builder.addStatement("return $T.$N($L)", 
			                TypeName.get(enclosingType),
			                info.name,
			                CodeBlock.join(argumentBlocks, ", ")
			        );
		        	return builder;
		        }
		        builder.addStatement("$T $N = $T.$N($L)", 
		                TypeName.get(returnType), 
		                currentVarName,
		                TypeName.get(enclosingType),
		                info.name,
		                CodeBlock.join(argumentBlocks, ", ")
		        );
		    
		        
				return builder;
			}
			
		};
	}
	
	CodeProvider codeProvider;
	
	public VarriableInit(CodeProvider codeProvider) {
		this.codeProvider = codeProvider;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	public CodeBlockBuilder build(CodeProviderContext context, CodeBlockBuilder current) {
		return codeProvider.build(context, current);
	}
	
	@Override
	public String toString() {
		return Strings.format("@:@", name, info == null ? "none" : info.cls.getSimpleName());
	}

	private static String toCamelCase(String input) {
	    if (input == null || input.isEmpty()) return "";
	    StringBuilder sb = new StringBuilder();
	    boolean nextUpper = true;
	    for (int i = 0; i < input.length(); i++) {
	        char c = input.charAt(i);
	        if (Character.isLetterOrDigit(c)) {
	            if (nextUpper) {
	                sb.append(Character.toUpperCase(c));
	                nextUpper = false;
	            } else {
	                sb.append(c);
	            }
	        } else {
	            nextUpper = true;
	        }
	    }
	    return sb.toString();
	}

}
