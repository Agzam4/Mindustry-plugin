package agzam4.bans;

import agzam4proc.lib.PVars;
import arc.files.Fi;

public class Bans {

	public static final Fi dir = PVars.dataDirectory.child("iplists");
	
	public static BanGroup bots = new BanGroup(dir.child("bots.txt"));
	
	public static void init() {
		dir.mkdirs();
		bots.load();
	}
	
}
