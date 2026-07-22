package agzam4.maps;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.serialization.Jval;
import mindustry.Vars;

public class MapsManager {

	protected static Fi dir = Vars.dataDirectory.child("user-maps");
	protected static Fi database = dir.child("database.hjson");
	
	private static ObjectMap<String, MapMaker> makers = ObjectMap.of();
		
	// List of all maps, each map can contains several authors (or not contains any)
	public static Seq<MapSlot> maps = Seq.with();
	
	private static Seq<MapSlot> bungle = Seq.with();
	
	public static void init() {
		load();
		Vars.maps.setMapProvider((mode, prev) -> {
			if(bungle.size == 0) {
				bungle.addAll(maps);
				bungle.shuffle();
			}
			while (bungle.size > 0) {
				var slot = bungle.remove(0);
				var map = slot.map();
				if(map == null) continue;
				return map;
			}
			Log.warn("Valid maps not found");
			return prev;
		});
	}
	
	
	private static void load() {
		// Loading default maps
		
		
		// Loading user maps
		try {
			if(database.exists()) {
				var data = Jval.read(database.reader());
				var defaultList = data.get("maps").asArray();
				for (int i = 0; i < defaultList.size; i++) {
					var item = defaultList.get(i);
					if(item.has("default")) {
						String name = item.getString("default");
						maps.add(new DefaultMapSlot(i, Vars.maps.defaultMaps().find(m -> m.file.nameWithoutExtension().equals(name))));
						continue;
					}
					if(item.has("custom")) {
						String name = item.getString("custom");
						maps.add(new CustomMapSlot(i, Vars.maps.defaultMaps().find(m -> m.file.nameWithoutExtension().equals(name))));
						continue;
					}
					maps.add(new UserMapSlot(i, defaultList.get(i)));
				}
			} else {
				database.parent().mkdirs();
			}
			
			// build-in maps
			Vars.maps.defaultMaps().each(m -> {
				if(maps.contains(s -> s instanceof DefaultMapSlot def && def.map == m)) return;
				maps.add(new DefaultMapSlot(maps.size, m));
			});
			
			// custom maps
			Vars.maps.customMaps().each(m -> {
				if(maps.contains(s -> s instanceof CustomMapSlot custom && custom.map == m)) return;
				maps.add(new CustomMapSlot(maps.size, m));
			});
			Log.info("@ maps loaded", maps.size);
		} catch (Exception e) {
			Log.err(e);
		}
	}
	
	public static MapMaker getCreateMaker(String uuid) {
		if(!makers.containsKey(uuid)) makers.put(uuid, new MapMaker(uuid));
		return makers.get(uuid);
	}
	
	public static @Nullable MapMaker maker(String uuid) {
		return makers.get(uuid);
	}
	
}
