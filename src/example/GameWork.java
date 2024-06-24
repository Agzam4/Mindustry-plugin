package example;

import static mindustry.Vars.*;
import static mindustry.content.Blocks.*;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.game.Team;
import mindustry.game.Teams.TeamData;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public class GameWork {

	public static final Block[] serpyloTurrets = {duo, scatter, scorch, hail, arc, wave, lancer, swarmer, salvo, fuse, ripple, cyclone,
			foreshadow, spectre, meltdown, segment, parallax, tsunami
	};

	public static final Block[] erekirTurrets = {breach, diffuse, sublimate, titan, disperse, afflict, lustre, scathe, smite, malign};

	public static final int bulletAfflictId = getPowerTurretBulletId(Blocks.afflict, 165);
	public static final int bulletSmiteId = geItemTurretBulletId(Blocks.smite, Items.surgeAlloy, 171);

	private static int getPowerTurretBulletId(Block block, int def) {
		if(block instanceof PowerTurret) return ((PowerTurret) block).shootType.id;
		return def;
	}

	private static int geItemTurretBulletId(Block block, Item item, int def) {
		if(block instanceof ItemTurret) {
			BulletType type = ((ItemTurret) block).ammoTypes.get(item);
			if(type == null) return def;
			return type.id;
		}
		return def;
	}
	
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

	public static Team getTeamByName(String name) {
		for (int i = 0; i < Team.baseTeams.length; i++) {
			if(Team.baseTeams[i].name.equalsIgnoreCase(name)) return Team.baseTeams[i];
		}
		return null;
	}

	public static boolean supportsEnv(Block block) {
		return block.supportsEnv(Vars.state.rules.env);
	}
	
	public static boolean supportsEnv(UnitType type) {
		return type.supportsEnv(Vars.state.rules.env);
	}

	public static Block getBlockByEmoji(String emoji) {
		for (int i = 0; i < Vars.content.blocks().size; i++) {
			Block block = Vars.content.blocks().get(i);
			if(block.hasEmoji()) {
				if(emoji.equals(block.emoji())) return block;
			}
		}
		return null;
	}
	
	public static int roundMax(float value) {
		if(value > 0) return Mathf.ceil(value);
		return Mathf.floor(value);
	}

}
