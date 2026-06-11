package agzam4.logs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import agzam4.Game;
import arc.func.Func;

public class LogsAnnotations {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface JsonProp {}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Sensitive {
		
		SensitiveProtector value();
		
		enum SensitiveProtector {
			
			ip(ip -> null),
			uuid(Game::nameByUuid);
			
			Func<String, String> func;
			
			private SensitiveProtector(Func<String, String> func) {
				this.func = func;
			}
			
			
		}
			
		
	}
	
}
