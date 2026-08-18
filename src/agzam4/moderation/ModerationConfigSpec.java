package agzam4.moderation;

import agzam4proc.apt.config.ConfigAnnotations.Config;

@Config("moderation")
public class ModerationConfigSpec {

	// --- Moderators ----------------------------------------- //
	
	/** Initial moderator ban duration in minutes */
	public static int startBanTime = 5;
	
	/** Maximum moderator ban duration in minutes */
	public static int maxBanTime = 60;
	
	/** Ban duration multiplier for repeated offenses */
	public static float banTimeMultiplier = 2;

	// --- Players -------------------------------------------- //
	
	/** Votekick ban duration in minutes */
	public static int votekickBanDuration = 60;
	
	/** Minimum map playtime to start voting*/
	public static int votekickRequiredMapPlaytime = 5;

	/** Minimum server playtime to start voting*/
	public static int votekickRequiredTotalPlaytime = 15;
	
}
