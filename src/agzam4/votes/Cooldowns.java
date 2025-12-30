package agzam4.votes;

import arc.struct.ObjectMap;
import arc.util.Time;

public class Cooldowns<T> {
	
	private ObjectMap<T, Cooldown> cooldowns = new ObjectMap<T, Cooldown>();

	private final long intervalms;
	
	public Cooldowns(float seconds) {
        intervalms = (int)(seconds * 1000);
	}
	
	public Cooldown get(T t) {
		return cooldowns.get(t, () -> new Cooldown());
	}
	
	public class Cooldown {
    	
        private long time;
        
        private Cooldown() {}

        public boolean cooldown(){
            return Time.timeSinceMillis(time) <= intervalms;
        }

        public void start(){
            time = Time.millis();
        }

        public void stop(){
            time = Time.millis() + intervalms;
        }
    }

}
