package agzam4proc.api.utils.init;

import com.squareup.javapoet.CodeBlock;
import agzam4proc.api.utils.CodeBlockBuilder;
import agzam4proc.api.utils.Equality;
import agzam4proc.api.utils.Interfaces.CodeProviderContext;

public abstract class VariableInit implements Equality<VariableInit> {

	public VariableInit() {}

	/**
	 * Initialization of variable builder
	 */
	public abstract CodeBlockBuilder builder(CodeProviderContext context, CodeBlockBuilder builder);
	
	/**
	 * Variable name / value
	 */
	public abstract CodeBlock result();
	
	public static String toCamelCase(String input) {
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

	@Override
	public boolean eql(VariableInit other) {
		return other == this;
	}

}
