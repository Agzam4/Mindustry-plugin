package agzam4;

import arc.files.Fi;
import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.io.JsonIO;

public class Players {

	private static Players instance;
	private ObjectMap<String, PlayerData> players;
	private static Fi save;
	
	public Players() {
		players = new ObjectMap<>();
	}
	
	public static void init() {
		save = new Fi(Vars.saveDirectory + "/players_data.txt", Vars.saveDirectory.type());
//		Vars.netServer.admins.getAdmins().forEach(i -> admins.put(i, AdminData.from(i)));
		load();
		if(instance == null) instance = new Players();
	}

	public static void load() {
		if(!save.exists()) return;
		instance = JsonIO.read(Players.class, save.readString());
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
