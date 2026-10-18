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

	/** The timeframe after a ban expires during which any new ban is multiplied */
	public static int escalationTime = 60*25;
	
	// --- Players -------------------------------------------- //
	
	/** Votekick ban duration in minutes */
	public static int votekickBanDuration = 60;
	
	/** Minimum map playtime to start voting*/
	public static int votekickRequiredMapPlaytime = 5;

	/** Minimum server playtime to start voting*/
	public static int votekickRequiredTotalPlaytime = 15;

	/** Minimum map playtime to vote in existing votekick*/
	public static int voteRequiredMapPlaytime = 0;
	
	/** Minimum server playtime to vote in existing votekick*/
	public static int voteRequiredTotalPlaytime = 5;
}
