package agzam4.commands.admin;

import agzam4.CommandsManager.ResultSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.UnitTypes;
import mindustry.gen.Player;
import mindustry.gen.Unit;

public class UnitCommand extends CommandHandler<Player> {

	{
		parms = "[type] [t/c]";
		desc = "Создает юнита, list для списка";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Player player, ReceiverType type) {
		if(args.length > 0) {
			if(args[0].equals("list")) {
				StringBuilder unitTypes = new StringBuilder();
				Vars.content.units().each(ut -> {
					if(ut == UnitTypes.block) return;
					if(unitTypes.length() != 0) unitTypes.append(", ");
					unitTypes.append(ut.name);
				});
				sender.sendMessage(unitTypes.toString());
				return;
			}
		}
		var ut = Vars.content.units().find(t -> t.name.equals(args[0]) && t != UnitTypes.block);
		
		if(require(ut == null, sender, "[red]Юнит не найден [gold]/unit list")) return;
		Unit u = ut.spawn(player.team(), player.mouseX, player.mouseY);
		if(args.length > 1) {
			if(args[1].equals("true") || args[1].equals("y") || args[1].equals("t") || args[1].equals("yes")) {
				player.unit(u);
			}
			if(args[1].equals("c") || args[1].equals("core")) {
				player.unit(u);
				u.spawnedByCore(true);
			}
		}
		u.add();
		sender.sendMessage("Готово!"); 
	}
	
	@Override
	public Seq<?> complete(String[] args, Player receiver, ReceiverType type) {
		if(args.length == 0) return Vars.content.units().select(u -> u != UnitTypes.block).map(u -> u.name);
		if(args.length == 1) return Seq.with("c", "t");
		return super.complete(args, receiver, type);
	}
}
