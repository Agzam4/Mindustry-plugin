package agzam4.mcp;

import agzam4.Game;
import agzam4.utils.Strs;
import arc.util.Strings;

public class McpUlits {

	public static String strip(String s) {
		return Game.strip(s);
	}

	public static String escapedStrip(String s) {
		return Strs.escape(strip(s));
	}
	
	public static String snakeCase(String s) {
		return Strings.camelToKebab(s).replace('-', '_');
	}
	
	
}
