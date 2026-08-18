package agzam4.maps;

import agzam4proc.apt.config.ConfigAnnotations.Config;

@Config("maps")
public class MapsConfigSpec {

	/** Is custom maps system enabled */
	public static boolean enabled = true;

	/** Minimum map playtime to start voting*/
	public static int skipmapRequiredMapPlaytime = 0;

	/** Minimum server playtime to start voting*/
	public static int skipmapRequiredTotalPlaytime = 5;
	
	
}
