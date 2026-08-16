package agzam4.mcp;

import agzam4.Game;
import agzam4.utils.Strs;
import arc.util.Strings;

public class McpUlits {

	public static String strip(String s) {
		s = Game.strip(s);
	    s = s.replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "[IP_REDACTED]");
	    s = s.replaceAll("\\b[A-Za-z0-9+/]{16,}={0,2}\\b", "[UUID_REDACTED]");
	    s = s.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b", "[EMAIL]");
	    s = s.replaceAll("https?://[^\\s]+", "[URL]");
	    s = s.replace("[", "⟨").replace("]", "⟩").replace("<", "‹").replace(">", "›");
		return s;
	}

	public static String escapedStrip(String s) {
		return Strs.escape(strip(s));
	}
	
	public static String snakeCase(String s) {
		return Strings.camelToKebab(s).replace('-', '_');
	}
	
	
}
