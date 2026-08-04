package agzam4proc.apt.config;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import agzam4proc.BaseProcessor;
import agzam4proc.apt.config.ConfigAnnotations.Config;
import agzam4proc.utils.element.TypeElem;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;

public class ConfigProcessor extends BaseProcessor {

	@Override
	public Seq<Class<?>> classes() {
		return Seq.with(Config.class);
	}

	@Override
	public void onElement(ObjectMap<Class<?>, Seq<Element>> map) throws Throwable {

		if(round == 1) {
			Log.info("&lc Phase 1: Generating config managers");
			for (var config : map.get(Config.class)) {
				if (!(config instanceof TypeElement type)) continue;
				var elem = TypeElem.of(type);
				
			}
		}
	}
	
}
