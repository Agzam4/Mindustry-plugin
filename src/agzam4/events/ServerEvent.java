package agzam4.events;

import arc.util.I18NBundle;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.BlockDestroyEvent;
import mindustry.game.EventType.ConfigEvent;
import mindustry.game.EventType.DepositEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.WithdrawEvent;
import mindustry.gen.Player;

public abstract class ServerEvent {

	public final String name;
	public final EventNet net;
	
	public I18NBundle bundle;
	private boolean isRunning;
	
//	protected String color = "white";
//	public boolean isGenerated;
//	public ServerEvents type = null;
//	private String tagName;
	

	public ServerEvent(String name) {
		this.name = name;
		net = new EventNet(this);
		init();
//		this.tagName = bungle("name");
//		this.color = bungle("color");
//		commandName = name.toLowerCase().replaceAll(" ", "_");
	}
	
	public final boolean isRunning() {
		return isRunning;
	}
	
	public final void run() {
		isRunning = true;
//		announce();
	}
	
	public final void runSilently() {
		isRunning = true;
	}
	
	public final void stop() {
		isRunning = false;
	}

	public final String bungle(String string) {
		return bundle.get(string, "[red]???" + string + "???[]");
	}
	
	public final String bungle(String text, Object... args) {
		return Strings.format(bungle(text), args);
	}
	
	public final String bungleDef(String string, String def) {
		return bundle.get(string, def);
	}

	public abstract void init();

	public void announce() {
		net.announce("announce");
		net.message("announce");
	}

	public void playerJoin(PlayerJoin e) {
		net.message(e.player, "info");
	}
	
	public String getInfo() {
		return bungle("info");
	}

	
	
	/**
	 * 	On game update (every tick)
	 */
	public abstract void update();
	
	
//	public String getName() {
//		return name;
//	}
	
//	public String getCommandName() {
//		return commandName;
//	}
	
//	public String getColor() {
//		return bungle("color");
//	}
	
	@Override
	public String toString() {
		return "Event(" + name + ")";
	}

	/**
	 * Calls after world loaded but game not stared
	 * Allows to use set blocks and other modifications without Net calls
	 * May be called before running
	 */
	public void prepare() {
		// for @Override
	}

	public void blockBuildEnd(BlockBuildEndEvent e) {
		// for @Override
	}

	public void unitDestroy(UnitDestroyEvent e) {
		// for @Override
	}

	public void deposit(DepositEvent e) {
		// for @Override
	}

	public void tap(TapEvent e) {
		// for @Override
	}

	public void withdraw(WithdrawEvent e) {
		// for @Override
	}

	public void config(ConfigEvent event) {
		// for @Override
	}

	public void blockDestroy(BlockDestroyEvent event) {
		// for @Override
	}

	public void trigger(@Nullable Player player, String... args) {
		// for @Override
	}


//	public void trigger(String command) {
//		// for @Override
//	}

//	public int id() {
//		return type == null ? -1 : type.ordinal();
//	}
//	public String getTagName() {
//		return tagName;
//	}


	
}
