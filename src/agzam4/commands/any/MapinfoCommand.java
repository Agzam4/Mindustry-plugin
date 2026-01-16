package agzam4.commands.any;

import static agzam4.Emoji.liquidsEmoji;
import static agzam4.Emoji.oreBlocksEmoji;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import agzam4.utils.Log;
import arc.graphics.Color;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.gen.Player;
import mindustry.type.Item;
import mindustry.type.Liquid;

public class MapinfoCommand extends CommandHandler<Object> {

	{
		desc = "Показывает статистику ресурсов карты";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		final Item itemDrops[] = new Item[] {
				Items.copper,
				Items.lead,
				Items.scrap,
				Items.sand,
				Items.coal,
				Items.titanium,
				Items.thorium
		};
		final Liquid liquidDrops[] = new Liquid[] {
				Liquids.water,
				Liquids.oil,
				Liquids.slag,
				Liquids.cryofluid
		};
		int counter[] = new int[itemDrops.length];
		int lcounter[] = new int[liquidDrops.length];
		int summaryCounter = 0;
		int typesCounter = 0;
		for(int x = 0; x < Vars.world.width(); x++){
			for(int y = 0; y < Vars.world.height(); y++) {
				if(Vars.world.tile(x, y).block() != Blocks.air) continue;
				Item floor = Vars.world.tile(x, y).floor().itemDrop;
				Item overlay = Vars.world.tile(x, y).overlay().itemDrop;
				Liquid lfloor = Vars.world.tile(x, y).floor().liquidDrop;
				Liquid loverlay = Vars.world.tile(x, y).overlay().liquidDrop;
				for (int i = 0; i < counter.length; i++) {
					if(itemDrops[i] == overlay || itemDrops[i] == floor) {
						if(counter[i] == 0) {
							typesCounter++;
						}
						counter[i]++;
						summaryCounter++;
					}
				}
				for (int i = 0; i < liquidDrops.length; i++) {
					if(liquidDrops[i] == loverlay || liquidDrops[i] == lfloor) {
						lcounter[i]++;
					}
				}
			}
		}
		StringBuilder worldInfo = new StringBuilder();
		if(summaryCounter == 0) return;
		worldInfo.append("Информация о карте:\n");
		worldInfo.append("[gold]Название: [lightgray]" + Vars.state.map.name() + "\n");
		worldInfo.append("[gold]Автор: [lightgray]" + Vars.state.map.author() + "\n");
		if(receiver instanceof Player player && Admins.has(player, "mapinfo")) worldInfo.append("[gold]Файл: [lightgray]" + Vars.state.map.file.name() + "\n");
		worldInfo.append("[gold]Рекорд: [lightgray]" + Vars.state.map.getHightScore() + "\n");
		worldInfo.append("[white]Ресурсы:\n");
		for (int i = 0; i < counter.length; i++) {
			float cv = ((float)counter[i])*typesCounter/summaryCounter/3f;
			if(cv > 1/3f) cv = 1/3f;
			Log.info("cv: @", cv);
			int percent = (int) Math.ceil(counter[i]*100d/summaryCounter);
			Color c = Color.HSVtoRGB(cv*360f, 80, 100);
			worldInfo.append(oreBlocksEmoji[i]);
			worldInfo.append('[');
			worldInfo.append('#');
			worldInfo.append(c.toString());
			worldInfo.append("] ");
			if(counter[i] > 0) {
				worldInfo.append(counter[i]);
				worldInfo.append(" (");
				worldInfo.append(percent);
				worldInfo.append("%)");
			} else {
				worldInfo.append("-");
			}
			worldInfo.append("\n[white]");
		}

		worldInfo.append("Жидкости:");
		boolean isLFound = false;
		for (int i = 0; i < lcounter.length; i++) {
			if(lcounter[i] > 0) {
				worldInfo.append("\n[white]");
				worldInfo.append(liquidsEmoji[i]);
				worldInfo.append("[lightgray]: ");
				worldInfo.append(counter[i]);
				isLFound = true;
			}
		}
		if(!isLFound) {
			worldInfo.append(" [red]нет");
		}
		sender.sendMessage(worldInfo.toString());
	}

}
