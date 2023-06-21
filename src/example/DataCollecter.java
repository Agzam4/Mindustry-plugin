package example;

import java.util.Calendar;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration.PlayerInfo;

public class DataCollecter {

	public static final String FILENAME = "agzam_s_plugin_statistics.json";
	public static final String FILENAME_ADMINS = "agzam_s_plugin_admins.json";

	private transient long sleepTime = 60*60*5; // 5 min

	private int dayMaxOnlineCount = 0;
	private int onlineCount = 0;
	private String mapName = "Loading...";
	private int waveNumber = 0;
	private int messagesCount = 0;

	private String[][] locations = new String[24][];
	private String[][] playerData = new String[30][5];
	
	public void init() {
		
	}
	
	public void messageEvent(Player player, String text) {
		messagesCount++;
	}

	private transient boolean isCollecting;
	private transient int day;
	
	public void collect() {
		isCollecting = true;
		updates = 0;
	}
	
	int updates = 0;
	
	public void update() {
		if(isCollecting) {
			updates++;
			
			if(updates > sleepTime) {
				updates = 0;
				collecData();
				save();
			}
		}
	}
	
	private transient int lastHours = -1;
	
	private void collecData() {
		onlineCount = Groups.player.size();
		waveNumber = Vars.state.wave;
		mapName = Vars.state.map.name();
		
		dayMaxOnlineCount = Math.max(dayMaxOnlineCount, onlineCount);
		if(day != getDayOfWeek()) {
			day = getDayOfWeek();
			dayMaxOnlineCount = 0;
		}
		
		
		for (int i = 0; i < Math.min(Groups.player.size(), playerData.length); i++) {
			Player player = Groups.player.index(i);
//			playerData

			playerData[i][0] = player.coloredName();
			playerData[i][1] = player.usid();
			playerData[i][2] = player.uuid();
			playerData[i][3] = player.ip();
			
		}
		//		
		
		try {
			int hours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			if(lastHours != hours) {
				lastHours = hours;
				locations[hours] = new String[Groups.player.size()];
				for (int i = 0; i < Groups.player.size(); i++) {
					locations[hours][i] = Groups.player.index(i).locale();
				}
			}
		} catch (Exception e) {
			
		}
	}
	
	public void save() {
//		String json = JsonIO.write(this);
		
		StringBuilder json = new StringBuilder();
		
		json.append("{\n");
		createJsonValue(json, "dayMaxOnlineCount", dayMaxOnlineCount);
		createJsonValue(json, "onlineCount", onlineCount);
		createJsonValue(json, "waveNumber", waveNumber);
		createJsonValue(json, "messagesCount", messagesCount);
		createJsonValue(json, "mapName", mapName);
		createJsonValue(json, "locations", locations);
//		createJsonValue(json, "playerData", playerData);
		
		json.append('}');
		
		Fi fi = new Fi(getPathToFile(FILENAME));
		fi.writeString(json.toString());
		

		StringBuilder adminsList = new StringBuilder("Admins list (IP/ID/NAME):");
		
		if(Vars.netServer != null) {
	        Seq<PlayerInfo> admins = Vars.netServer.admins.getAdmins();
	        if(admins != null) {
	        	if(admins.size == 0){
	        	}else{
	        		for (int i = 0; i < admins.size; i++) {
	        			PlayerInfo info = admins.get(i);
	        			adminsList.append("\n" + info.lastIP + " " + info.id + " " + info.plainLastName());
					}
	        	}
	        }
		}
		Fi fia = new Fi(getPathToFile(FILENAME_ADMINS));
		fia.writeString(adminsList.toString());
	}

	private void createJsonValue(StringBuilder json, String name, String[][] values) {
		json.append("\t\"");
		json.append(name);
		json.append("\": [");
		for (int i = 0; i < values.length; i++) {
			if(values[i] == null) {
				json.append("null");
			} else {
				json.append('[');
				for (int j = 0; j < values[i].length; j++) {
					if(values[i][j] == null) {
						json.append("null");
					} else {
						json.append('"');
						json.append(values[i][j]);
						json.append('"');
					}
					if(j != values.length - 1) {
						json.append(", ");
					}
				}
				json.append(']');
			}
			
			if(i != values.length - 1) {
				json.append(", ");
			}
		}
		json.append("]\n");
	}
	
	private void createJsonValue(StringBuilder json, String name, String value) {
		json.append("\t\"");
		json.append(name);
		json.append("\": \"");
		json.append(value);
		json.append("\",\n");
	}
	
	private void createJsonValue(StringBuilder json, String name, int value) {
		json.append("\t\"");
		json.append(name);
		json.append("\": ");
		json.append(value);
		json.append(",\n");
	}
	
	public static String getPathToFile(String name) {
		return Vars.saveDirectory + "/" + name;
	}
	
	public long getSleepTime() {
		return sleepTime;
	}
	
	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
		if(sleepTime < 10);
	}
	
	private int getDayOfWeek() {
		return Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
	}
	
	public void printData() {
		Log.info("onlineCount: @", onlineCount);
		Log.info("dayMaxOnlineCount: @", dayMaxOnlineCount);
		Log.info("mapName: @", mapName);
		Log.info("waveNumber: @", waveNumber);
		Log.info("messagesCount: @", messagesCount);
		Log.info("(transient) day Of week: @", getDayOfWeek());
	}
}
