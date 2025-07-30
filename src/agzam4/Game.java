package agzam4;

import static mindustry.Vars.*;
import static mindustry.content.Blocks.*;
import static mindustry.content.UnitTypes.*;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.struct.ObjectMap;
import arc.util.I18NBundle;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Reflect;
import arc.util.Strings;
import arc.util.io.PropertiesUtils;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.ctype.MappableContent;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BulletType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Posc;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.Attribute;

public class Game {

	private static I18NBundle bundle;
	
	public static void init() {
		bundle = I18NBundle.createEmptyBundle();
        try {
    		ObjectMap<String, String> properties = new ObjectMap<>();
			PropertiesUtils.load(properties, new InputStreamReader(Game.class.getResourceAsStream("/bundles/bundle.properties"), "UTF-8"));
			bundle.setProperties(properties);
        } catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

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
		return "[#" + item.color.toString() + "]" + contentName(item);
	}

	public static @Nullable Team getTeamByName(String name) {
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

	public static int classGround 		= 1;
	public static int classGroundGreen 	= 2;
	public static int classSpider 		= 3;
	public static int classAir 			= 4;
	public static int classAirGreen 	= 5;
	public static int classNaval 		= 6;
	public static int classNavalGreen 	= 7;
	public static int classCore 		= 8;
	public static int classTank 		= 9;
	public static int classCrab 		= 10;
	public static int classAirErekir 	= 11;
	public static int clasHug			= 12;
	public static int classCoreErekir 	= 13;
			
	public static final UnitType[][] unitTiers = {
			{dagger,mace,fortress,scepter,reign},
			{nova,pulsar,quasar,vela,corvus},
			{crawler,atrax,spiroct,arkyid,toxopid},
			{flare,horizon,zenith,antumbra,eclipse},
			{mono,poly,mega,quad,oct},
			
			{risso,minke,bryde,sei,omura},
			{retusa,oxynoe,oxynoe,aegires,navanax},
			{alpha,beta,gamma},
			{stell,locus,precept,vanquish,conquer},
			{merui,cleroi,anthicus,tecta,collaris},
			
			{elude,avert,obviate,quell,disrupt},
			{renale,latum},
			{evoke,incite,emanate},
	};

	/**
	 * Get unit tier (1 - start tier, 0 - not found)
	 * @param unit - type of unit
	 * @return
	 */
	public static int getUnitTier(UnitType unit) {
		for (int type = 0; type < unitTiers.length; type++) {
			for (int tier = 0; tier < unitTiers[type].length; tier++) {
				if(unitTiers[type][tier] == unit) return tier+1;
			}
		}
		return 0;
	}
	

	public static int getUnitClass(UnitType unit) {
		for (int type = 0; type < unitTiers.length; type++) {
			for (int tier = 0; tier < unitTiers[type].length; tier++) {
				if(unitTiers[type][tier] == unit) return type+1;
			}
		}
		return 0;
	}

	/**
	 * Creating explosion at (x,y) for any type of units (ground and air)
	 * @param team - owner of explosion (wasn't damaged)
	 * @param x - x position of explosion in world units
	 * @param y - x position of explosion in world units
	 * @param radius - radius of explosion in world units
	 * @param damage - amount of damage
	 */
	public static void explosion(Team team, Posc pos, float radius, float damage) {
		explosion(team, pos.x(), pos.y(), radius, damage, true, true, false);
	}
	
	/**
	 * Creating explosion at (x,y) for any type of units (ground and air)
	 * @param team - owner of explosion (wasn't damaged)
	 * @param x - x position of explosion in world units
	 * @param y - x position of explosion in world units
	 * @param radius - radius of explosion in world units
	 * @param damage - amount of damage
	 */
	public static void explosion(Team team, float x, float y, float radius, float damage) {
		explosion(team, x, y, radius, damage, true, true, false);
	}
	
	/**
	 * Creating explosion at (x,y) for any type of units (ground and air)
	 * @param team - owner of explosion (wasn't damaged)
	 * @param x - x position of explosion in world units
	 * @param y - x position of explosion in world units
	 * @param radius - radius of explosion in world units
	 * @param damage - amount of damage
	 * @param pierce - is damage piercing
	 */
	public static void explosion(Team team, float x, float y, float radius, float damage, boolean pierce) {
		explosion(team, x, y, radius, damage, true, true, pierce);
	}
	
	/**
	 * Creating explosion at (x,y)
	 * @param team - owner of explosion (wasn't damaged)
	 * @param x - x position of explosion in world units
	 * @param y - x position of explosion in world units
	 * @param radius - radius of explosion in world units
	 * @param damage - amount of damage
	 * @param ground - is ground units was damaged
	 * @param air - is air units was damaged
	 * @param pierce - is damage piercing
	 */
	public static void explosion(Team team, float x, float y, float radius, float damage, boolean ground, boolean air, boolean pierce) {
		Call.logicExplosion(team, x, y, radius, damage, air, ground, pierce, false);	
	}

	public static Block getWateredFloor(Floor floor) {
		if(floor == sand || floor == sandWater) return sandWater;
		if(floor == darksand || floor == darksandWater) return darksandWater;
		if(floor == moss || floor == sporeMoss || floor == taintedWater) return taintedWater;
		return water;
	}

	public static void effectPositioned(Effect effect, float x, float y, float size, Color color, Position position) {
		Call.effect(effect, x, y, size, color, position);
	}

	public static void effect(Effect effect, Position position, float size, Color color) {
		Call.effect(effect, position.getX(), position.getY(), size, color);
	}
	
	public static void effect(Effect effect, float x, float y, float size, Color color) {
		Call.effect(effect, x, y, size, color);
	}

	public static @Nullable Player findPlayer(final String s) {
		Player found = Groups.player.find(p -> p.uuid().equals(s));
        if(found != null) return found;
        String str = strip(s);
        found = Groups.player.find(p -> p.name.equals(s));	
        if(found != null) return found;
        found = Groups.player.find(p -> strip(p.name).equals(str));	
        if(found != null) return found;
        return Groups.player.find(p -> strip(p.name).replaceAll(" ", "_").equals(str));		
	}

	public static @Nullable Block findBlock(String name) {
		Block b = Vars.content.block(name);
		if(b != null) return b;
		b = Vars.content.blocks().find(s -> s.hasEmoji() && s.emoji().equals(name));
		if(b != null) return b;
		try {
			return Reflect.get(Blocks.class, null, name);
		} catch (Exception | Error e) {
			return null;
		}
	}

	public static String strip(String s) {
		return Strings.stripColors(Strings.stripGlyphs(s));
	}

	public static float getAttribute(int x, int y, Attribute att) {
		Tile t = Vars.world.tile(x, y);
		if(t == null) return 0;
		return t.floor().attributes.get(att);
	}

	public static String round(float f) {
		return Strings.autoFixed(f, 2);
	}

	public static String bungle(String string) {
		return bundle.get(string, "[red]???" + string + "???[]");
	}

	public static String bungleDef(String string, String def) {
		return bundle.get(string, def);
	}

	public static String bungle(String text, Object... args) {
		return bungle(Strings.format(text, args));
	}
	
	public static String formatByFileds(String src, Object object) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < src.length(); i++) {
			char c = src.charAt(i);
			if(c == '@') {
				int j;
				for (j = i + 1; Character.isAlphabetic(src.charAt(j)) && j < src.length(); j++);
				Object value = Reflect.get(object, src.substring(i, j));
				Log.info("field: @", src.substring(i, j));
				if(value == null) result.append("null");
				else result.append(value.toString());
				continue;
			}
			result.append(c);
		}
		return result.toString();
	}

	public static Object formatByMap(String src, ObjectMap<String, Object> props) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < src.length(); i++) {
			char c = src.charAt(i);
			if(c == '@') {
				int j;
				for (j = i + 1; Character.isAlphabetic(src.charAt(j)) && j < src.length(); j++);
				Object value = props.get(src.substring(i+1, j));
				if(value == null) result.append("null");
				else result.append(value.toString());
				i = j-1;
				if(src.charAt(j) == '@') i++;
				continue;
			}
			result.append(c);
		}
		return result.toString();
	}

	public static String formatContent(String src) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < src.length(); i++) {
			char c = src.charAt(i);
			if(c == '@') {
				int j;
				for (j = i + 1; Character.isAlphabetic(src.charAt(j)) && j < src.length(); j++);
				Object value = bundle.get("global." + src.substring(i+1, j));
				if(value == null) result.append("[red]???" + src.substring(i+1, j) + "???[]");
				else result.append(value.toString());
				i = j-1;
				if(src.charAt(j) == '@') i++;
				continue;
			}
			result.append(c);
		}
		return result.toString();
	}
	
	public static String contentName(MappableContent content) {
		return bundle.get("content." + content.name, "[red]???" + content.name + "???[]");
	}

	public static void sync() {
		Call.worldDataBegin();
		Groups.player.each(p -> Vars.netServer.sendWorldData(p));
	}

	public static void clearUnit(Player player) {
		UnitType type = UnitTypes.alpha;
		Position pos = player;
		var core = player.bestCore();
		if(core != null) {
			pos = core;
			if(core.block instanceof CoreBlock block) {
				type = block.unitType;
			}
		}
		var u = type.spawn(player.team(), pos);
		u.spawnedByCore = true;
		u.add();
		player.unit(u);
	}

}
