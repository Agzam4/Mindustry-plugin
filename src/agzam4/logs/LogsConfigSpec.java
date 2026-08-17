package agzam4.logs;

import agzam4proc.apt.config.ConfigAnnotations.Config;

@Config("logs")
public class LogsConfigSpec {

	/** Enables or disables new log creation **/
	public static boolean logging = true;
	
	/** Number database log rows before rotation */
	public static int maxRows = 50_000;
	
}
