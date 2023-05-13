package example;

import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.type.UnitType;

public class Work {

	public static boolean isPlayerUnit(UnitType unit) {
		return unit == UnitTypes.alpha || unit == UnitTypes.beta || unit == UnitTypes.gamma
				|| unit == UnitTypes.evoke || unit == UnitTypes.incite || unit == UnitTypes.emanate;
	}
	

	public static void localisateItemsNames() {
		if(Items.copper.localizedName.equalsIgnoreCase(Items.copper.name)) {
	    	Items.beryllium.localizedName = "бериллий";
	    	Items.blastCompound.localizedName = "взрывчатка";
	    	Items.carbide.localizedName = "карбид";
	    	Items.coal.localizedName = "уголь";
	    	Items.copper.localizedName = "медь";
	    	Items.dormantCyst.localizedName = "оболчка";
	    	Items.fissileMatter.localizedName = "материя";
	    	Items.graphite.localizedName = "грфит";
	    	Items.lead.localizedName = "свинец";
	    	Items.metaglass.localizedName = "стекло";
	    	Items.oxide.localizedName = "оксид";
	    	Items.phaseFabric.localizedName = "фаза";
	    	Items.plastanium.localizedName = "пластан";
	    	Items.pyratite.localizedName = "пиротит";
	    	Items.sand.localizedName = "песок";
	    	Items.scrap.localizedName = "железо";
	    	Items.silicon.localizedName = "кремний";
	    	Items.sporePod.localizedName = "споры";
	    	Items.surgeAlloy.localizedName = "кинетик";
	    	Items.thorium.localizedName = "торий";
	    	Items.titanium.localizedName = "титан";
	    	Items.tungsten.localizedName = "вольфрам";
	    	Items.fissileMatter.localizedName = "материя";
	    	Items.dormantCyst.localizedName = "обломки";
		}
	}
}


