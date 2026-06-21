package agzam4proc.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class ApiAnnotations {

	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE)
	public @interface Router {

		/** Prefix of router (for example: /router)*/
		String value();
		
	}

	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.METHOD)
	public @interface Post {

		/** Post endpoint */
		String value();
		
	}

	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.PARAMETER)
	public @interface Parm {

		/** Returns name of parameter */
		String value() default "";
		
	}
	
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE)
	public @interface Dependency {}
	
}
