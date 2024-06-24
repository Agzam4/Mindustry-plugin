package example.events;

import java.util.HashMap;
import arc.audio.Sound;
import arc.func.Cons;
import arc.graphics.Color;
import arc.util.Strings;
import example.CommandsManager;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Sounds;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Sorter.SorterBuild;
import mindustry.world.blocks.logic.MessageBlock.MessageBuild;

public class MusicEvent extends ServerEvent {

//	private static final Sound sorterSounds[] = {
//			Sounds.shoot,
//			Sounds.boom,
//			Sounds.laser,
//			Sounds.release,
//			Sounds.titanExplosion
//	};
//	
//	private final static float sorterSoundsVoulme[] = {
//			1,
//			1f,
//			.25f,
//			1f,
//			1f
//	};
//	
//	private static final Sound iSorterSounds[] = {
//			Sounds.back,
//			Sounds.unlock
//	};\

	static String[] noteNames = {
			"C",
			"C#",
			"D",
			"D#",
			"E",
			"F",
			"F#",
			"G",
			"G#",
			"A",
			"A#",
			"B",
	};
	
	static float[] notes = { 
			16.35f,	// 0)  C		
			17.32f,	// 1)  C#	Db
			18.35f, // 2)  D
			19.45f, // 3)  D#	Eb
			20.60f, // 4)  E
			21.83f, // 5)  F	
			23.12f, // 6)  F#	Gb
			24.50f, // 7)  G	
			25.96f, // 8)  G#	Ab
			27.50f, // 9)  A	A
			29.14f, // 10) A#	Bb
			30.87f  // 11) B	
	};
	
	public static final HashMap<Block, Cons<Building>> soundPlayers;
	
	public static MusicEvent current;
	
	static int lastId = 0;
	
	static {
		soundPlayers = new HashMap<Block, Cons<Building>>();

		soundPlayers.put(Blocks.sorter, build -> {
			if(build instanceof SorterBuild sorter) {
				Item item = sorter.config();
				if(item == null) return;
				int id = item.id;
				
				if(id <= 8) {
					playSound(Sounds.shootAlt, .75f, 10 + id*5f);
					return;
				}
				if(id <= 16) {
					playSound(Sounds.shootAltLong, .75f, 10 + (id-8)*5f);
					return;
				}
				
				playSound(Sounds.boom, .75f, 5 + (id-16)*5f);
//				if(id/4 < sorterSounds.length) {
//					playSound(sorterSounds[id/4], sorterSoundsVoulme[id/4], 1 + id%4/2f);
//				} else {
//					playSound(id);
//				}
			}
		});
		
		soundPlayers.put(Blocks.invertedSorter, build -> {
			if(build instanceof SorterBuild sorter) {
				Item item = sorter.config();
				if(item == null) return;
				int id = item.id;

				if(id <= 8) {
					playSound(Sounds.shootAlt, .75f, 10 + id*5f);
					return;
				}
				if(id <= 16) {
					playSound(Sounds.shootAltLong, .75f, 10 + (id-8)*5f);
					return;
				}
				
				playSound(Sounds.rockBreak, .75f, 5 + (id-16)*5f);
//				if(id <= 8) {
//					playSound(Sounds.unlock, 1, 1 + id/2f);
//					return;
//				}
//				if(id <= 12) {
//					playSound(Sounds.shootAlt, .1f, 5 + (id-8)*5f);
//					return;
//				}
//				if(id <= 16) {
//					playSound(Sounds.shoot, .25f, 5 + (id-12)*5f);
//					return;
//				}
			}
//			Team.sharded.rules().blockHealthMultiplier
//			Sounds.getSoundId(Sounds.chatMessage)
		});
		
		soundPlayers.put(Blocks.message, build -> {
			if(build instanceof MessageBuild message) {
				String[] data = message.config().split(" ");
				int id = 0;
				float pitch = 1;
				float voulme = 0;
				if(data.length > 0) {
					if(data[0].equals("?")) {
						id = lastId; 
					} else {
						lastId = id = Strings.parseInt(data[0], 0);
					}
				}
				if(data.length > 1) {
					boolean found = false;
					for (int i = 0; i < noteNames.length; i++) {
						if(data[1].equalsIgnoreCase(noteNames[i])) {
							found = true;
							pitch = notes[i];
							break;
						}
					}
					if(!found) pitch = Strings.parseFloat(data[1], 1);
				}
				if(data.length > 2) {
					voulme = Strings.parseFloat(data[2], 1);
				}
				playSound(CommandsManager.sound(id), voulme, pitch);
//				build.configure(id + " " + voulme + " " + pitch);
			}
		});
	}
	
	
	public MusicEvent() {
		super("music_event");
		color = "violet";
	}

