package agzam4.commands.admin;

import agzam4.Game;
import agzam4.CommandsManager.ResultSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import arc.Events;
import arc.math.geom.Bresenham2;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.World;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Player;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OverlayFloor;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.BuildVisibility;

public class BrushCommand extends CommandHandler<Player> {
	
	{
		parms = "[none/block/floor/overlay/info] [block/none]";
		desc = "Устанваливает кисточку";
	}
	
	static {
		Events.on(TapEvent.class, event -> {
			Player player = event.player;
			if(player == null) return;
			if(event.tile == null) return;
			if(!player.admin()) return;
			Brush brush = Brush.get(player);
			Vars.mods.getScripts().runConsole("var ttile = Vars.world.tile(" + event.tile.pos() + ")");
			if(brush.info && event.tile.build != null) player.sendMessage("[white]lastAccessed: [gold]" + event.tile.build.lastAccessed);
		});
    	Events.run(Trigger.update, () -> Brush.brushes.each(Brush::update));
		Events.on(GameOverEvent.class, e -> Brush.brushes.clear());
		Events.on(PlayerLeave.class, e -> Brush.brushes.removeAll(b -> b.owner == e.player));
	}
	
	private static class Brush {

		private static Seq<Brush> brushes = Seq.with();

		private final Player owner;
		private Block block, floor, overlay;
		private int brushLastX = -1, brushLastY = -1;
		public boolean info = false;
		
		public Brush(Player owner) {
			this.owner = owner;
		}
		
		private void update() {
    		if(owner == null) return;
    		if(!owner.shooting()) {
    			brushLastY = brushLastX = -1;
    			return;
    		}
    		
    		if(block == null && floor == null && overlay == null) return;
    		
    		int tileX = World.toTile(owner.mouseX());
    		int tileY = World.toTile(owner.mouseY());
    		if(brushLastX == tileX && brushLastY == tileY) return;

    		if(brushLastX == -1 || brushLastY == -1) {
        		brushLastX = tileX;
        		brushLastY = tileY;
        		if(brushLastX == -1 || brushLastY == -1) return;
        		paintTile(Vars.world.tile(tileX, tileY), false);
    			return;
    		}
    		
    		int x0 = brushLastX;
    		int y0 = brushLastY;
    		int x1 = tileX;
    		int y1 = tileY;
    		
    		brushLastX = tileX;
    		brushLastY = tileY;
    		
    		if(x0 == -1 || y0 == -1 || x1 == -1 || y1 == -1) return;
    		
    		Bresenham2.line(x0, y0, x1, y1, (x,y) -> paintTile(Vars.world.tile(x, y), x != x0 || y != y0));
		}

		public static Brush get(Player player) {
			var brush = brushes.find(b -> b.owner == player);
			if(brush == null) {
				brush = new Brush(player);
				brushes.add(brush);
			}
			return brush;
		}

