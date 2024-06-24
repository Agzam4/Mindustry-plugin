package example.achievements;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.serialization.Json;
import mindustry.content.Blocks;
import mindustry.game.EventType.UnitCreateEvent;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class AchievementsManager {
	
	// /achievements

	public ObjectMap<String, PlayerAchievements> playersAchievements;
	public Fi savePath = Core.files.local("achievements.json");

	private ObjectMap<String, PlayerStatus> playersSatuses = null;
	
	public boolean enabled = false;
	
	public enum Achievement {
		
		CREATE_T5_UNDER_50_WAVE("\ue86d"),
		SURVIVAL_250_WAVES("\ue84d");
		
		PlayerAchievements achievement;
		String name;
		String glyph;
		
		private Achievement(String glyph) {
			this.glyph = glyph;
			String str =  toString();
			boolean needUpper = false;
			name = "";
			
			for (int i = 0; i < str.length(); i++) {
				char ch = str.charAt(i);
				if(ch == '_') {
					needUpper = true;
					continue;
				}
				if(needUpper) {
					name += Character.toUpperCase(ch);
					needUpper = false;
				} else {
					name += Character.toLowerCase(ch);
				}
			}
			System.out.println("Achievement: " + name);
			achievement = new PlayerAchievements(name);
		} 
		
		public String getName() {
			return name;
		}

		public String getGlyph() {
			return glyph;
		}

		public String getGlyphId() {
			byte[] bs = glyph.getBytes();
			StringBuilder sb = new StringBuilder(bs.length * 2);
			for(byte b : bs) sb.append(String.format("%02x", b));
			return sb.toString();
		}
	}
	
	
	public AchievementsManager() {
		load();
		
		Events.on(WorldLoadEndEvent.class, e -> {
			playersSatuses = new ObjectMap<>();
			
			for (int i = 0; i < Groups.player.size(); i++) {
				Player player = Groups.player.index(i);
				String uidd = player.con.uuid;
				if(playersSatuses.containsKey(uidd)) {
					playersSatuses.get(uidd).worldLoadEnd();
				} else {
					playersSatuses.put(uidd, new PlayerStatus(this, uidd));
				}
			}
		});
		Events.on(UnitCreateEvent.class, e -> {
			if(!enabled) return;
			if(e.spawner != null) {
				if(e.spawner.block == Blocks.tetrativeReconstructor) {
					for (int i = 0; i < Groups.player.size(); i++) {
						Player player = Groups.player.index(i);
						if(player.name().equals(e.spawner.lastAccessed)) {
							achievementCompleted(player.con.uuid, Achievement.CREATE_T5_UNDER_50_WAVE);
							break;
						}
						String uidd = player.con.uuid;
						if(playersSatuses.containsKey(uidd)) {
							playersSatuses.get(uidd).unitCreate();
						} else {
							playersSatuses.put(uidd, new PlayerStatus(this, uidd));
						}
					}
				}
			}
		});
		
		if(playersAchievements == null) {
			playersAchievements = new ObjectMap<>();
		}
	}
	

    @SuppressWarnings("unchecked")
	public boolean load() {
    	if (!savePath.exists()) {
			return false;
		}
    	try {
        	Json json = new Json();
    		playersAchievements = json.fromJson(ObjectMap.class, savePath);
		} catch (Exception e) {
			Log.err("Error on loading achievements: " + e.getMessage() + " (path: " + savePath.absolutePath() + ")");
		}
		if(playersAchievements == null) playersAchievements = new ObjectMap<>();
		return true;
	}
	
	public void save() {
		Json json = new Json();
		json.toJson(playersAchievements, savePath);
	}
	
	int updates = 0;
	public void update() {
		if(!enabled) return;
		if(playersSatuses == null) return;
		updates++;
		if(updates%60*5==0) {
			for (int i = 0; i < Groups.player.size(); i++) {
				Player player = Groups.player.index(i);
				String uidd = player.con.uuid;
				if(playersSatuses.containsKey(uidd)) {
					playersSatuses.get(uidd).update(player);
				} else {
					playersSatuses.put(uidd, new PlayerStatus(this, uidd));
				}
			}
		}
		// Waves player
		
		
	}


	public boolean achievementCompleted(String uidd, Achievement achievement) {
		if(!enabled) return false;
		if(playersAchievements == null) {
			playersAchievements = new ObjectMap<>();
		}
		if(!playersAchievements.containsKey(uidd)) playersAchievements.put(uidd, new PlayerAchievements(uidd));
		PlayerAchievements achievements = playersAchievements.get(uidd);
		if(achievements.achievementCompleted(achievement)) {
			save();
			return true;
		}
		return false;
	}
}
