package agzam4.achievements;

import agzam4.Game;
import agzam4.bot.Bots;
import agzam4.bot.Bots.NotifyTag;
import agzam4.database.Database.PlayerEntity;
import arc.Core;
import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.serialization.Json;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.net.Administration.PlayerInfo;

public class AchievementsManager {
	

	public enum Achievement {

		meteoriteLaunch(),
		test(),
		none();
		

		public final int id;
		public final String bungleName;
		
		private Achievement() {
			id = ordinal();
			bungleName = Strings.camelToKebab(name());
		}

		public void reward(@Nullable PlayerEntity player, int tier, Object... args) {
			if(player == null) return;
			if(!player.achievement(this, tier)) return;
			
			@Nullable PlayerInfo info = Vars.netServer.admins.playerInfo.get(player.uuid);
			if(info == null) return;
			Object[] _args = new Object[args.length+2];
			for (int i = 0; i < args.length; i++) _args[i+2] = args[i];
			_args[0] = info.lastName;
			_args[1] = tier;
			
			Call.warningToast(0, format("on-reward", _args));
			Call.sendMessage(format("on-reward", _args));
			Bots.notify(NotifyTag.achievement, format("on-reward", _args));
		}

		public String format(String name, Object... args) {
			return Strings.format(Game.bungle("achievement.@.@", bungleName, name), args);
		}
	}
	
	public static ObjectMap<String, Integer> mapsIds;
	
	public static void init() {
		updateMaps();
	}

	@SuppressWarnings("unchecked")
	public static void updateMaps() {
    	Json json = new Json();
    	try {
        	mapsIds = json.fromJson(ObjectMap.class, Core.files.local("mapsIds.json"));
		} catch (Exception e) {
			mapsIds = new ObjectMap<String, Integer>();
		}
    	Vars.maps.all().each(map -> {
    		if(mapsIds.containsKey(map.name())) return;
    		for (int i = 0; i < mapsIds.size+1; i++) {
				if(mapsIds.containsValue(i, true)) continue;
	    		mapsIds.put(map.name(), i);
	    		return;
			}
    		Log.err("Map id not inited for @", map.name());
    	});
	}

	public static int mapId() {
		return AchievementsManager.mapsIds.get(Vars.state.map.name());
	}
	
	
//	public ObjectMap<String, PlayerAchievements> playersAchievements;
//	public Fi savePath = Core.files.local("achievements.json");
//	private ObjectMap<String, PlayerStatus> playersSatuses = null;
//	public boolean enabled = false;
	
//	public enum Achievement {
//		
//		CREATE_T5_UNDER_50_WAVE("\ue86d"),
//		SURVIVAL_250_WAVES("\ue84d");
//		
//		PlayerAchievements achievement;
//		String name;
//		String glyph;
//		
//		private Achievement(String glyph) {
//			this.glyph = glyph;
//			String str =  toString();
//			boolean needUpper = false;
//			name = "";
//			
//			for (int i = 0; i < str.length(); i++) {
//				char ch = str.charAt(i);
//				if(ch == '_') {
//					needUpper = true;
//					continue;
//				}
//				if(needUpper) {
//					name += Character.toUpperCase(ch);
//					needUpper = false;
//				} else {
//					name += Character.toLowerCase(ch);
//				}
//			}
//			System.out.println("Achievement: " + name);
//			achievement = new PlayerAchievements(name);
//		} 
//		
//		public String getName() {
//			return name;
//		}
//
//		public String getGlyph() {
//			return glyph;
//		}
//
//		public String getGlyphId() {
//			byte[] bs = glyph.getBytes();
//			StringBuilder sb = new StringBuilder(bs.length * 2);
//			for(byte b : bs) sb.append(String.format("%02x", b));
//			return sb.toString();
//		}
//	}
//	
//	
//	public AchievementsManager() {
//		load();
//		
//		Events.on(WorldLoadEndEvent.class, e -> {
//			playersSatuses = new ObjectMap<>();
//			
//			for (int i = 0; i < Groups.player.size(); i++) {
//				Player player = Groups.player.index(i);
//				String uidd = player.con.uuid;
//				if(playersSatuses.containsKey(uidd)) {
//					playersSatuses.get(uidd).worldLoadEnd();
//				} else {
//					playersSatuses.put(uidd, new PlayerStatus(this, uidd));
//				}
//			}
//		});
//		Events.on(UnitCreateEvent.class, e -> {
//			if(!enabled) return;
//			if(e.spawner != null) {
//				if(e.spawner.block == Blocks.tetrativeReconstructor) {
//					for (int i = 0; i < Groups.player.size(); i++) {
//						Player player = Groups.player.index(i);
//						if(player.name().equals(e.spawner.lastAccessed)) {
//							achievementCompleted(player.con.uuid, Achievement.CREATE_T5_UNDER_50_WAVE);
//							break;
//						}
//						String uidd = player.con.uuid;
//						if(playersSatuses.containsKey(uidd)) {
//							playersSatuses.get(uidd).unitCreate();
//						} else {
//							playersSatuses.put(uidd, new PlayerStatus(this, uidd));
//						}
//					}
//				}
//			}
//		});
//		
//		if(playersAchievements == null) {
//			playersAchievements = new ObjectMap<>();
//		}
//	}
//	
//
//    @SuppressWarnings("unchecked")
//	public boolean load() {
//    	if (!savePath.exists()) {
//			return false;
//		}
//    	try {
//        	Json json = new Json();
//    		playersAchievements = json.fromJson(ObjectMap.class, savePath);
//		} catch (Exception e) {
//			Log.err("Error on loading achievements: " + e.getMessage() + " (path: " + savePath.absolutePath() + ")");
//		}
//		if(playersAchievements == null) playersAchievements = new ObjectMap<>();
//		return true;
//	}
//	
//	public void save() {
//		Json json = new Json();
//		json.toJson(playersAchievements, savePath);
//	}
//	
//	int updates = 0;
//	public void update() {
//		if(!enabled) return;
//		if(playersSatuses == null) return;
//		updates++;
//		if(updates%60*5==0) {
//			for (int i = 0; i < Groups.player.size(); i++) {
//				Player player = Groups.player.index(i);
//				String uidd = player.con.uuid;
//				if(playersSatuses.containsKey(uidd)) {
//					playersSatuses.get(uidd).update(player);
//				} else {
//					playersSatuses.put(uidd, new PlayerStatus(this, uidd));
//				}
//			}
//		}
//		// Waves player
//		
//		
//	}
//
//
//	public boolean achievementCompleted(String uidd, Achievement achievement) {
//		if(!enabled) return false;
//		if(playersAchievements == null) {
//			playersAchievements = new ObjectMap<>();
//		}
//		if(!playersAchievements.containsKey(uidd)) playersAchievements.put(uidd, new PlayerAchievements(uidd));
//		PlayerAchievements achievements = playersAchievements.get(uidd);
//		if(achievements.achievementCompleted(achievement)) {
//			save();
//			return true;
//		}
//		return false;
//	}
}
