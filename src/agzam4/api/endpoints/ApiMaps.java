package agzam4.api.endpoints;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import agzam4.admins.Admins;
import agzam4.api.auth.SensitiveData;
import agzam4.api.auth.SensitiveData.SensitiveType;
import agzam4.api.endpoints.ApiEvents.EventInfo;
import agzam4.commands.Permissions;
import agzam4.events.ServerEventsManager;
import agzam4.maps.UserMapSlot;
import agzam4.maps.FileMapSlot;
import agzam4.maps.MapSlot;
import agzam4.maps.MapsManager;
import agzam4gen.api.dependencies.Auth;
import agzam4gen.api.dependencies.Body;
import agzam4gen.api.dependencies.BodyParm;
import agzam4gen.api.dependencies.ChunkedEncoding;
import agzam4gen.api.dependencies.HeaderParm;
import agzam4proc.api.ApiAnnotations.Parm;
import agzam4proc.api.ApiAnnotations.Post;
import agzam4proc.api.ApiAnnotations.Router;
import agzam4proc.api.ApiAnnotations.Type;
import agzam4proc.api.lib.ApiResponse;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.net.Administration.PlayerInfo;

@Router("/maps")
public class ApiMaps {

	
	
	@Type
	public static class MapSlotInfo {
		
		public int id;
		public String name;
		public int version;
		public String status;

		public boolean editable;
		public boolean canApprove;
		public boolean enabled;
		public boolean canManage;

		public int[] authors;
		
		public int[] events;
	}

	@Post
	public static MapSlotInfo[] slots(@Auth PlayerInfo info) {
		Seq<? extends MapSlot> slots;
		if(Admins.has(info, Permissions.manageMaps)) {
			slots = MapsManager.maps;
		} else {
			var maker = MapsManager.maker(info.id);
			if(maker == null) return new MapSlotInfo[0];
			slots = maker.slots;
		}
		MapSlotInfo[] infos = new MapSlotInfo[slots.size];
		for (int i = 0; i < infos.length; i++) {
			infos[i] = write(info, slots.get(i));
		}
		return infos;
	}
	
	private static MapSlotInfo write(PlayerInfo info, MapSlot slot) {
		MapSlotInfo slotinfo = new MapSlotInfo();
		slotinfo.id = slot.id;
		slotinfo.enabled = slot.enabled;
		slotinfo.canManage = Admins.has(info, Permissions.manageMaps);
		

		slotinfo.events = new int[ServerEventsManager.events.size];
    	for (int i = 0; i < slotinfo.events.length; i++) {
    		String key = ServerEventsManager.events.get(i).name;
    		if(!slot.events.containsKey(key)) continue;
    		slotinfo.events[i] = slot.events.get(key) ? 1 : -1;
		}
		
		if(slot instanceof UserMapSlot ums) {
			slotinfo.status = ums.status.name();
			slotinfo.name = ums.name;
			slotinfo.editable = Admins.has(info, Permissions.manageMaps);
			slotinfo.canApprove = ums.uploaded() && Admins.has(info, Permissions.manageMaps);
			slotinfo.authors = new int[ums.authors.size];
			slotinfo.version = ums.version;
			for (int i = 0; i < ums.authors.size; i++) slotinfo.authors[i] = SensitiveData.insertOrGet(ums.authors.get(i).uuid, SensitiveType.uuid);
		} else if(slot instanceof FileMapSlot fms) {
			slotinfo.status = Strings.camelToKebab(fms.type.name());
			slotinfo.name = slot.name();
			slotinfo.authors = new int[0];
		}
		return slotinfo;
	}

	@Type
	public static class MapCreatorInfo {
		
		public int id;
		public int[] slots;
		public int maxSlots;
		
	}

