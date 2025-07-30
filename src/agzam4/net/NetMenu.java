package agzam4.net;

import java.awt.Event;

import javax.validation.constraints.Null;

import arc.Events;
import arc.func.Cons;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.game.EventType.MenuOptionChooseEvent;
import mindustry.gen.*;

public class NetMenu {

	private static int ids = 0;
	
	
	private static IntMap<NetMenu> menus = new IntMap<>();
	

	public String title = "";
	public String text = "";
	
	private Seq<Seq<NetMenuButton>> table = new Seq<>();
	
	public static void init() {
		Events.on(MenuOptionChooseEvent.class, e -> {
			var menu = menus.remove(e.menuId);
			if(menu == null) {
				return;
			}
			if(menu.player != e.player) return;
			if(e.option < 0) {
				if(menu.onClose != null) menu.onClose.get();
				return;
			}
			menu.listeners[e.option].get();
			if(menu.builder != null) menu.show(menu.player);
		});
	}

	
	public NetMenu() {
		row();
	}

	public NetMenu(String title) {
		this.title = title;
		row();
	}
	
	@Nullable Runnable builder = null;

	public void build(Runnable builder) {
		this.builder = builder;
	}
	
	public NetMenu button(String text, NetMenuListener listener) {
		table.get(table.size-1).add(new NetMenuButton(text, listener));
		return this;
	}
	
	public NetMenu row() {
		table.add(new Seq<NetMenuButton>());
		return this;
	}
	
	
	private String[][] array = {};
	private NetMenuListener[] listeners = {};
	private NetMenuListener onClose = null;
	
	private Player player;
	
	public void show(Player player) {
		if(builder != null) {
			table.clear();
			row();
			builder.run();
		}
		int count = 0;
		if(table.size != array.length) array = new String[table.size][];
		for (int y = 0; y < array.length; y++) {
			int w = table.get(y).size;
			if(array[y] == null || array[y].length != w) array[y] = new String[w];
			for (int x = 0; x < w; x++) {
				array[y][x] = table.get(y).get(x).text;
				count++;
			}
		}
		if(count != listeners.length) listeners = new NetMenuListener[count];
		count = 0;
		for (int y = 0; y < array.length; y++) {
			for (int x = 0; x < array[y].length; x++) {
				listeners[count++] = table.get(y).get(x).listener;
			}
		}
		this.player = player;
		menus.put(ids, this);
		Call.menu(player.con, ids, title, text, array);
		ids++;
	}
	
	private static class NetMenuButton {
		
		public NetMenuListener listener;
		public String text;
		
		public NetMenuButton(String text, NetMenuListener listener) {
			this.text = text;
			this.listener = listener;
		}
		
	}
	
	public static interface NetMenuListener {
		
		public void get();
		
	}

	
	
}
