package agzam4.commands.server;

import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.admins.Admins;
import agzam4.commands.CommandHandler;
import agzam4.commands.Permissions;
import mindustry.Vars;

public class RunwaveCommand extends CommandHandler<Object> {

	{
		desc = "Запускает волну";
	}

	@Override
	public void command(String[] args, ResultSender sender, Object receiver, ReceiverType type) {
		boolean force = Admins.has(receiver, Permissions.forceRunwave);
		if(require(!force && Vars.state.enemies > 0, sender, type.bungle("runwave.enemies"))) return;
		Vars.logic.runWave();
		sender.sendMessage(type.bungle("runwave.ready"));
    }

}
