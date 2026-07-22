package agzam4.maps;

import java.io.IOException;
import java.io.InputStream;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.serialization.Jval;
import mindustry.io.MapIO;
import mindustry.maps.Map;

public class UserMapSlot extends MapSlot {

	public static final Fi mapsDir = MapsManager.dir.child("maps");
	
	public String name;
	
	// Increments with any approve
	public int version;
	
	/**
	 * Override events on map
	 * true - enable event
	 * false - never use event on this map
	 * not in map means - use default
	 */
	public ObjectMap<String, Boolean> events;
	
	public Seq<MapMaker> authors = Seq.with();
	
	public MapSlotStatus status = MapSlotStatus.rejected;

	Map map = null;
	
	public UserMapSlot(int id, String name) {
		super(id);
		this.name = name;
		this.version = 0;
		try {
			updateMap();
		} catch (IOException e) {
			Log.err(e);
		}
	}
	
	public UserMapSlot(int id, Jval jval) {
		super(id);
		this.name = jval.getString("name", "unnamed");
		this.version = jval.getInt("version", 0);
		this.status = MapSlotStatus.of(jval.getString("status"), MapSlotStatus.approved);
		var authorsList = jval.get("authors").asArray();
		for (int a = 0; a < authorsList.size; a++) {
			var maker = MapsManager.getCreateMaker(authorsList.get(a).asString());
			maker.slots.add(this);
			authors.add(maker);
		}
		try {
			updateMap();
		} catch (IOException e) {
			Log.err(e);
		}
	}

	@Override
	public Jval save() {
		var jval = Jval.newObject();
		jval.put("name", name);
		jval.put("version", version);
		jval.put("status", status.name());
		var authorsList = Jval.newArray();
		for (int a = 0; a < authors.size; a++) {
			authorsList.add(authors.get(a).uuid);
		}
		jval.put("authors", authorsList);
		return jval;
	}
	
	public enum MapSlotStatus {
		
		approved, // map added and can be used
		verification, // waiting for approve
		rejected // map not added
		;

		static MapSlotStatus of(String string, MapSlotStatus def) {
			for (var v : values()) {
				if(string.equals(v)) return v;
			}
			return def;
		}
		
	}

	public enum FileState {
		
		current(".msav"),
		backup(".msav.backup"),
		uploaded(".msav.uploaded")
		;

		String ext;
		
		FileState(String ext) {
			this.ext = ext;
		}
		
		private Fi get(UserMapSlot slot) {
			return mapsDir.child(slot.id+ext);
		}
		
	}
	
	private Fi file(FileState state) {
		return state.get(this);
	}

	public synchronized void upload(InputStream mapData) throws IOException {
		var fi = file(FileState.uploaded);
		fi.write(mapData, false);
		MapIO.createMap(fi, true);
		status = MapSlotStatus.verification;
	}
	
	public synchronized void approve() throws IOException {
		if(status == MapSlotStatus.approved) return;

        var uploaded = file(FileState.uploaded);
		var current = file(FileState.current);
		
        if (!uploaded.exists()) throw new IOException("Uploaded file not found");
        
        MapIO.createMap(uploaded, true);
        
		if(status != MapSlotStatus.rejected) {
			if(current.exists()) current.copyTo(file(FileState.backup)); // current -> backup
		}
		
		uploaded.moveTo(file(FileState.current)); // uploaded -> current
		status = MapSlotStatus.approved;
		version++;
		updateMap();
	}

	public synchronized void reject() throws IOException {
		if (status == MapSlotStatus.rejected) return;
		
		if(status == MapSlotStatus.approved) {
			restoreBackup();
		}
		
		status = MapSlotStatus.rejected;
	}
	
	public synchronized void restoreBackup() throws IOException {
		var backup = file(FileState.backup);
		var current = file(FileState.current);
		if(backup.exists()) {
			backup.moveTo(current);
			version--;
		} else {
            current.delete();
            version = 0;
		}
		updateMap();
	}
	
	private void updateMap() throws IOException {
		var current = file(FileState.current);
		if(current.exists()) {
			map = MapIO.createMap(current, true);
			return;
		}
		map = null;
	}


	public void updateInfo() {
		if(map == null) return;
		map.tags.put("name", name());
		map.tags.put("author", authors.toString(", ", m -> m.name()));
	}

	@Override
	public Map map() {
		updateInfo();
		return map;
	}

	@Override
	public String name() {
		return Strings.format("@ v@", name, version);
	}
	
	
}
