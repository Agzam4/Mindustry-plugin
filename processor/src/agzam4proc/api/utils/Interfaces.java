package agzam4proc.api.utils;

import agzam4proc.api.utils.init.VariableInit;

public class Interfaces {
	

	public interface NameProvider {
		
		public String name();
		
	}
	
	public static class CodeProviderContext {
		
		public DagNode<VariableInit> node;
		public Namespace namespace = new Namespace();
		
		public CodeProviderContext(DagNode<VariableInit> node, Namespace namespace) {
			this.node = node;
			this.namespace = namespace;
		}
		
		
	}
	
	public interface CodeProvider {
		
		public CodeBlockBuilder build(CodeProviderContext context, CodeBlockBuilder builder);
		
	}

	
}