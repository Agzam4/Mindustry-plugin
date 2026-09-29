package agzam4.commands.admin;

import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.UnitTypes;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.PingLocationCallPacket;
import mindustry.gen.Player;
import mindustry.gen.TargetDummyUnit;
import mindustry.gen.Unit;
import mindustry.input.InputHandler;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;

public class UnitCommand extends CommandHandler<Player> {

	{
		parms = "[type] [t/c] [core]";
		desc = "Создает юнита, list для списка";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Player player, ReceiverType type) {
		if(require(args.length < 1, sender, "Мало аргументов")) return;

		if(args[0].equals("kill")) {
			var units = Groups.unit.intersect(player.mouseX-4f, player.mouseY-4f, 8f, 8f);
			units.each(u -> {
		        if(u instanceof TargetDummyUnit td) {
		        	Call.unitDespawn(td);
		        	return;
		        }
				u.kill();
			});
			sender.sendMessage("Уничтожено: " + units.size + " юнитов");
			return;
		}
		if(args[0].equals("ride")) {
			var units = Groups.unit.intersect(player.mouseX-4f, player.mouseY-4f, 8f, 8f);
			if(require(units.size == 0, sender, "[red]Нет юнита на тайле")) return;
			player.unit(units.first());
			sender.sendMessage("Готово");
			return;
		}
		if(args[0].equals("list")) {
			StringBuilder unitTypes = new StringBuilder();
			Vars.content.units().each(ut -> {
				if(allowedUnit(ut, player)) return;
				if(unitTypes.length() != 0) unitTypes.append(", ");
				unitTypes.append(ut.name);
			});
			sender.sendMessage(unitTypes.toString());
			return;
		}
		var ut = Vars.content.units().find(t -> t.name.equals(args[0]));
		if(require(ut == null || !allowedUnit(ut, player), sender, "[red]Юнит не найден [gold]/unit list")) return;
		
		Unit u = ut.spawn(player.team(), player.mouseX, player.mouseY);
		if(args.length > 1) {
			if(args[1].equals("true") || args[1].equals("y") || args[1].equals("t") || args[1].equals("yes")) {
				player.unit(u);
			}
			if(args[1].equals("c")) {
				player.unit(u);
				u.spawnedByCore(true);
			}
		}

        u.set(player.mouseX, player.mouseY);
        u.rotation = 90f;
        if(u instanceof TargetDummyUnit td) {
        	var build = (args.length > 2 && args[2].equals("core")) || (args.length > 1 && args[1].equals("core")) ? player.core() : u.buildOn();
    		if(require(build == null, sender, "[red]Нет целевой постройки, наведи на блок или используй аргумент core")) return;
        	var p = new PingLocationCallPacket();
        	p.player = player;
        	p.x = build.x;
        	p.y = build.y;
        	p.text = "Блок-хранитель";
        	Vars.net.send(p, Seq.with(player.con), true);
    		sender.sendMessage(Strings.format("Блок-хранитель: @ [gray](@,@)", build.block.emoji(), build.tileX(), build.tileY())); 
        	td.building(build);
        }
        u.add();
        if(ut == UnitTypes.dummy) {
        	Call.unitTetherBlockSpawned(u.tileOn(), u.id);
        }
		sender.sendMessage("Готово!"); 
	}

	private boolean allowedUnit(UnitType unit, Player player) {
		if(unit == UnitTypes.dummy) return true;
		if(unit == UnitTypes.block) return false;
		if(unit.hidden && unit.internal) return false;
		return true;
	}

	@Override
	public Seq<?> complete(String[] args, Player receiver, ReceiverType type) {
		if(args.length == 0) {
			Seq<Object> seq = Seq.with();
			seq.addAll(Vars.content.units().select(u -> allowedUnit(u, receiver)));
			seq.add("kill");
			seq.add("ride");
			return seq;
		}
		if(args.length == 1) return Seq.with("c", "t", "core");
		if(args.length == 2) return Seq.with("core");
		return super.complete(args, receiver, type);
	}
}