	@Post
	public static MapSlotInfo createSlot(@Auth PlayerInfo info, @BodyParm String name) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMaps)) throw ApiResponse.forbidden;
		var slot = MapsManager.createSlot(name);
		MapsManager.save();
		return write(info, slot);
	}

	@Post
	public static MapSlotInfo renameSlot(@Auth PlayerInfo info, @BodyParm int id, @BodyParm String name) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMaps)) throw ApiResponse.forbidden;
		var slot = MapsManager.maps.get(id);
		if(!(slot instanceof UserMapSlot ums)) throw ApiResponse.forbidden;
		if(name.isEmpty()) new ApiResponse("Name is empty", 422);
		ums.name = name;
		MapsManager.save();
		return write(info, slot);
	}
	
	@Post
	public static MapSlotInfo setEnabledSlot(@Auth PlayerInfo info, @BodyParm int id, @BodyParm boolean enabled) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMaps)) throw ApiResponse.forbidden;
		var slot = MapsManager.maps.get(id);
		slot.enabled = enabled;
		MapsManager.save();
		return write(info, slot);
	}

	@Post
	public static MapSlotInfo upload(@Auth PlayerInfo info, @HeaderParm @Parm("Map-Slot") int mapSlot, @Body InputStream file) throws ApiResponse, IOException {
		var maker = MapsManager.maker(info.id);
		if(!Admins.has(info, Permissions.manageMaps)) {
			Log.info(maker);
			if(maker == null) throw ApiResponse.forbidden;
			Log.info(maker.slots);
			if(!maker.slots.contains(s -> s.id == mapSlot)) throw ApiResponse.forbidden;
		}
		var slot = MapsManager.maps.get(mapSlot);
		if(!(slot instanceof UserMapSlot ums)) throw ApiResponse.forbidden;
		try {
			ums.upload(file);
			Log.info("[red]OK!");
		} catch (IOException e) {
			throw new ApiResponse(e.getMessage(), 422);
		}
		MapsManager.save();
		return write(info, slot);
	}


	@Post
	public static void download(@Auth PlayerInfo info, @BodyParm int id, @ChunkedEncoding OutputStream os) throws ApiResponse, IOException {
		var maker = MapsManager.maker(info.id);
		if(!Admins.has(info, Permissions.manageMaps)) {
			Log.info(maker);
			if(maker == null) throw ApiResponse.forbidden;
			Log.info(maker.slots);
			if(!maker.slots.contains(s -> s.id == id)) throw ApiResponse.forbidden;
		}
		var slot = MapsManager.maps.get(id);
		if(!(slot instanceof UserMapSlot ums)) throw ApiResponse.forbidden;
		try {
			ums.download(os);
		} catch (IOException e) {
			Log.err(e);
			throw new ApiResponse(e.getMessage(), 422);
		}
	}
	
	

	@Post
	public static MapSlotInfo approve(@Auth PlayerInfo info, @BodyParm int id) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMaps)) throw ApiResponse.forbidden;
		var slot = MapsManager.maps.get(id);
		if(!(slot instanceof UserMapSlot ums)) throw ApiResponse.forbidden;
		ums.approve();
		MapsManager.save();
		return write(info, slot);
	}

	@Post
	public static MapSlotInfo reject(@Auth PlayerInfo info, @BodyParm int id) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMaps)) throw ApiResponse.forbidden; 
		var slot = MapsManager.maps.get(id);
		if(!(slot instanceof UserMapSlot ums)) throw ApiResponse.forbidden;
		ums.reject();
		MapsManager.save();
		return write(info, slot);
	}

	@Post
	public static MapCreatorInfo[] creators(@Auth PlayerInfo info) throws ApiResponse {
		if(!Admins.has(info, Permissions.manageMapCreators)) throw ApiResponse.forbidden;
		Seq<MapCreatorInfo> infos = Seq.with();
		MapsManager.makers.each((uuid, creator) -> {
			var creatorinfo = new MapCreatorInfo();
			creatorinfo.id = SensitiveData.insertOrGet(uuid, SensitiveType.uuid);
			creatorinfo.slots = new int[creator.slots.size];
			for (int i = 0; i < creatorinfo.slots.length; i++) {
				creatorinfo.slots[i] = creator.slots.get(i).id;
			}
			creatorinfo.maxSlots = creator.maxMaps;
		});
		return infos.toArray(MapCreatorInfo.class);
	}

	@Post
	public static MapSlotInfo setMapCreators(@Auth PlayerInfo info, @BodyParm int id, @BodyParm int[] authors) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMapCreators)) throw ApiResponse.forbidden;
		var slot = MapsManager.maps.get(id);
		if(!(slot instanceof UserMapSlot ums)) throw ApiResponse.forbidden;
		ums.clearAuthors();
		for (var a : authors) {
			String uuid = SensitiveData.resolve(a);
			if(uuid == null) continue;
			ums.addAuthor(MapsManager.getCreateMaker(uuid));
		}
		MapsManager.save();
		return write(info, slot);
	}
	

	@Post
	public static MapSlotInfo setEvent(@Auth PlayerInfo info, @BodyParm String event, @BodyParm int status, @BodyParm int id) throws ApiResponse, IOException {
		if(!Admins.has(info, Permissions.manageMaps)) throw ApiResponse.forbidden; 
		if(ServerEventsManager.find(event) == null) throw ApiResponse.notFound;

		var slot = MapsManager.maps.get(id);
		if(status == 0) slot.events.remove(event);
		if(status == 1) slot.events.put(event, true);
		if(status == -1) slot.events.put(event, false);

		MapsManager.save();
		return write(info, slot);
	}
}
