package agzam4proc.api.utils;

import arc.struct.ObjectMap;

public class Namespace {

	public static Namespace of(String ...vars) {
		Namespace n = new Namespace();
		for (var v : vars) n.get(v);
		return n;
	}

	private ObjectMap<String, Integer> names = ObjectMap.of();
	
	public Namespace() {

	}

	public String get(String name) {
        if (names.containsKey(name)) {
            int count = names.get(name) + 1;
            names.put(name, count);
            return name + count;
        } else {
        	names.put(name, 1);
        	return name;
        }
	}
	
	
	
	
	
}
