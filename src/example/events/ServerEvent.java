package example.events;

import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.DepositEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.WithdrawEvent;
import mindustry.game.GameStats;

public abstract class ServerEvent {

	private String name;
	private String commandName;
	protected String color = "white";
	private boolean isRunning;
	public boolean isGenerated;
	
	public ServerEvent(String name) {
		this.name = name;
		commandName = name.toLowerCase().replaceAll(" ", "_");
	}
	
	
	public void run() {
		isRunning = true;
		announce();
	}
	
	public void stop() {
		isRunning = false;
	}

	public abstract void init();
	public abstract void announce();
	
	/**
	 * 	On game update (every tick)
	 */
	public abstract void update();
	public abstract void generateWorld();
	
	
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public String getName() {
		return name;
	}
	
	public String getCommandName() {
		return commandName;
	}
	
	public String getColor() {
		return color;
	}
	
	@Override
	public String toString() {
		return super.toString() + " " + getCommandName();
	}


	protected void blockBuildEnd(BlockBuildEndEvent e) {
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


	public void playerJoin(PlayerJoin e) {
		// for @Override
	}
}
