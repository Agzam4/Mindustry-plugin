package agzam4.votes;

import agzam4.Game;
import agzam4.managers.Players;
import arc.Events;
import arc.math.Mathf;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.game.Team;
import mindustry.game.EventType.GameOverEvent;
import mindustry.gen.Call;
import mindustry.gen.Player;

public class SkipmapVoteSession extends VoteSession {

	public static @Nullable SkipmapVoteSession current = null;
	
	public static float voteDuration = 3 * 60;
	
	{{
		percent = .45f;
	}}
	
	public SkipmapVoteSession() {
		super(voteDuration);
		current = this;
	}

	@Override
	public void onFail() {
		Call.sendMessage("[lightgray]Голосование закончилось. Недостаточно голосов, чтобы пропустить карту");
	}

	@Override
	public void onPass() {
		Call.sendMessage("[gold]Голосование закончилось. Карта успешно пропущена!");
		Events.fire(new GameOverEvent(Team.derelict));
	}

	@Override
	public void onVote(Player player, int d) {
		Call.sendMessage(Strings.format("[@]@[lightgray] проголосовал @[] пропуска карты[accent] (@/100)\n[lightgray]Напишите[orange] /smvote <y/n>[], чтобы проголосовать [green]за[]/[red]против",
				Game.colorToHex(player.color), player.name, d >= 0 ? "[green]за" : "[red]против", votes*100/votesRequired()));
	}
	
	@Override
	public int playerScale(Player player) {
		int minutes = Players.mapPlaytime(player);
		if(minutes == 0) return 0;
		return (int) (10*Mathf.log(10, minutes+1));
	}
	
	@Override
	public void cancel() {
		super.cancel();
		current = null;
	}
	
	public static void stop() {
		if(current != null) current.cancel();		
	}

}
