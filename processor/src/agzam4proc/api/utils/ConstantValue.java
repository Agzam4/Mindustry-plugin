package agzam4proc.api.utils;

import com.squareup.javapoet.CodeBlock;

public class ConstantValue {

	public final String value;
	
	private enum Types {
		
		string("$S"), varriable("$L");
		
		private String format;

		Types(String format) {
			this.format = format;
		}
		
	}
	
	private final Types types;
	
	public ConstantValue(Types types, String value) {
		this.value = value;
		this.types = types;
	}
	
	
	public static ConstantValue varriable(String name) {
		return new ConstantValue(Types.varriable, name);
	}


	public static ConstantValue none() {
		return new ConstantValue(Types.varriable, null);
	}


	public static ConstantValue string(String value) {
		return new ConstantValue(Types.string, value);
	}


	public boolean isString() {
		return types == Types.string;
	}


	public CodeBlock asCode() {
		return CodeBlock.of(types.format, value);
	}
	
	@Override
	public String toString() {
		return asCode().toString();
	}

}
