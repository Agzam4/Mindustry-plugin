package agzam4.mcp.tools;

import agzam4.Game;
import agzam4.api.auth.SensitiveData;
import agzam4.api.auth.SensitiveData.SensitiveType;
import agzam4.database.Databases;
import agzam4.utils.Strs;
import agzam4proc.apt.mcp.McpAnnotations.McpTool;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Groups;

public class InfoMcpTools {

	/**
	 * Show list of current server players
	 */
	@McpTool
	public static String players() {
		StringBuilder response = new StringBuilder();
		int[] lines = new int[1];
		Groups.player.each(p -> {
			if(lines[0] != 0) response.append("\n");
			response.append(Strings.format("@. Id: @, name: @, times joined: @, times kicked: @", 
					++lines[0],
					SensitiveData.insertOrGet(p.uuid(), SensitiveType.uuid),
					Strs.escape(Game.nameByUuid(p.uuid())),
					p.admin ? "(admin)" : "", 
							p.getInfo().timesJoined, 
							p.getInfo().timesKicked)
					);
		});
		return response.toString();
	}
	

	/**
	 * Show info about player by id
	 * @param id - id of player
	 */
	@McpTool
	public static String trace(int id) {
		StringBuilder response = new StringBuilder();
		int[] lines = new int[1];
		String uuid = SensitiveData.resolve(id);
		if(uuid == null) throw new RuntimeException("Player not found");

		var info = Vars.netServer.admins.playerInfo.get(uuid);
		
		StringBuilder builder = new StringBuilder();
		builder.append(Strings.format("Last name: @\n", Strs.escape(info.lastName)));
		builder.append(Strings.format("Times joined: @\n", info.timesJoined));
		builder.append(Strings.format("Times kicked: @\n", info.timesKicked));
		builder.append(Strings.format("Playertime: @\n", Databases.player(uuid).playtime));
		builder.append("Names history:");
		for (int i = 0; i < info.names.size; i++) {
			builder.append(Strings.format("\n@. @", i+1, Strs.escape(info.names.get(i))));
		}
		
		return builder.toString();
	}

}
