package agzam4.bans;

import agzam4.moderation.Kicks;
import arc.files.Fi;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class BanGroup {

	public FileIpList list;
	
	public BanGroup(Fi fi) {
		list = new FileIpList(fi, true);
	}

	public void load() {
		list.load();
	}
	
	public void banBy(Player kicker, String ip, String reason) {
		if(ip == null) return;
		// Kick all players with banned IP
		Groups.player.each(p -> {
			if(ip.equals(p.ip())) Kicks.kick(kicker, p, reason);
		});
		list.addIp(ip);
	}

	public boolean has(String ip) {
		if(ip == null) return false;
		return list.has(ip);
	}
	
}
