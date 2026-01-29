package agzam4.votes;

import agzam4.Game;
import agzam4.bot.Bots;
import agzam4.bot.TelegramBot;
import agzam4.bot.Bots.NotifyTag;
import agzam4.managers.Kicks;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.core.NetServer;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration.Config;

public class KickVoteSession extends VoteSession {

	public static Config requiredMapPlayertime = 
			new Config("votekickRequiredMapPlaytime", "Минимальное требуемое время в минутах на карте", 5);
	
	public static Config requiredTotalPlayertime = 
			new Config("votekickRequiredPlaytime", "Минимальное требуемое время в минутах на карте", 15);
	
	public static @Nullable KickVoteSession current = null;
	
	public Player kicker, target;
	public String reason;
	
	public Runnable passListener = () -> {};
	
	public KickVoteSession(Player kicker, Player target, String reason) {
		super(NetServer.voteDuration);
		this.kicker = kicker;
		this.target = target;
		this.reason = reason;
		current = this;
	}
	
	@Override
	public void onFail() {
		Call.sendMessage(Strings.format("[lightgray]Голосование провалено. Недостаточно голосов, чтобы кикнуть [orange] @[lightgray].", target.name));
		Bots.notify(NotifyTag.votekick, "Голосование провалено");
	}
	
	@Override
	public void onPass() {
		Call.sendMessage(Strings.format("[orange]Голосование принято. [red] @[orange] забанен на @ минут", target.name, (NetServer.kickDuration / 60)));
		Bots.notify(NotifyTag.votekick, 
			Strings.format(bungle("pass.bot"), TelegramBot.strip(target.name), NetServer.kickDuration / 60),
			Strings.format(bungle("pass.admin.bot"), TelegramBot.strip(target.name), (NetServer.kickDuration / 60), target.uuid(), target.usid(), target.ip())
		);
		Kicks.kick(kicker, target, reason, NetServer.kickDuration);
		passListener.run();
	}
	
	@Override
	public void onVote(Player player, int d) {
		Call.sendMessage(Strings.format("[lightgray]@[lightgray] проголосовал @[] кик[orange] @[lightgray].[accent] (@/@)\n[lightgray]Напиши[orange] /vote <y/n>[] чтобы проголосовать",
				player.name, d > 0 ? "[green]за" : "[red]против", target.name, votes, votesRequired()));		
	}
	
	@Override
	public void cancel() {
		super.cancel();
		current = null;
	}

	String bungle(String name) {
		return Game.bungle("command.response.votekick." + name);
	}

	@Override
	public int votesRequired() {
		return 2 + (Groups.player.size() > 4 ? 1 : 0);	
	}
	
}
