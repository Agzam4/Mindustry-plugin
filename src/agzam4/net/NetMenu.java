package agzam4.net;

import java.awt.Event;

import arc.Events;
import arc.func.Cons;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.game.EventType.MenuOptionChooseEvent;
import mindustry.gen.*;

public class NetMenu {

	private static int ids = 0;
	
	
	private static IntMap<ShownNetMenu> menus = new IntMap<>();
	

	public String title = "";
	public String text = "";
	
	private Seq<Seq<NetMenuButton>> table = new Seq<>();
	
	public static void init() {
		Events.on(MenuOptionChooseEvent.class, e -> {
			Log.info("MenuOptionChooseEvent: @ @ @", e.player, e.menuId, e.option);
			var menu = menus.remove(e.menuId);
			if(menu == null) {
				Log.info("Menu not found");
				return;
			}
			
			Log.info("Player: @", menu.player, e.player);
			if(e.option < 0) {
				Log.info("Menu close");
				if(menu.menu.onClose != null) menu.menu.onClose.get(menu.player);
			}
			menu.menu.listeners[e.option].get(menu.player);
		});
	}
	
	public NetMenu() {
		row();
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
	
	public void show(Player player) {
		Log.info("Menu shown to @ (#@)", player, ids);
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
		
		menus.put(ids, new ShownNetMenu(this, player));
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
		
		public void get(Player player);
		
	}
	
	
	private static class ShownNetMenu {

		public final NetMenu menu;
		public final Player player;
		
		public ShownNetMenu(NetMenu menu, Player player) {
			this.player = player;
			this.menu = menu;
		}
		
	}
	
}
