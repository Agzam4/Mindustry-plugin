package example.achievements;

import static mindustry.Vars.*;

import mindustry.gen.Call;
import mindustry.gen.Player;

public class PlayerStatus {

	private String uidd;
	private AchievementsManager manager;
	
	public PlayerStatus(AchievementsManager manager, String uidd) {
		this.manager = manager;
		this.uidd = uidd;
	}
	
	int wavesSurvived = 0, lastWave = 0;

	public void update(Player player) {
		if(state.wave > lastWave) {
			lastWave = state.wave;
			wavesSurvived++;
			
			if(wavesSurvived >= 10) {
				if(manager.achievementCompleted(uidd, AchievementsManager.Achievement.SURVIVAL_250_WAVES)) {
					achievementCompletedToast(player, "Проживите 250 волн");
				}
			}
		}
	}

	private void achievementCompletedToast(Player player, String title) {
		Call.warningToast(player.con, 0, "Достижение получено: [gold]" + title);
	}
	
	public void worldLoadEnd() {
		lastWave = 0;
		wavesSurvived = 0;
	}

	public void unitCreate() {
		
	}
}
