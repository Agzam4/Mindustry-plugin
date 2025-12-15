package agzam4.votes;

import arc.struct.ObjectIntMap;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public abstract class VoteSession {

	protected ObjectIntMap<String> voted = new ObjectIntMap<>();
	protected ObjectIntMap<String> scales = new ObjectIntMap<>();
	public Timer.Task task;
	public int votes;

	protected float percent = .45f;

	public VoteSession(float duration) {
		this.task = Timer.schedule(() -> {
			if(checkPass()) return;
			onFail();
			cancel();
		}, duration);
		
		totalScale();
	}
	
	public abstract void onFail();
	public abstract void onPass();
	public abstract void onVote(Player player, int ds);
	
	public boolean isPlayerVoted(Player player, int d) {
		return voted.get(player.uuid(), 0) == d || voted.get(Vars.netServer.admins.getInfo(player.uuid()).lastIP, 0) == d;
	}

	public void vote(Player player, int d) {
		
		int scale = getCreatePlayerScale(player);
		
		int lastVote = voted.get(player.uuid(), 0) | voted.get(Vars.netServer.admins.getInfo(player.uuid()).lastIP, 0) | voted.get(player.ip(), 0);
		votes -= lastVote * scale;
		votes += d * scale;
		voted.put(player.uuid(), d);
		voted.put(Vars.netServer.admins.getInfo(player.uuid()).lastIP, d);
		voted.put(player.ip(), d);
		
		onVote(player, d);

		checkPass();
	}

	protected final int getCreatePlayerScale(Player player) {
		int scl = scales.get(player.uuid(), -1);
		if(scl != -1) return scl;
		scl = Math.max(playerScale(player), 1);
		scales.put(player.uuid(), scl);
		return scl;
	}
	
	public int playerScale(Player player) {
		return 1;
	}
	
	public boolean checkPass() {
		if(votes < votesRequired()) return false;
		onPass();
		cancel();
		return true;
	}
	
	public void cancel() {
		task.cancel();
	}
	
	private int tmp = 0;
	
	public final int totalScale() {
		tmp = 0;
		Groups.player.forEach(p -> { tmp += getCreatePlayerScale(p); });
		return tmp;
	}
	
	public int votesRequired() {
		if(Groups.player.size() <= 2) return totalScale();
		return Math.max(1, (int) Math.ceil(totalScale()*percent));
	}
}
