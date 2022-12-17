package example;

import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import arc.Core;
import arc.Files;
import arc.files.Fi;
import arc.util.Log;
import arc.util.serialization.Json;
import arc.util.serialization.JsonWriter;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.core.Logic;
import mindustry.game.SectorInfo;
import mindustry.game.Waves;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.io.JsonIO;

public class DataCollecter {

	public static final String FILENAME = "agzam_s_plugin_statistics.json";

	private transient long sleepTime = 5 * 60_000 / 60 / 5; // FIXME: 5 minutes

	private int dayMaxOnlineCount = 0;
	private int onlineCount = 0;
	private String mapName = "Loading...";
	private int waveNumber = 0;
	private int messagesCount = 0;
	
	private String[][] locations = new String[24][];
	
	public void init() {
		
	}
	
	public void messageEvent(Player player, String text) {
		messagesCount++;
	}

	private transient boolean isCollecting;
	private transient int day;
	
	public void collect() {
		isCollecting = true;
		new Thread(() -> {
			while (isCollecting) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				collecData();
				save();
			}
		}).start();
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
		json.append('}');
		
		Fi fi = new Fi(getPathToFile());
		fi.writeString(json.toString());
//		Files.writeString(json);//(Vars.saveDirectory, json);
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
	
	public static String getPathToFile() {
		return Vars.saveDirectory + "/" + FILENAME;
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
