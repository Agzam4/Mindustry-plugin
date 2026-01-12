package agzam4.commands;

import agzam4.CommandsManager.ResultSender;
import agzam4.CommandsManager.ReceiverType;
import mindustry.gen.Player;

public abstract class PlayerCommandHandler extends CommandHandler<Player> {

	public abstract void command(String[] args, Player player);
	
	@Override
	public void command(String[] args, ResultSender sender, Player receiver, ReceiverType type) {
		command(args, receiver);
	}
	
}