		private void paintTile(@Nullable Tile tile, boolean saveCores) {
			if(tile == null) return;
			if(block != null) {
				if(!(saveCores && tile.block() instanceof CoreBlock)) {
					tile.setNet(block, owner.team(), 0);
				}
			}
			if(floor != null) {
				if(overlay != null) {
					tile.setFloorNet(floor, overlay);
				} else {
					tile.setFloorNet(floor, tile.overlay());
				}
			} else {
				if(overlay != null) {
					tile.setFloorNet(tile.floor(), overlay);
				}
			}		
		}
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Player player, ReceiverType type) {
		Brush brush = Brush.get(player);
		if(args.length == 0) {
			sender.sendMessage(Strings.format("Кисточка: [@,@,@]", brush.floor, brush.overlay, brush.block));
		} else if(args.length == 1) {
			if(args[0].equalsIgnoreCase("none")) {
				brush.block = null;
				brush.floor = null;
				brush.overlay = null;
				brush.info = false;
				sender.sendMessage("[gold]Кисть отчищена");
			} else if(args[0].equalsIgnoreCase("block")) {
				if(brush.block == null) sender.sendMessage("[gold]К кисти не привязан блок");
				else sender.sendMessage("[gold]К кисти привязан блок: " + brush.block.emoji() + " [lightgray]" + brush.block);
			} else if(args[0].equalsIgnoreCase("floor")) {
				if(brush.floor == null) sender.sendMessage("[gold]К кисти не привязана поверхность");
				else sender.sendMessage("[gold]К кисти привязан блок: " + brush.floor.emoji() + " [lightgray]" + brush.floor);
			} else if(args[0].equalsIgnoreCase("overlay")) {
				if(brush.overlay == null) sender.sendMessage("[gold]К кисти не привязано покрытие");
				else sender.sendMessage("[gold]К кисти привязан блок: " + brush.overlay.emoji() + " [lightgray]" + brush.overlay);
			} else if(args[0].equalsIgnoreCase("info")) {
				brush.info  = true;
				sender.sendMessage("[gold]готово!");
			} else {
				sender.sendMessage("[red]На первом месте только аргумены [lightgray]none/block/floor/overlay");
			}
		} else if(args.length == 2) {
			String blockname = args[1];
			if(blockname.equals("core1")) blockname = "coreShard";
			if(blockname.equals("core2")) blockname = "coreFoundation";
			if(blockname.equals("core3")) blockname = "coreNucleus";
			if(blockname.equals("core4")) blockname = "coreBastion";
			if(blockname.equals("core5")) blockname = "coreCitadel";
			if(blockname.equals("core6")) blockname = "coreAcropolis";
			if(blockname.equals("power+")) blockname = "powerSource";
			if(blockname.equals("power-")) blockname = "powerVoid";
			if(blockname.equals("item+")) blockname = "itemSource";
			if(blockname.equals("item-")) blockname = "itemVoid";
			if(blockname.equals("liq+")) blockname = "liquidSource";
			if(blockname.equals("liq-")) blockname = "liquidVoid";
			if(blockname.equals("s")) blockname = "shieldProjector";
			if(blockname.equals("ls")) blockname = "largeShieldProjector";
			
			if(args[0].equalsIgnoreCase("block") || args[0].equalsIgnoreCase("b")) {
				if(args[1].equals("none")) {
					brush.block = null;
					sender.sendMessage("[gold]Блок отвязан");
					return;
				}
				@Nullable Block find = Game.findBlock(blockname);

				if(require(find == null, sender, "[red]Блок не найден")) return;
				if(require(!allowedBlock(find, player), sender, "[red]Блок недоступен в текущем режиме игры")) return;
				brush.block = find;
				sender.sendMessage("[gold]Поверхность привязана!");
			} else if(args[0].equalsIgnoreCase("floor") || args[0].equalsIgnoreCase("f")) {
				if(args[1].equals("none")) {
					brush.floor = null;
					sender.sendMessage("[gold]Поверхность отвязана");
					return;
				}
				@Nullable Block find = Game.findBlock(blockname);
				if(require(find == null, sender, "[red]Поверхность не найдена")) return;
				if(require(!(find instanceof Floor), sender, "[red]Это не поверхность")) return;
				brush.floor = find;
				sender.sendMessage("[gold]Поверхность привязана!");
			} else if(args[0].equalsIgnoreCase("overlay") || args[0].equalsIgnoreCase("o")) {
				if(args[1].equals("none")) {
					brush.overlay = null;
					sender.sendMessage("[gold]Покрытие отвязано");
					return;
				}
				@Nullable Block find = Game.findBlock(blockname);
				if(require(find == null, sender, "[red]Поверхность не найдена")) return;
				if(require(!(find instanceof OverlayFloor) && find != Blocks.air, sender, "[red]Это не поверхность")) return;
				brush.overlay = find;
				sender.sendMessage("[gold]Поверхность привязана!");
			}
		}
	}
	
	private boolean allowedBlock(Block find, Player player) {
		return (find.canBeBuilt() || find.buildVisibility == BuildVisibility.hidden) || Admins.has(player, "brush-sandbox");
	}

	@Override
	public Seq<?> complete(String[] args, Player receiver, ReceiverType type) {
		if(args.length == 0) return Seq.with("none очистить все слои", "block", "floor", "overlay", "info");
		Seq<String> seq = Seq.with("air", "none");
		if(args[0].equalsIgnoreCase("b") || args[0].equalsIgnoreCase("block")) {
			seq.addAll(Vars.content.blocks().select(b -> allowedBlock(b, receiver)).map(b -> b.name + " " + b.emoji()));
			return seq;
		}
		if(args[0].equalsIgnoreCase("f") || args[0].equalsIgnoreCase("floor")) {
			seq.addAll(Vars.content.blocks().select(b -> b instanceof Floor).map(b -> b.name + " " + b.emoji()));
			return seq;
		}
		if(args[0].equalsIgnoreCase("o") || args[0].equalsIgnoreCase("overlay")) {
			seq.addAll(Vars.content.blocks().select(b -> b instanceof OverlayFloor).map(b -> b.name + " " + b.emoji()));
			return seq;
		}
		return super.complete(args, receiver, type);
	}
	
}
