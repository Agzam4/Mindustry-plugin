package agzam4proc.apt.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class ConfigAnnotations {

	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE)
	public @interface Config {

		/** Name of config file **/
		String value();
		
	}
	
	
	
	
	
}
