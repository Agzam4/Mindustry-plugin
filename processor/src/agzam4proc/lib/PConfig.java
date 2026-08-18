package agzam4proc.lib;

import arc.func.Cons;
import arc.func.Prov;
import arc.struct.Seq;

public class PConfig {

    public static final Seq<PConfig> all = new Seq<>();
	
    public final Object defaultValue;
    public final String key, description;
    public final Prov<Object> get;
    public final Cons<String> set;

    public PConfig(String name, String description, Object def, Prov<Object> get, Cons<String> set){
    	this.key = name;
    	this.description = description;
    	this.defaultValue = def;
    	this.get = get;
    	this.set = set;
    	all.add(this);
    }

    public boolean isNum(){
        return defaultValue instanceof Integer;
    }

    public boolean isBool(){
        return defaultValue instanceof Boolean;
    }

    public boolean isString(){
        return defaultValue instanceof String;
    }

	public Object get() {
		return get.get();
	}

	public void set(String value) {
		set.get(value);
	}
	
	@Override
	public String toString() {
		return "Config-" + key;
	}
    
}
