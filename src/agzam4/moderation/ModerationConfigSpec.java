package agzam4.moderation;

import agzam4proc.apt.config.ConfigAnnotations.Config;

@Config("moderation")
public class ModerationConfigSpec {

	/** Initial moderator ban duration in minutes */
	public static int startBanTime = 5;
	
	/** Maximum moderator ban duration in minutes */
	public static int maxBanTime = 60;
	
	/** Ban duration multiplier for repeated offenses */
	public static float banTimeMultiplier = 2;

	/** Votekick ban duration in minutes */
	public static int votekickBanDuration = 60;
}
