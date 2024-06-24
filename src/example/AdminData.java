package example;

import java.util.Iterator;

import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.util.Strings;
import mindustry.content.Blocks;
import mindustry.net.Administration.PlayerInfo;

public class AdminData {

	public static AdminData from(PlayerInfo info) {
		return new AdminData();
	}
	
	private ObjectSet<String> permissions;
	
	private AdminData() {
		permissions = new ObjectSet<String>();
	}

	public boolean has(String key) {
		return permissions.contains(key);
	}

	public boolean add(String key) {
		return permissions.add(key);
	}

	public boolean remove(String key) {
		return permissions.remove(key);
	}
	
	public String permissionsAsString(char separator) {
		StringBuilder line = new StringBuilder();
		boolean first = true;
		for (Iterator<String> iterator = permissions.iterator(); iterator.hasNext();) {
			String permission = iterator.next();
			if(!first) line.append(separator);
			first = false;
			line.append(permission);
		}
		return line.toString();
	}

	public int permissionsCount() {
		return permissions.size;
	}

}
