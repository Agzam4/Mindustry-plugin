package agzam4;

import arc.files.Fi;
import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.io.JsonIO;

public class PlayersData {

	private static PlayersData instance;
	private ObjectMap<String, PlayerData> players;
	private static Fi save;
	
	public PlayersData() {
		players = new ObjectMap<>();
	}
	
	public static void init() {
		save = new Fi(Vars.saveDirectory + "/players_data.txt", Vars.saveDirectory.type());
//		Vars.netServer.admins.getAdmins().forEach(i -> admins.put(i, AdminData.from(i)));
		load();
		if(instance == null) instance = new PlayersData();
	}

	public static void load() {
		if(!save.exists()) return;
		instance = JsonIO.read(PlayersData.class, save.readString());
	}

	public static void save() {
		save.writeString(JsonIO.write(instance));
	}
	
	public static PlayerData getData(String uuid) {
		return instance.players.get(uuid);
	}

	public static PlayerData data(String uuid) {
		if(!instance.players.containsKey(uuid)) instance.players.put(uuid, PlayerData.from(uuid));
		return getData(uuid);
	}
}