	@Override
	public void init() {
		
	}
	
	@Override
	public void playerJoin(PlayerJoin e) {
		e.player.sendMessage("[violet]Постройте цепочку: [white]" + Blocks.switchBlock.emoji() + Blocks.diode.emoji() + Blocks.sorter.emoji());
	}
	
	@Override
	public void announce() {
		Call.announce("[violet]Музыкальное событие начнется на следующей карте!");
		Call.sendMessage("[violet]Музыкальное событие начнется на следующей карте!");
	}

	int playerX, playerY;
	boolean isPlaying = false;
	
	int updates = 0;
	
	@Override
	public void update() {
		updates++;
		if(updates%3 == 0 && isPlaying) {
			Tile play = Vars.world.tile(playerX, playerY);
			if(play == null) {
				isPlaying = false;
				return;
			}

			if(play.build == null) {
				isPlaying = false;
				return;
			}

			if(play.block() == null) {
				isPlaying = false;
				return;
			}
			

			if(play.block() == Blocks.diode) {
				Building front = play.build.front();
				if(front != null) {
					if(front.block == Blocks.diode || soundPlayers.containsKey(front.block)) {// || front.block == Blocks.message) {
//						Call.sound(Sounds.place, 1, 1, 2);
						Call.effect(Fx.chainLightning, playerX*Vars.tilesize, playerY*Vars.tilesize, 0, Color.sky, front);
						playerX = front.tileX();
						playerY = front.tileY();
						return;
					}
				}
				Call.effect(Fx.mine, playerX*Vars.tilesize, playerY*Vars.tilesize, 0, Color.sky);
				isPlaying = false;
				return;
			}

			boolean needNext = false;
			if(play.block() == Blocks.switchBlock) {
				needNext = true;
			}

			Cons<Building> soundPlayer = soundPlayers.get(play.block());
			
			if(soundPlayer != null) {
				current = this;
				soundPlayer.get(play.build);
				needNext = true;
			}
			if(needNext) {
				for (int i = 0; i < 4; i++) {
					Building check = null;
					if(i == 0) check = play.build.front();
					if(i == 1) check = play.build.right();
					if(i == 2) check = play.build.left();
					if(i == 3) check = play.build.back();
					
					if(check != null) {
						if(check.block == Blocks.diode) {
							if(check.front() != play.build) {
								if(check.enabled()) {
									Call.effect(Fx.chainLightning, playerX*Vars.tilesize, playerY*Vars.tilesize, 0, Color.white, check);
									playerX = check.tileX();
									playerY = check.tileY();
									return;
								}
							}
						}
					}
				}
				Call.effect(Fx.mine, playerX*Vars.tilesize, playerY*Vars.tilesize, 0, Color.sky);
				isPlaying = false;
				return;
			}
			
			isPlaying = false;
			return;
		}
		
	}

	public static void playSound(int id) {
		playSound(id, 1);
	}

	public static void playSound(int id, float pitch) {
		playSound(CommandsManager.sound(id), 1f, pitch);
	}
	
	private static void playSound(Sound sound, float voulme, float pitch) {
		if(sound == null) return;
		if(current == null) return;
		Call.soundAt(sound, current.playerX*Vars.tilesize, current.playerY*Vars.tilesize, voulme, pitch);
	}
	
	@Override
	public void generateWorld() {
		Call.sendMessage("[violet]Постройте цепочку: [white]" + Blocks.switchBlock.emoji() + Blocks.diode.emoji() + Blocks.sorter.emoji());
	}
	
	@Override
	public void tap(TapEvent e) {
		if(isPlaying) return;
		if(e.tile == null) return;
		if(e.tile.block() != Blocks.switchBlock) return;
		if(e.tile.build == null) return;
		playerX = e.tile.centerX();
		playerY = e.tile.centerY();
		isPlaying = true;
	}
}
