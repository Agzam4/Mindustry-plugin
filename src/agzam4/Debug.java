package agzam4;

import arc.graphics.Color;
import arc.math.geom.Point2;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.gen.Call;
import mindustry.net.Administration.Config;

public class Debug {

	
	public static boolean debug = false;
	
	public static void effect(Point2 point) {
		effect(point.x, point.y);
	}
	
	public static void effect(int x, int y) {
		effect(x, y, 1);
	}
	
	public static void effect(int x, int y, int size) {
		if(configDebug()) Call.effect(Fx.rotateBlock, x*Vars.tilesize, y*Vars.tilesize, size, Color.white);
	}

	public static boolean configDebug() {
		return Config.debug.bool();
	}
	
}
