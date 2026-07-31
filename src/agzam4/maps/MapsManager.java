package agzam4.maps;

import java.io.IOException;

import agzam4.CommandsManager.ReceiverType;
import agzam4.admins.Admins;
import agzam4.commands.Permissions;
import agzam4.maps.FileMapSlot.MapFileTypes;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Reflect;
import arc.util.Strings;
import arc.util.serialization.Jval;
import arc.util.serialization.Jval.Jformat;
import mindustry.Vars;

public class MapsManager {

	protected static Fi dir = Vars.dataDirectory.child("user-maps");
	protected static Fi database = dir.child("database.hjson");
	
	public static ObjectMap<String, MapCreator> makers = ObjectMap.of();
		
	// List of all maps, each map can contains several authors (or not contains any)
	public static Seq<MapSlot> maps = Seq.with();
	
	public static Seq<MapSlot> bungle = Seq.with();
	
	public static void init() {
		load();
		Vars.maps.setMapProvider((mode, prev) -> {
			Log.info("Bungle: @", bungle);
			if(bungle.size == 0) {
				bungle.addAll(maps);
				bungle.shuffle();
			}
			while (bungle.size > 0) {
				var slot = bungle.remove(0);
				if(!slot.enabled) continue;
				var map = slot.map();
				if(map == null) continue;
				return map;
			}
			Log.warn("Valid maps not found");
			return prev;
		});
		Log.info("LOADED: " + Vars.maps);
	}
	
	
	private static void load() {
		try {
			// Loading user maps
			if(database.exists()) {
				String hjson = database.readString();
				var data = Jval.read(hjson);
				var defaultList = data.get("maps").asArray();
				mapsLoop:
				for (int i = 0; i < defaultList.size; i++) {
					var item = defaultList.get(i);
					for (var fmt : MapFileTypes.values()) {
						if(!item.has(fmt.tag)) continue;
						var slot = new FileMapSlot(fmt, i, item);
						if(slot.map == null) continue mapsLoop; // Loaded but not found
						maps.add(slot); // OK
						continue mapsLoop;
					}
					maps.add(new UserMapSlot(i, defaultList.get(i)));
				}
			} else {
				database.parent().mkdirs();
			}
			
		} catch (Exception e) {
			Log.err(e);
		}
		
		// build-in maps
		Vars.maps.defaultMaps().each(m -> {
			if(maps.contains(s -> s instanceof FileMapSlot def && def.map == m)) return;
			maps.add(new FileMapSlot(MapFileTypes.buildIn, maps.size, m));
		});
		
		// custom maps
		Vars.maps.customMaps().each(m -> {
			if(maps.contains(s -> s instanceof FileMapSlot custom && custom.map == m)) return;
			maps.add(new FileMapSlot(MapFileTypes.custom, maps.size, m));
		});
		Log.info("@ maps loaded", maps.size);
	}
	
	
	public synchronized static void save() throws IOException {
		var object = Jval.newObject();
		{
			var mapsArray = Jval.newArray();
			object.put("maps", mapsArray);
			for (int i = 0; i < maps.size; i++) {
				mapsArray.add(maps.get(i).save());
			}
		}
		String result = object.toString(Jformat.formatted);
		database.writeString(result, false);
//		object.writeTo(database.writer(false), Jformat.hjson);
	}
	
	public static MapCreator getCreateMaker(String uuid) {
		if(!makers.containsKey(uuid)) makers.put(uuid, new MapCreator(uuid));
		return makers.get(uuid);
	}
	
	public static @Nullable MapCreator maker(String uuid) {
		return makers.get(uuid);
	}

	public static UserMapSlot createSlot(String name) {
		var slot = new UserMapSlot(maps.size, name);
		maps.add(slot);
		return slot;
	}
	
	public static Seq<MapSlot> list(Object receiver) {
		boolean anyMap = Admins.has(receiver, Permissions.manageMaps);
		return maps.select(map -> {
			if(!map.enabled && !anyMap) return false;
			if(map.map() == null) return false;
			return true;
		});
	}
}
