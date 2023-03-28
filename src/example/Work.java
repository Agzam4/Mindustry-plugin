package example;

import mindustry.content.UnitTypes;
import mindustry.type.UnitType;

public class Work {

	public static boolean isPlayerUnit(UnitType unit) {
		return unit == UnitTypes.alpha || unit == UnitTypes.beta || unit == UnitTypes.gamma
				|| unit == UnitTypes.evoke || unit == UnitTypes.incite || unit == UnitTypes.emanate;
	}
}
