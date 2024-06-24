package example;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.net.Administration.PlayerInfo;

public class Admins {

	private static ObjectMap<PlayerInfo, AdminData> admins;
	private static Fi save;
	
	public static void init() {
		save = new Fi(Vars.saveDirectory + "/admins_data.txt", Vars.saveDirectory.type());
		admins = new ObjectMap<>();
//		Vars.netServer.admins.getAdmins().forEach(i -> admins.put(i, AdminData.from(i)));
		load();
	}
	
	public static @Nullable AdminData adminData(@Nullable PlayerInfo info) {
		if(info == null) return null;
		return admins.get(info);
	}

	public static boolean has(Player player, String string) {
		if(player.admin) return true;
		var data = adminData(player.getInfo());
		if(data == null) return false;
		return data.has(string);
	}

	/**
	 * @param player - target player
	 * @return true of player was added or null if player is null or already added
	 */
	public static boolean add(@Nullable Player player) {
		if(player == null) return false;
		if(admins.containsKey(player.getInfo())) return false;
		admins.put(player.getInfo(), AdminData.from(player.getInfo()));
		return true;
	}

	/**
	 * @param player - target player
	 * @return true of player was removed or null if player is null or not found
	 */
	public static boolean remove(Player player) {
		if(player == null) return false;
		return admins.remove(player.getInfo()) != null;
	}

	public static void load() {
		if(!save.exists()) return;
		String[] data = save.readString().split("\n");
		for (int i = 0; i < data.length; i++) {
			String[] args = data[i].split(" ");
			if(args.length < 1) continue;
			var info = Vars.netServer.admins.getInfo(args[0]);
			if(info == null) continue;
			var ad = AdminData.from(info);
			for (int j = 1; j < args.length; j++) {
				ad.add(args[j]);
			}
			admins.put(info, ad);
		}
	}
	
	public static void save() {
		StringBuilder result = new StringBuilder();
		admins.each((info, data) -> {
			if(result.length() != 0) result.append('\n');
			result.append(info.id);
			result.append(' ');
			result.append(data.permissionsAsString(' '));
		});
		save.writeString(result.toString(), false);
	}
	
}
