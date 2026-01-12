package agzam4.commands.server;

import agzam4.Game;
import agzam4.CommandsManager.ResultSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Player;
import mindustry.type.Item;

public class FillitemsCommand extends CommandHandler<Object> {

	{
		parms = "[item] [count]";
		desc = "Заполните ядро предметами";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		try {
			if(args.length == 0) {
				StringBuilder names = new StringBuilder();
				Vars.content.items().each(i -> {
					if(names.length() != 0) names.append(", ");
					if(type == ReceiverType.player) names.append("[white]" + i.emoji() + " " + Game.getColoredLocalizedItemName(i));
					if(type == ReceiverType.bot) names.append("<code>" + Game.contentName(i) + "</code>");
					if(type == ReceiverType.server) names.append(Game.contentName(i));
				});
				sender.sendMessage(type.format("fillitems.names", names));
				return;
			}
			int count = args.length > 1 ? Integer.parseInt(args[1]) : 0;

			String itemname = args[0].toLowerCase();

			if(itemname.equals("all")) {
				Team team = receiver instanceof Player p ? p.team() : Vars.state.rules.defaultTeam;
				if(require(team.cores().size == 0, sender, type.err("fillitems.no-core"))) return;
				Vars.content.items().each(i -> team.cores().get(0).items.set(i, 9999999));
				return;
			}

			Item item = Vars.content.items().find(i -> itemname.equalsIgnoreCase(i.name) || itemname.equalsIgnoreCase(Game.contentName(i)));
			if(require(item == null, sender, type.err("fillitems.no-item"))) return;
			Team team = receiver instanceof Player p ? p.team() : Vars.state.rules.defaultTeam;
			if(require(team.cores().size == 0, sender, type.err("fillitems.no-core"))) return;

			team.cores().get(0).items.add(item, count);
			sender.sendMessage(type.format("fillitems.added", count, item.name));
		} catch (Exception e) {
			sender.sendMessage(e.getMessage());
		}
	}

	@Override
	public Seq<String> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length == 0) return Vars.content.items().map(i -> i.name);
		return null;
	}
	
}
