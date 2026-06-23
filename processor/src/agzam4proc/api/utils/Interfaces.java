package agzam4proc.api.utils;

import arc.struct.ObjectMap;

public class Interfaces {
	

	public interface NameProvider {
		
		public String name();
		
	}
	
	public static class CodeProviderContext {
		
		public DagNode<VarriableInit, ConstantValue> node;
		public ObjectMap<String, Integer> namespace = new ObjectMap<>();
		
		public CodeProviderContext(DagNode<VarriableInit, ConstantValue> node, ObjectMap<String, Integer> namespace) {
			this.node = node;
			this.namespace = namespace;
		}
		
		
	}
	
	public interface CodeProvider {
		
		public CodeBlockBuilder build(CodeProviderContext context, CodeBlockBuilder builder);
		
	}

	
}