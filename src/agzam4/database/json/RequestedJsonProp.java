package agzam4.database.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import agzam4.Game;
import arc.func.Func2;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequestedJsonProp {

	JsonPropType value() default JsonPropType.JSON;

	public enum JsonPropType {
		JSON((r,v) -> v),
		UUID((r,v) -> r.sensitive ? v : Game.nameByUuid((String)v));

		public final Func2<JsonRequest, Object, Object> conv;
		private JsonPropType(Func2<JsonRequest, Object, Object> conv) {
			this.conv = conv;
		}
	}

}

