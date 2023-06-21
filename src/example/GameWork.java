package example;

import static mindustry.Vars.*;
import static mindustry.content.Blocks.*;

import java.util.concurrent.TimeUnit;

import arc.graphics.Color;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;

public class GameWork {

	public static final Block[] turrets = {duo, scatter, scorch, hail, arc, wave, lancer, swarmer, salvo, fuse, ripple, cyclone,
			foreshadow, spectre, meltdown, segment, parallax, tsunami
	};

	public static Team defaultTeam() {
		return state.rules.defaultTeam;
	}

	public static void changeBuildingTeam(Building building, Team team) {
		building.tile.setNet(building.block, Team.crux, building.rotation);
	}

	public static void replaceBuilding(Building building, Block newBlock) {
		building.tile.setNet(newBlock, building.team, building.rotation);
	}

	public static void removeEnvBlock(int tileX, int tileY) {
		Tile tile = world.tile(tileX, tileY);
		if(tile == null) return;
		if(tile.build != null) return;
		tile.setNet(Blocks.air);
	}

	public static String colorToHex(Color color) {
    	return String.format("#%02x%02x%02x", (int)(color.r*255), (int)(color.g*255), (int)(color.b*255));
	}
	
	public static String getColoredLocalizedItemName(Item item) {
		return "[#" + item.color.toString() + "]" + item.localizedName;
	}
	
	
	private static long warningToastLastTime = System.nanoTime();
	private static String warningToastLast = "";
	
	public static void warningToast(String text) {
		if(text.equals(warningToastLast)) return;
		long time = System.nanoTime();
		if(time - warningToastLastTime < TimeUnit.NANOSECONDS.toSeconds(5)) return;
		Call.warningToast(0, text);
		warningToastLast = text;
		warningToastLastTime = time;
	}
	
}
