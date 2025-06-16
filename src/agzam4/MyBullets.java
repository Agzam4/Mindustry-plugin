package agzam4;

import arc.util.Nullable;
import mindustry.entities.bullet.BulletType;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.ContinuousLiquidTurret;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.LiquidTurret;
import mindustry.world.blocks.defense.turrets.PowerTurret;

public class MyBullets {

	/**
	 * @param itemTurret - block of item turret
	 * @param item - type of ammo
	 * @return null if block not item turret and turret bullet type if found
	 */
	public static @Nullable BulletType getItemBullet(Block itemTurret, Item item) {
		if(!(itemTurret instanceof ItemTurret turret)) return null;
		return turret.ammoTypes.get(item);
	}
	
	/**
	 * @param turret - block of liquid turret
	 * @param liquid - type of ammo
	 * @return null if block not item turret and turret bullet type if found
	 */
	public static @Nullable BulletType getLiquidBullet(Block turret, Liquid liquid) {
		if(turret instanceof LiquidTurret t) return t.ammoTypes.get(liquid);
		if(turret instanceof ContinuousLiquidTurret t) return t.ammoTypes.get(liquid);
		return null;
	}

	/**
	 * @param turret - block of liquid turret
	 * @return null if block not item turret and turret bullet type if found
	 */
	public static BulletType getLaserBullet(Block turret) {
		if(!(turret instanceof PowerTurret t)) return null;
		return t.shootType;
	}
	
	public static BulletType getUnitBullet(UnitType type, int i) {
		return type.weapons.get(i).bullet;
	}

}
