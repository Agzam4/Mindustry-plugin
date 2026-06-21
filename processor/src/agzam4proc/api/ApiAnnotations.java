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
		String value() default "";
		
	}

	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.PARAMETER)
	/** Returns name of parameter of upper function (null for endpoints if not set by {@link Parm}) */
	public @interface CallerParm {}

	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.PARAMETER)
	/** Redefines name of parameter (see also {@link CallerParm}) */
	public @interface Parm {
		String value();
	}

//  TODO
//	@Retention(RetentionPolicy.SOURCE)
//	@Target(ElementType.PARAMETER)
//	public @interface Default {
//		String value() default "";
//	}

	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE)
	public @interface Dependency {}

	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.METHOD)
	public @interface DependencyImpl {}
}
