package example.achievements;

import arc.struct.ObjectMap;
import arc.util.Log;
import example.achievements.AchievementsManager.Achievement;
import mindustry.content.Blocks;
import mindustry.content.Items;

public class PlayerAchievements {

	public String uidd;
	// name GlyphId
	public ObjectMap<String, String> achievements;
	
	public PlayerAchievements(String uidd) {
		this.uidd = uidd;
		achievements = new ObjectMap<String, String>();
	}

	public boolean achievementCompleted(Achievement achievement) {
		if(!achievements.containsKey(achievement.name)) {
			Log.info("Player [" + uidd + "] completed achievement: " + achievement.name + " #" + achievement.getGlyphId());
			achievements.put(achievement.name, achievement.getGlyphId());
			return true;
		}
		return false;
	}
}
