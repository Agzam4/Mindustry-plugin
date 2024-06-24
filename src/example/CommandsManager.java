package example;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.glassfish.jersey.internal.inject.Custom;

import arc.Core;
import arc.Events;
import arc.audio.Sound;
import arc.files.Fi;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Timekeeper;
import arc.util.Timer;
import arc.util.CommandHandler.CommandRunner;
import example.bot.TelegramBot;
import example.events.ServerEvent;
import example.events.ServerEventsManager;
import example.events.abilities.BurnAbility;
import example.events.abilities.CoalAbility;
import example.events.abilities.CopperAbility;
import example.events.abilities.ExplodeAbility;
import example.events.abilities.GraphiteAbility;
import example.events.abilities.IceAbility;
import example.events.abilities.KnockbackAbility;
import example.events.abilities.LeadAbility;
import example.events.abilities.PhaseAbility;
import example.events.abilities.PlastanAbility;
import example.events.abilities.SandAbility;
import example.events.abilities.ScrapAbility;
import example.events.abilities.ShieldAbility;
import example.events.abilities.ShockAbility;
import example.events.abilities.SporeAbility;
import example.events.abilities.ThoriumAbility;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.core.World;
import mindustry.core.NetServer;
import mindustry.entities.abilities.Ability;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.game.EventType.BlockBuildBeginEvent;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.BlockDestroyEvent;
import mindustry.game.EventType.BuildSelectEvent;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.maps.Map;
import mindustry.net.Administration.ActionType;
import mindustry.net.Administration.Config;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OverlayFloor;
import mindustry.world.blocks.storage.CoreBlock;
import static example.Emoji.*;
import static mindustry.Vars.*;

public class CommandsManager {

	
	private ObjectMap<String, Integer> lastkickTime = new ObjectMap<>();
	
	public static SkipmapVoteSession[] currentlyMapSkipping = {null};
    public @Nullable VoteSession currentlyKicking = null;
    
    public static Seq<String> extraStarsUIDD;
	
	public static boolean chatFilter;		
	public static String discordLink = "";	// Link on discord "/setdiscord" to change
	public static int doorsCup = 100; 		// Max doors limit "/doorscup" to change

//	private static Team admin;				// Special team
	private static ArrayList<Integer> doorsCoordinates;

	private Player brushOwner = null;
	private Block brushBlock;
	private Floor brushFloor;
	private OverlayFloor brushOverlay;
	private int brushLastX = -1;
	private int brushLastY = -1;
	
	
	private Seq<CommandBlock> commandBlocks; 

	private boolean adminSandbox = false;
	private boolean blockInfo = false;

	private Player lastThoriumReactorPlayer;
	
	public static final Seq<Sound> sounds = new Seq<Sound>();

	public void init() {
		
		Field[] soundFields = Sounds.class.getFields();
		for (int i = 0; i < soundFields.length; i++) {
			if(Modifier.isStatic(soundFields[i].getModifiers())) {
				try {
					Object object = soundFields[i].get(Sounds.class);
					if(object instanceof Sound) {
						System.out.println((Sound) object);
						sounds.add((Sound) object);
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		
		discordLink = Core.settings.getString(ExamplePlugin.PLUGIN_NAME + "-discord-link", null);
		doorsCup = Core.settings.getInt(ExamplePlugin.PLUGIN_NAME + "-doors-cup", Integer.MAX_VALUE);
		discordLink = Core.settings.getString(ExamplePlugin.PLUGIN_NAME + "-discord-link", null);
		chatFilter = Core.settings.getBool(ExamplePlugin.PLUGIN_NAME + "-chat-filter", false);
		extraStarsUIDD = new Seq<>();
		
//		helpers = (Seq<String>) Core.settings.get(ExamplePlugin.PLUGIN_NAME + "-helpers", new Seq<String>());

//		admin = Team.all[10];
//		admin.name = "admin";

		doorsCoordinates = new ArrayList<>();
		commandBlocks = new Seq<CommandBlock>();

		
		registerPlayersCommands();
		registerAdminCommands();
		
    	Events.run(Trigger.update, () -> {
    		for (int i = 0; i < commandBlocks.size; i++) {
    			if(commandBlocks.get(i).needRemove) {
    				commandBlocks.remove(i);
    				return;
    			}
    			commandBlocks.get(i).update();
			}
    		if(brushOwner == null) return;
    		if(!brushOwner.shooting()) {
    			brushLastY = brushLastX = -1;
    			return;
    		}
    		if(brushBlock == null && brushFloor == null && brushOverlay == null) return;
    		
    		int tileX = World.toTile(brushOwner.mouseX());
    		int tileY = World.toTile(brushOwner.mouseY());
    		if(brushLastX == tileX && brushLastY == tileY) return;

    		if(brushLastX == -1 || brushLastY == -1) {
        		brushLastX = tileX;
        		brushLastY = tileY;
        		if(brushLastX == -1 || brushLastY == -1) return;
        		paintTile(Vars.world.tile(tileX, tileY), false);
    			return;
    		}
    		
    		int x0 = brushLastX;//Math.min(brushLastX, tileX);
    		int x1 = tileX;//Math.max(brushLastX, tileX);
    		int y0 = brushLastY;//Math.min(brushLastY, tileY);
    		int y1 = tileY;//Math.max(brushLastY, tileY);
    		
//    		Log.info("@ @ to @ @", tileX, tileY, brushLastX, brushLastY);
    		brushLastX = tileX;
    		brushLastY = tileY;

    		int dx = x1-x0;
    		int dy = y1-y0;
    		if(Math.abs(dx) > Math.abs(dy)) {
    			if(dx == 0) return;
    			int k = x0 < x1 ? 1 : -1;
    			for (int xx = 0; xx < Math.abs(dx); xx++) {
    				int x = x0 + xx*k;
    				int y = y0 + (dy*xx/Math.abs(dx));
    				paintTile(Vars.world.tile(x, y), true);
				}
    		} else {
    			if(dy == 0) return;
    			int k = y0 < y1 ? 1 : -1;
    			for (int yy = 0; yy < Math.abs(dy); yy++) {
    				int x = x0 + (dx*yy/Math.abs(dy));
    				int y = y0 + yy*k;
    				paintTile(Vars.world.tile(x, y), true);
				}
    		}
			paintTile(Vars.world.tile(x1, y1), false);
    	});

		Events.on(GameOverEvent.class, e -> {
			brushOwner = null;
			commandBlocks.clear();
			lastkickTime.clear();
		});
		
		Events.on(WorldLoadEndEvent.class, e -> {
			commandBlocks.clear();
		});
		
		Events.on(PlayerLeave.class, e -> {
			if(e.player != null) TelegramBot.sendToAll("<b>" + e.player.plainName() + "</b> has left <i>(" + (Groups.player.size()-1) + " players)</i>");
			if(e.player == brushOwner) brushOwner = null;
		});
		
		Events.on(BuildSelectEvent.class, event -> { 
			Unit builder = event.builder;
			if(builder == null) return;
			BuildPlan buildPlan = builder.buildPlan();
			if(buildPlan == null) return;
			Block block = buildPlan.block;
			
			if(block == Blocks.door || block == Blocks.doorLarge || block == Blocks.blastDoor) {

				Tile door = event.tile;
				if(door == null) return;
				int center = Point2.pack(door.centerX(), door.centerY());
				for (int i = 0; i < doorsCoordinates.size(); i++) {
					int pos = doorsCoordinates.get(i);
					if(center == pos) {
						doorsCoordinates.remove(i);
						break;
					}
				}
				if(!event.breaking) {
					doorsCoordinates.add(Point2.pack(event.tile.centerX(), event.tile.centerY()));
				}
				if(event.tile != null)
					updateDoors(event.tile);
			}
		});

		/**
		 * Destroy (by explosion, or enemy and etc.)
		 */
    	Events.on(BlockDestroyEvent.class, event -> {
    		if(event.tile == null) return;
    		if(event.tile.block() == null) return;
    		Block block = event.tile.block();
    		
    		if(block == Blocks.door || block == Blocks.doorLarge || block == Blocks.blastDoor) {
    			updateDoors(event.tile);
    		}
    	});

		/**
		 * Info message about builder, that building thoriumReactor
		 */
		Events.on(BlockBuildBeginEvent.class, event -> {
			Unit builder = event.unit;
			if(builder == null) return;
			BuildPlan buildPlan = builder.buildPlan();
			if(buildPlan == null) return;
			Block block = buildPlan.block;
			
			if(!event.breaking) {
				if(block == Blocks.door || block == Blocks.doorLarge || block == Blocks.blastDoor) {
					if(doorsCoordinates.size() >= doorsCup) {
						event.tile.setNet(Blocks.air);
						Player player = builder.getPlayer();
						if(player != null) {
							builder.clearBuilding();
							if(builder.plans != null) builder.plans.clear();
							player.clearUnit();
							Log.info(player.plainName() + " exceeded the limit of doors");
							player.sendMessage("[red]Достигнут максимальный лимит дверей!");
							return;
						}
						return;
					}
				}
			}
		});
		
		Events.on(BlockBuildEndEvent.class, event -> {
			Unit builder = event.unit;
			if(builder == null) return;
			BuildPlan buildPlan = builder.buildPlan();
			if(buildPlan == null) return;

			if(builder.isPlayer()) {
				Player player = builder.getPlayer();
				if(player == null) return;
				if(player.admin()) {
					if(player != brushOwner) return;
					if(adminSandbox) {
						if(brushBlock != null || brushFloor != null || brushOverlay != null) {
							Tile tile = event.tile;
							if(brushBlock != null) {
								tile.setNet(brushBlock, player.team(), 0);
							}
							if(brushFloor != null) {
								if(brushOverlay != null) {
									tile.setFloorNet(brushFloor, brushOverlay);
								} else {
									tile.setFloorNet(brushFloor);
								}
							} else {
								if(brushOverlay != null) {
									tile.setFloorNet(tile.floor(), brushOverlay);
								}
							}
						}
					}
				}
			}
		});

		// "/brush" command
		Events.on(TapEvent.class, event -> {
			Player player = event.player;
			if(player == null) return;
			if(!player.admin()) return;
			if(player != brushOwner) return;
			if(event.tile == null) return;
			mods.getScripts().runConsole("var ttile = Vars.world.tile(" + event.tile.pos() + ")");
			if(blockInfo) {
				if(event.tile.build != null) player.sendMessage("[white]lastAccessed: [gold]" + event.tile.build.lastAccessed);
			}
		});
		
		Vars.netServer.admins.addActionFilter(action -> {
			if(action.type == ActionType.pickupBlock) {
				if(action.tile == null) return true;
				if(action.tile.block() == Blocks.thoriumReactor) return false;
			}
			if(action.type == ActionType.placeBlock) {
				if(action.tile == null) return true;
				if(action.block == Blocks.thoriumReactor) {
					if(action.player == null) return false;
	                var cores = action.player.team().cores();
	                for (int i = 0; i < cores.size; i++) {
	                	if(cores.get(i).dst2(action.tile) <= 500*Vars.tilesize*Vars.tilesize) {
	                		if(lastThoriumReactorPlayer != action.player) Call.sendMessage("[scarlet]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок [" + GameWork.colorToHex(action.player.color()) + "]" + action.player.name + " []строит реактор рядом с ядром (" + (int)(World.toTile(cores.get(i).dst(action.tile))) + " блоках от ядра)");
	                		lastThoriumReactorPlayer = action.player;
	                		return false;
	                	}
	                }
				}
			}
			return true;
		});
		// "/chatfilter" command
		Vars.netServer.admins.addChatFilter((player, text) -> {
			if(player != null && text != null) TelegramBot.sendToAll("<u><b>" + player.plainName() + "</b></u>: " + Strings.stripColors(text));
			if(chatFilter) {
				text = "[white]" + text + "[white]";
				char[] msg = text.toCharArray();

				StringBuilder result = new StringBuilder();

				int noobI = -1;
				StringBuilder noob = new StringBuilder();
				for (int i = 0; i < msg.length; i++) {
					char c = msg[i];
					char uc = Character.toUpperCase(c);
					if(noob.length() == 0 && (uc == 'N' || uc == 'Н')) {
						noob.append(c);
						noobI = i;
					} else if(uc == 'O' || uc == 'У' || uc == 'Y') {
						noob.append(c);
					} else if(uc == 'Б' || uc == 'B') {
						noob.append(c);
					} else {
						if(noob.length() > 2) {
							boolean isUpper = Character.isUpperCase(noob.charAt(0));
							char end = Character.toUpperCase(noob.charAt(noob.length()-1));
							if(end == 'B' || end == 'Б') {
								result.delete(noobI, noobI+noob.length());
								if(end == 'B') {
									result.append(isUpper ? "Pr" : "pr");
									for (int j = 0; j < noob.length()-3; j++) {
										result.append('o');
									}
								} else {
									result.append(isUpper ? "Пр" : "пр");
									for (int j = 0; j < noob.length()-2; j++) {
										result.append('о');
									}
								}
							}
						}
						noob.delete(0, noob.length());
					}
					result.append(msg[i]);
				}
				text = result.toString();
			}
			ExamplePlugin.dataCollect.messageEvent(player, text);
			return text;
		});
	}

	private void paintTile(@Nullable Tile tile, boolean saveCores) {
		if(tile == null) return;
		if(brushBlock != null) {
			if(!(saveCores && tile.block() instanceof CoreBlock)) {
				tile.setNet(brushBlock, brushOwner.team(), 0);
			}
		}
		if(brushFloor != null) {
			if(brushOverlay != null) {
				tile.setFloorNet(brushFloor, brushOverlay);
			} else {
				tile.setFloorNet(brushFloor, tile.overlay());
			}
		} else {
			if(brushOverlay != null) {
				tile.setFloorNet(tile.floor(), brushOverlay);
			}
		}		
	}

	private void updateDoors(Tile tile) {
		for (int i = 0; i < doorsCoordinates.size(); i++) {
			int pos = doorsCoordinates.get(i);
			Tile door = world.tile(pos);
			if(door == tile) continue;
			if((door.block() != Blocks.door && door.block() != Blocks.doorLarge && door.block() != Blocks.blastDoor) || door == null) {
				doorsCoordinates.remove(i);
				i--;
				if(i < 0) i = 0;
			} else {
				int center = Point2.pack(door.centerX(), door.centerY());
				if(center != pos) {
					doorsCoordinates.remove(i);
					i--;
					if(i < 0) i = 0;
				}
			}
		}		
	}
	
	public Seq<PlayerCommand> playerCommands = new Seq<PlayerCommand>();

	public void playerCommand(String text, String parms, String desc, CommandRunner<Player> run) {
		playerCommands.add(new PlayerCommand(text, parms, desc, run));
	}

	public void playerCommand(String text, String desc, CommandRunner<Player> run) {
		playerCommands.add(new PlayerCommand(text, "", desc, run));
	}

	public void adminCommand(String text, String parms, String desc, CommandRunner<Player> run) {
		playerCommands.add(new PlayerCommand(text, parms, desc, run).admin(true));
	}

	public void adminCommand(String text, String desc, CommandRunner<Player> run) {
		playerCommands.add(new PlayerCommand(text, "", desc, run).admin(true));
	}
	
	class PlayerCommand {
		
		private final String text, parms, desc;
		private boolean admin = false;
		private CommandRunner<Player> run;

		public PlayerCommand(String text, String parms, String desc, CommandRunner<Player> run) {
			this.text = text;
			this.parms = parms;
			this.desc = desc;
			this.run = run;
		}

		public PlayerCommand admin(boolean admin) {
			this.admin = admin;
			return this;
		}
		
		public boolean check(Player player) {
			if(!admin) return true;
			if(player.admin()) return true;
			var data = Admins.adminData(player.getInfo());
			if(data == null) return false;
			return data.has(text);
		}
		
		public CommandRunner<Player> run() {
			if(!admin) return (args, player) -> {
				if(TelegramBot.bot != null) TelegramBot.sendToAll("<b><u>" + player.plainName() + "</u></b>: <code>" + build(args) + "</code>");
				run.accept(args, player);
			};
			return (args, player) -> {
				if(!check(player)) return;
				if(TelegramBot.bot != null) TelegramBot.sendToAll("#helper <b><u>" + player.plainName() + "</u></b>: <code>" + build(args) + "</code>");
				run.accept(args, player);
			};
		}

		private String build(String[] args) {
			StringBuilder command = new StringBuilder("&#47;");
			command.append(text);
			for (int i = 0; i < args.length; i++) {
				command.append(' ');
				command.append(args[i] == null ? "null" : args[i]);
			}
			return command.toString();
		}
	}
	
//	int commandVotekick

	public void registerClientCommands(CommandHandler handler) {
		handler.removeCommand("help");
		handler.removeCommand("votekick");
		handler.removeCommand("vote");
//		handler.getCommandList().forEach(command -> {
//			if(command.text.equals("votekick")) {
//				command.description
//			}
//		});
		
		playerCommands.each(c -> handler.register(c.text, c.parms, c.desc, c.run()));

//		registerPlayersCommands(handler);
//		registerAdminCommands(handler);
	}


	public void registerAdminCommands() {
		adminCommand("admin", "<add/remove> <name>", "Добавить/удалить админа", (arg, player) -> {
			if(require(arg.length != 2 || !(arg[0].equals("add") || arg[0].equals("remove")), player, "[red]Second parameter must be either 'add' or 'remove'.")) return;
			boolean add = arg[0].equals("add");
			PlayerInfo target;
			Player playert = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[1])));
			if(playert != null) {
				target = playert.getInfo();
			} else {
				target = Vars.netServer.admins.getInfoOptional(arg[1]);
				playert = Groups.player.find(p -> p.getInfo() == target);
			}
			if(require(playert.uuid() == player.uuid(), player, "[red] Вы не можете изменить свой статус")) return;
			if(target != null){
				if(add) Vars.netServer.admins.adminPlayer(target.id, playert == null ? target.adminUsid : playert.usid());
				else Vars.netServer.admins.unAdminPlayer(target.id);
				if(playert != null) playert.admin(add);
				player.sendMessage("[gold]Изменен статус администратора игрока: [" + GameWork.colorToHex(playert.color) + "]" + Strings.stripColors(target.lastName));
			} else {
				player.sendMessage("[red]Игрока с таким именем или ID найти не удалось. При добавлении администратора по имени убедитесь, что он подключен к Сети; в противном случае используйте его UUID");
			}
			Vars.netServer.admins.save();
		});

		adminCommand("nextmap", "<название...>", "Устанавливает следущую карту", (arg, player) -> {
            Map res = maps.all().find(map -> map.plainName().replace('_', ' ').equalsIgnoreCase(Strings.stripColors(arg[0]).replace('_', ' ')));
            if(res == null && arg[0].startsWith("#")) {
				try {
					res = maps.all().get(Integer.parseInt(arg[0].substring(1))-1);
				} catch (Exception e) {
	                player.sendMessage("[red]" + e.getMessage());
				}
			}
            if(res != null){
                maps.setNextMapOverride(res);
                player.sendMessage("[white]Следующая карта установлена на [gold]" + res.plainName());
            }else{
                player.sendMessage("[red]Карта " +arg[0] + " не найдена");
            }
        });

		adminCommand("runwave", "Запускает волну", (arg, player) -> {
			Vars.logic.runWave();
			player.sendMessage("[gold]Готово!");
        });
		
		adminCommand("fillitems", "[item] [count]", "Заполните ядро предметами", (arg, player) -> {
			try {
				final Item serpuloItems[] = {
						Items.scrap, Items.copper, Items.lead, Items.graphite, Items.coal, Items.titanium, Items.thorium, Items.silicon, Items.plastanium,
						Items.phaseFabric, Items.surgeAlloy, Items.sporePod, Items.sand, Items.blastCompound, Items.pyratite, Items.metaglass
				};
				final Item erekirOnlyItems[] = {Items.beryllium, Items.tungsten, Items.oxide, Items.carbide, Items.fissileMatter, Items.dormantCyst};
				Work.localisateItemsNames();
				if(arg.length == 0) {
					StringBuilder ruNames = new StringBuilder("Русские названия предметов: ");
					for (int i = 0; i < serpuloItems.length; i++) {
						ruNames.append(GameWork.getColoredLocalizedItemName(serpuloItems[i]));
						ruNames.append(", ");
					}
					for (int i = 0; i < erekirOnlyItems.length; i++) {
						ruNames.append(GameWork.getColoredLocalizedItemName(erekirOnlyItems[i]));
						if(i + 1 < erekirOnlyItems.length) {
							ruNames.append(", ");
						}
					}
					player.sendMessage(ruNames.toString());
					return;
				}
				Item item = null;
				String itemname = arg[0].toLowerCase();
				for (int i = 0; i < serpuloItems.length; i++) {
					Item si = serpuloItems[i];
					if(itemname.equalsIgnoreCase(si.name) || itemname.equalsIgnoreCase(si.localizedName)) {
						item = si;
						break;
					}
				}
				if(item == null) {
					for (int i = 0; i < erekirOnlyItems.length; i++) {
						Item ei = erekirOnlyItems[i];
						if(itemname.equalsIgnoreCase(ei.name) || itemname.equalsIgnoreCase(ei.localizedName)) {
							item = ei;
							break;
						}
					}
				}
				if(item == null && itemname != null) {
					var items = Vars.content.items();
					for (int i = 0; i < items.size; i++) {
						if(items.get(i).emoji() == null) continue;
						if(!items.get(i).emoji().equals(itemname)) continue;
						item = items.get(i);
					}
				}
				if(item == null) {
					if(itemname.equalsIgnoreCase(Items.dormantCyst.name) || itemname.equalsIgnoreCase(Items.dormantCyst.localizedName)) {
						item = Items.dormantCyst;
					}
					if(itemname.equalsIgnoreCase(Items.fissileMatter.name) || itemname.equalsIgnoreCase(Items.fissileMatter.localizedName)) {
						item = Items.fissileMatter;
					}
				}
				if(item != null) {
					Team team = player.team();
					int count = arg.length > 1 ? Integer.parseInt(arg[1]) : 0;
					if(team.cores().size == 0) {
						player.sendMessage("[red]У Вашей команды игроков нет ядер");
						return;
					}
					team.cores().get(0).items.add(item, count);
					player.sendMessage("Добавлено " + "[gold]x" + count + " [orange]" + item.name);
				} else {
					player.sendMessage("Предмет не найден");
				}
			} catch (Exception e) {
				player.sendMessage(e.getMessage());
			}
		});
		
		adminCommand("chatfilter", "<on/off>", "Включить/выключить фильтр чата", (arg, player) -> {
			if(require(arg.length == 0, player, "[red]Недостаточно аргументов")) return;
			if(arg[0].equals("on")) {
				chatFilter = true;
				Core.settings.put(ExamplePlugin.PLUGIN_NAME + "-chat-filter", chatFilter);
				player.sendMessage("[green]Чат фильтр включен");
			}else if(arg[0].equals("off")) {
				chatFilter = false;
				Core.settings.put(ExamplePlugin.PLUGIN_NAME + "-chat-filter", chatFilter);
				player.sendMessage("[red]Чат фильтр выключен");
			}else {
				player.sendMessage("Неверный аргумент, используйте [gold]on/off");
				return;
			}
		});

		adminCommand("dct", "[time]", "Установить интервал (секунд/10) обновлений данных", (arg, player) -> {
			if(require(arg.length == 0, player, "Интервал обновлений: " + ExamplePlugin.dataCollect.getSleepTime() + " секунд/10")) return;
			if(arg.length == 1) {
				long count = 0;
				try {
					count = Long.parseLong(arg[0]);
				} catch (Exception e) {
					player.sendMessage("[red]Вводить можно только числа!");
				}
				count *= 1_00;

				if(count <= 0) {
					player.sendMessage("[red]Интервал не может быть меньше 1!");
				}
				ExamplePlugin.dataCollect.setSleepTime(count);
				player.sendMessage("Установлен интервал: " + count + " ms");
				return;
			}
		});

		adminCommand("event", "[id] [on/off/faston]", "Включить/выключить событие", (arg, player) -> {
			if(arg.length == 0) {
				StringBuilder msg = new StringBuilder("[red]Недостаточно аргументов.[white]\nID событий:");
				for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
					msg.append('\n');
					ServerEvent event = ServerEventsManager.getServerEvent(i);
					msg.append('[');
					msg.append(event.getColor());
					msg.append(']');
					msg.append(event.getCommandName());
				}
				player.sendMessage(msg.toString());
				return;
			}
			if(arg.length == 1) {
				for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
					ServerEvent event = ServerEventsManager.getServerEvent(i);
					if(arg[0].equals(event.getCommandName())) {
						player.sendMessage("Событие [" + event.getColor() + "]" + event.getName() + "[white] имеет значение: " + event.isRunning());
						return;
					}
				}
				player.sendMessage("[red]Событие не найдено, [gold]/event [red] для списка событий");
				return;
			}
			if(arg.length == 2) {
				boolean isOn = false;
				boolean isFast = false;
				if(arg[1].equals("on")) {
					isOn = true;
				}else if(arg[1].equals("off")) {
					isOn = false;
				}else if(arg[1].equals("faston")) {
					isOn = true;
					isFast = true;
				}else {
					player.sendMessage("Неверный аргумент, используйте [gold]on/off[]");
					return;
				}

				for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
					ServerEvent event = ServerEventsManager.getServerEvent(i);
					if(arg[0].equals(event.getCommandName())) {
						boolean isRunning = event.isRunning();
						if(isRunning && isOn) {
							player.sendMessage("[red]Событие уже запущено");
							return;
						}
						if(!isRunning && !isOn) {
							player.sendMessage("[red]Событие итак не запущено");
							return;
						}

						if(isOn) {
							if(isFast) {
								ExamplePlugin.eventsManager.fastRunEvent(event.getCommandName());
								player.sendMessage("[white]Событие резко запущено! [gold]/sync");
							} else {
								ExamplePlugin.eventsManager.runEvent(event.getCommandName());
								player.sendMessage("[green]Событие запущено!");
							}
						} else {
							ExamplePlugin.eventsManager.stopEvent(event.getCommandName());
							player.sendMessage("[red]Событие остановлено!");
						}

						return;
					}
				}
				player.sendMessage("[red]Событие не найдено, [gold]/event [red] для списка событий");
				return;
			}
		});

		adminCommand("team", "[player] [team]", "Установить команду для игрока", (arg, player) -> {
			if(arg.length < 1) {
				StringBuilder teams = new StringBuilder();
				for (int i = 0; i < Team.baseTeams.length; i++) {
					teams.append(Team.baseTeams[i].name);
					teams.append(", ");
				}
				for (int i = 0; i < Team.all.length; i++) {
					teams.append(Team.all[i].name);
					if(i != Team.all.length - 1) teams.append(", ");
				}
				player.sendMessage("Команды:\n" + teams.toString());
			}
			if(arg.length == 1) {
				Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[0])));
				if(targetPlayer == null) {
					player.sendMessage("[red]Игрок не найден");
					return;
				}
				player.sendMessage("Игрок состоить в команде: " +  targetPlayer.team().name);
				return;
			}
			if(arg.length == 2) {
				Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[0])));
				if(targetPlayer == null) {
					player.sendMessage("[red]Игрок не найден");
					return;
				}
				player.sendMessage("Игрок состоить в команде: " +  targetPlayer.team().name);

				Team team = null;
				String targetTeam = arg[1].toLowerCase();
				for (int i = 0; i < Team.baseTeams.length; i++) {
					if(Team.baseTeams[i].name.equals(targetTeam.toLowerCase())) {
						team = Team.baseTeams[i];
					}
				}
				for (int i = 0; i < Team.all.length; i++) {
					if(Team.all[i].name.equals(targetTeam.toLowerCase())) {
						team = Team.all[i];
					}
				}
				if(team == null) {
					player.sendMessage("[red]Команда не найдена");
				} else {
					targetPlayer.team(team);
					if(team.name.equals(Team.crux.name)) {
						Log.info("crux");
						targetPlayer.unit().healTime(.01f);
						targetPlayer.unit().healthMultiplier(100);
						targetPlayer.unit().maxHealth(1000f);
						targetPlayer.unit().apply(StatusEffects.invincible, Float.MAX_VALUE);
					}
//					if(team.name.equals(admin.name)) {
//						targetPlayer.unit().healTime(.01f);
//						targetPlayer.unit().healthMultiplier(100);
//						targetPlayer.unit().maxHealth(1000f);
//						targetPlayer.unit().hitSize(0);
//						targetPlayer.unit().apply(StatusEffects.invincible, Float.MAX_VALUE);

//						admin.rules().infiniteResources = true;
//						admin.rules().cheat = true;
//						admin.rules().infiniteAmmo = true;
//						admin.rules().blockDamageMultiplier = Float.MAX_VALUE;
//						admin.rules().blockHealthMultiplier = Float.MAX_VALUE;
//						admin.rules().buildSpeedMultiplier = 100;
//						admin.rules().unitDamageMultiplier = Float.MAX_VALUE;
//					}
					player.sendMessage("Игрок " + targetPlayer.name() + " отправлен в команду [#" + team.color + "]" + team.name);
					targetPlayer.sendMessage("Вы отправлены в команду [#" + team.color + "]" + team.name);
				}
				return;
			}
		});

		adminCommand("config", "[name] [set/add] [value...]", "Конфикурация сервера", (arg, player) -> {
			if(arg.length == 0){
				player.sendMessage("All config values:");
				for(Config c : Config.all){
					player.sendMessage("[gold]" + c.name + "[lightgray](" + c.description + ")[white]:\n> " + c.get() + "\n");
				}
				return;
			}
			Config c = Config.all.find(conf -> conf.name.equalsIgnoreCase(arg[0]));
			if(c != null){
				if(arg.length == 1) {
					player.sendMessage(c.name + " is currently " + c.get());
				}else if(arg.length > 2) {
					if(arg[2].equals("default")){
						c.set(c.defaultValue);
					}else if(c.isBool()){
						c.set(arg[2].equals("on") || arg[2].equals("true"));
					}else if(c.isNum()){
						try{
							c.set(Integer.parseInt(arg[2]));
						}catch(NumberFormatException e){
							player.sendMessage("[red]Not a valid number: " + arg[2]);
							return;
						}
					}else if(c.isString()) {
						if(arg.length > 2) {
							if(arg[1].equals("add")) {
								c.set(c.get().toString() + arg[2].replace("\\n", "\n"));
							} else if(arg[1].equals("set")) {
								c.set(arg[2].replace("\\n", "\n"));
							} else {
								player.sendMessage("[red]Only [gold]add/set");
								return;
							}
						} else {
							player.sendMessage("[red]Add [gold]add/set [red]attribute");
						}
					}

					player.sendMessage("[gold]" + c.name + "[gray] set to [white]" + c.get());
					Core.settings.forceSave();
				} else {
					player.sendMessage("[red]Need more attributes");
				}
			}else{
				player.sendMessage("[red]Unknown config: '" + arg[0] + "'. Run the command with no arguments to get a list of valid configs.");
			}
		});

		adminCommand("sandbox", "[on/off] [team]", "Бесконечные ресурсы", (arg, player) -> {
			if(require(arg.length == 0, player, "[gold]infiniteResources: [gray]" + Vars.state.rules.infiniteResources)) return;
			Team team = null;
			if(arg.length == 2) {
				String targetTeam = arg[1].toLowerCase();
				for (int i = 0; i < Team.baseTeams.length; i++) {
					if(Team.baseTeams[i].name.equals(targetTeam.toLowerCase())) {
						team = Team.baseTeams[i];
					}
				}
				for (int i = 0; i < Team.all.length; i++) {
					if(Team.all[i].name.equals(targetTeam.toLowerCase())) {
						team = Team.all[i];
					}
				}
			}
			if(arg[0].equals("on")) {
				if(team == null) {
					Vars.state.rules.infiniteResources = true;
					player.sendMessage("[green]Включено!");
				} else {
					team.rules().infiniteResources = true;
					player.sendMessage("[green]Включено для команды [#" + team.color + "]" + team.name);
				}
				Call.setRules(player.con, state.rules);
			}else if(arg[0].equals("off")) {
				if(team == null) {
					Vars.state.rules.infiniteResources = false;
					player.sendMessage("[red]Выключено!");
				} else {
					team.rules().infiniteResources = false;
					player.sendMessage("[red]Выключено для команды [#" + team.color + "]" + team.name);
				}
				Call.setRules(player.con, state.rules);
			} else {
				player.sendMessage("[red]Только on/off");
			}
		});

		adminCommand("unit", "[type] [t/c]", "Создает юнита, list для списка", (arg, player) -> {
			String unitType = UnitTypes.gamma.name;
			Field[] fields = UnitTypes.class.getFields();
			if(arg.length > 0) {
				if(arg[0].equals("list")) {
					StringBuilder unitTypes = new StringBuilder("Типы юнитов");
					for (int i = 0; i < fields.length; i++) {
						String name = fields[i].getName();
						if(name.equals(UnitTypes.block.name)) continue;

						unitTypes.append(fields[i].getName());
						if(i+1 != fields.length) {
							unitTypes.append(", ");
						}
					}
					player.sendMessage(unitTypes.toString());
					return;
				}
				unitType = arg[0];
			}
			try {
				for (int i = 0; i < fields.length; i++) {
					if(fields[i].getName().equals(unitType)) {
						UnitType ut = (UnitType) fields[i].get(UnitTypes.class);
						if(ut == null) continue;
						if(ut.name.equals(UnitTypes.block.name)) {
							continue;
						}
						Unit u = ut.spawn(player.team(), player.mouseX, player.mouseY);
						if(arg.length > 1) {
							if(arg[1].equals("true") || arg[1].equals("y") || arg[1].equals("t") || arg[1].equals("yes")) {
								player.unit(u);
							}
							if(arg[1].equals("c") || arg[1].equals("core")) {
								player.unit(u);
								u.spawnedByCore(true);
							}
						}
						player.sendMessage("Готово!"); 

						if(!net.client()){
							u.add();
						}
						return;
					}
				}
				player.sendMessage("[red]Юнит не найден [gold]/unit list");
			} catch (IllegalArgumentException | IllegalAccessException e) {
				player.sendMessage(e.getLocalizedMessage());
			}
		});

		adminCommand("unban", "<ip/ID/all>", "Completely unban a person by IP or ID.", (arg, player) -> {
			if(arg[0].equals("all")) {
				int count = 0;
				while (true) {
					if(netServer.admins.bannedIPs.size > 0) {
						netServer.admins.unbanPlayerIP(netServer.admins.bannedIPs.get(0));
						count++;
					} else {
						break;
					}
				}
				player.sendMessage("[gold]Снято банов: [lightgray]" + count);
			} else {
				if(netServer.admins.unbanPlayerIP(arg[0]) || netServer.admins.unbanPlayerID(arg[0])){
					player.sendMessage("[gold]Unbanned player: [white]" + arg[0]);
				}else{
					player.sendMessage("[red]That IP/ID is not banned!");
				}
			}
		});

		adminCommand("bans", "List all banned IPs and IDs.", (arg, player) -> {
			Seq<PlayerInfo> bans = netServer.admins.getBanned();

			if(bans.size == 0){
				player.sendMessage("No ID-banned players have been found.");
			}else{
				player.sendMessage("Banned players [ID]:");
				for(PlayerInfo info : bans){
					player.sendMessage(" " + info.id + " / Last known name: [gold]" + info.plainLastName());
				}
			}

			Seq<String> ipbans = netServer.admins.getBannedIPs();

			if(ipbans.size == 0){
				player.sendMessage("No IP-banned players have been found.");
			}else{
				player.sendMessage("Banned players [IP]:");
				for(String string : ipbans){
					PlayerInfo info = netServer.admins.findByIP(string);
					if(info != null){
						player.sendMessage(" " + string + "   / Last known name: [gold]" + info.plainLastName() +"[] / ID: " + info.id);
					}else{
						player.sendMessage(" " + string + "   (No known name or info)");
					}
				}
			}
		});

		adminCommand("reloadmaps", "Перезагрузить карты", (arg, player) -> {
			int beforeMaps = maps.all().size;
			maps.reload();
			if(maps.all().size > beforeMaps) {
				player.sendMessage("[gold]" + (maps.all().size - beforeMaps) + " новых карт было найдено");
			}else if(maps.all().size < beforeMaps) {
				player.sendMessage("[gold]" + (beforeMaps - maps.all().size) + " карт было удалено");
			}else{
				player.sendMessage("[gold]Карты перезагружены");
			}
		});

		adminCommand("js", "<script...>", "Запустить JS", (arg, player) -> {
			if(arg[0].startsWith("!")) {
				player.sendMessage("[gold]" + mods.getScripts().runConsole(arg[0].substring(1)));
				return;
			}
			try {
				arg[0] =  arg[0].replaceAll("=>", "->");
				player.sendMessage("[gold]" + mods.getScripts().runConsole(arg[0]));
			} catch (Exception e) {
				player.sendMessage("[red]" + e.getMessage());
			}
		});

		adminCommand("link", "<link> [player]", "Отправить ссылку всем/игроку", (arg, player) -> {
			if(arg.length == 1) {
				Call.openURI(arg[0]);
			} else if(arg.length == 2) {
				Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[1])));
				if(targetPlayer != null) {
					Call.openURI(targetPlayer.con, arg[0]);
					player.sendMessage("[gold]Готово!");
				} else {
					player.sendMessage("[red]Игрок не найден");
				}
			}
		});

		adminCommand("setdiscord", "<link>", "\ue80d Сервера", (arg, player) -> {
			if(arg.length != 1) return;
			discordLink = arg[0];
			Core.settings.put(ExamplePlugin.PLUGIN_NAME + "-discord-link", discordLink);
			player.sendMessage("[gold]\ue80d Готово!");
		});

		adminCommand("pardon", "<ID> [index]", "Прощает выбор игрока по ID и позволяет ему присоединиться снова.", (arg, player) -> {
			int index = 0;
			if(arg.length >= 2) {
				try {
					index = Integer.parseInt(arg[1]);
				} catch (Exception e) {
					player.sendMessage("[red]" + e.getMessage());
					return;
				}
			}
			Seq<PlayerInfo> infos = netServer.admins.findByName(arg[0]).toSeq();
			if(index < 0) index = 0;
			if(index >= infos.size) index = infos.size-1;
			if(index < 0) {
				player.sendMessage("[red]No ids");
				return;
			}
			PlayerInfo info = infos.get(index);

			if(info != null){
				info.lastKicked = 0;
				netServer.admins.kickedIPs.remove(info.lastIP);
				player.sendMessage("Pardoned player: " + info.plainLastName() + " [lightgray](of " + infos.size + " find)");
			}else{
				player.sendMessage("[red]That ID can't be found");
			}
		});

		adminCommand("doorscup", "[count]", "Устанавливает лимит дверей", (arg, player) -> {
			if(arg.length == 1) {
				try {
					int lastDoorsCup = doorsCup;
					doorsCup = Integer.parseInt(arg[0]);
					Core.settings.put(ExamplePlugin.PLUGIN_NAME + "-doors-cup", doorsCup);
					player.sendMessage("[gold]Лимит дверей изменен с " + lastDoorsCup + " на " + doorsCup);
				} catch (Exception e) {
					player.sendMessage(e.getMessage());
				}
			} else if(arg.length == 0) {
				player.sendMessage("[gold]Сейчас дверей: " + doorsCoordinates.size() + "/" + doorsCup);
			}
		});

		adminCommand("brush", "[none/block/floor/overlay/free/info] [block/none]", "Устанваливает кисточку", (arg, player) -> {
			brushOwner = player;
			if(arg.length == 1) {
				if(arg[0].equalsIgnoreCase("none")) {
					brushBlock = null;
					brushFloor = null;
					brushOverlay = null;
					adminSandbox = false;
					blockInfo = false;
					player.sendMessage("[gold]Кисть отчищена");
				} else if(arg[0].equalsIgnoreCase("block")) {
					if(brushBlock == null) player.sendMessage("[gold]К кисти не привязан блок");
					else player.sendMessage("[gold]К кисти привязан блок: [lightgray]" + brushBlock.name);
				} else if(arg[0].equalsIgnoreCase("floor")) {
					if(brushBlock == null) player.sendMessage("[gold]К кисти не привязана поверхность");
					else player.sendMessage("[gold]К кисти привязана поверхность: [lightgray]" + brushFloor.name);
				} else if(arg[0].equalsIgnoreCase("overlay")) {
					if(brushBlock == null) player.sendMessage("[gold]К кисти не привязано покрытие");
					else player.sendMessage("[gold]К кисти привязан блок: [lightgray]" + brushOverlay.name);;
				} else if(arg[0].equalsIgnoreCase("free")) {
					adminSandbox = true;
					player.sendMessage("[gold]готово!");
				} else if(arg[0].equalsIgnoreCase("info")) {
					blockInfo = true;
					player.sendMessage("[gold]готово!");
				} else {
					player.sendMessage("[red]На первом месте только аргумены [lightgray]none/block/floor/overlay");
				}
			} else if(arg.length == 2) {
				String blockname = arg[1];
				if(blockname.equals("core1")) blockname = "coreShard";
				if(blockname.equals("core2")) blockname = "coreFoundation";
				if(blockname.equals("core3")) blockname = "coreNucleus";
				if(blockname.equals("core4")) blockname = "coreBastion";
				if(blockname.equals("core5")) blockname = "coreCitadel";
				if(blockname.equals("core6")) blockname = "coreAcropolis";
				if(blockname.equals("power+")) blockname = "powerSource";
				if(blockname.equals("power-")) blockname = "powerVoid";
				if(blockname.equals("item+")) blockname = "itemSource";
				if(blockname.equals("item-")) blockname = "itemVoid";
				if(blockname.equals("liq+")) blockname = "liquidSource";
				if(blockname.equals("liq-")) blockname = "liquidVoid";

				if(arg[0].equalsIgnoreCase("block") || arg[0].equalsIgnoreCase("b")) {
					if(arg[1].equals("none")) {
						brushBlock = null;
						player.sendMessage("[gold]Блок отвязан");
						return;
					}
					try {
						Field field = Blocks.class.getField(blockname);
						Block block = (Block) field.get(null);
						brushBlock = block;
						player.sendMessage("[gold]Блок привязан!");
					} catch (NoSuchFieldException | SecurityException e) {
						brushBlock = GameWork.getBlockByEmoji(blockname);
						if(brushBlock == null) player.sendMessage("[red]Блок не найден");
					} catch (ClassCastException e2) {
						player.sendMessage("[red]Это не блок");
					} catch (IllegalArgumentException | IllegalAccessException e) {
						player.sendMessage("[red]Доступ к блоку заблокирован");
					}
				} else if(arg[0].equalsIgnoreCase("floor") || arg[0].equalsIgnoreCase("f")) {
					if(arg[1].equals("none")) {
						brushFloor = null;
						player.sendMessage("[gold]Поверхность отвязана");
						return;
					}
					try {
						Field field = Blocks.class.getField(blockname);
						Floor floor = (Floor) field.get(null);
						brushFloor = floor;
						player.sendMessage("[gold]Поверхность привязана!");
					} catch (NoSuchFieldException | SecurityException e) {
						try {
							brushFloor = (Floor) GameWork.getBlockByEmoji(blockname);
							if(brushBlock == null) player.sendMessage("[red]Поверхность не найдена");
						} catch (ClassCastException e2) {
							player.sendMessage("[red]Это не поверхность");
						}
						brushFloor = (Floor) GameWork.getBlockByEmoji(blockname);
						if(brushFloor == null) player.sendMessage("[red]Блок не найден");
					} catch (ClassCastException e2) {
						player.sendMessage("[red]Это не поверхность");
					} catch (IllegalArgumentException | IllegalAccessException e) {
						player.sendMessage("[red]Доступ к поверхности заблокирован");
					}
				} else if(arg[0].equalsIgnoreCase("overlay") || arg[0].equalsIgnoreCase("o")) {
					if(arg[1].equals("none")) {
						brushOverlay = null;
						player.sendMessage("[gold]Покрытие отвязано");
						return;
					}
					try {
						Field field = Blocks.class.getField(blockname);
						OverlayFloor overlay = (OverlayFloor) field.get(null);
						brushOverlay = overlay;
						player.sendMessage("[gold]Покрытие привязано!");
					} catch (NoSuchFieldException | SecurityException e) {
						try {
							brushOverlay = (OverlayFloor) GameWork.getBlockByEmoji(blockname);
							if(brushOverlay == null) player.sendMessage("[red]Покрытие не найдено");
						} catch (ClassCastException e2) {
							player.sendMessage("[red]Это не поверхность");
						}
					} catch (ClassCastException e2) {
						player.sendMessage("[red]Это не покрытие");
					} catch (IllegalArgumentException | IllegalAccessException e) {
						player.sendMessage("[red]Доступ к покрытию заблокирован");
					}
				}
			}
		});

		adminCommand("etrigger", "<trigger> [args...]", "Устанваливает кисточку", (args, player) -> {
			ExamplePlugin.eventsManager.trigger(player, args);
		});

		adminCommand("extrastar", "[add/remove] [uidd/name]", "", (args, player) -> {
			if(args.length == 0) {
				if(extraStarsUIDD.isEmpty()) {
					player.sendMessage("[gold]Нет игроков");
				} else {
					StringBuilder sb = new StringBuilder("[gold]Игроки с дополнительными звездами:[white]");
					for (int i = 0; i < extraStarsUIDD.size; i++) {
						String uidd = extraStarsUIDD.get(i);
						String name = netServer.admins.getInfo(uidd).lastName;
						sb.append("\n" + uidd + " (" + name + ")");
					}
					player.sendMessage(sb.toString());
				}
			} else if(args.length == 2) {
				Player playert = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(args[1])));
				if(playert != null) args[1] = playert.uuid();

				if(args[0].equalsIgnoreCase("add")) {
					if(!extraStarsUIDD.contains(args[1])) {
						PlayerInfo info = netServer.admins.getInfo(args[1]);
						if(info == null) {
							player.sendMessage("[red]Игрок не найден");
						} else {
							extraStarsUIDD.add(args[1]);
							player.sendMessage("[gold]Игрок []" + info.lastName + " [gold]добавлен");
						}
					} else {
						player.sendMessage("[red]Игрок уже есть");
					}
				} else if(args[0].equalsIgnoreCase("remove")) {
					if(extraStarsUIDD.contains(args[1])) {
						PlayerInfo info = netServer.admins.getInfo(args[1]);
						if(info == null) {
							player.sendMessage("[red]Игрок не найден");
						} else {
							extraStarsUIDD.remove(args[1]);
							player.sendMessage("[gold]Игрок []" + info.lastName + " [gold]убран");
						}
					} else {
						player.sendMessage("[red]UIDD не найден");
					}
				} else {
					player.sendMessage("[red]Только add/remove");
				}
			} else {
				player.sendMessage("[red]Неверные аргументы");
			}
		});

		adminCommand("achievements", "<load/save/path> [args...]", "Управление достижениями", (args, player) -> {
			if(args.length > 0) {
				if(args[0].equalsIgnoreCase("load")) {
					if(ExamplePlugin.achievementsManager.load()) {
						player.sendMessage("[gold]\ue81b Загружено!");
					} else {
						player.sendMessage("[red]\ue81b Файл не найден!");
					}
					return;
				}
				if(args[0].equalsIgnoreCase("save")) {
					ExamplePlugin.achievementsManager.save();
					player.sendMessage("[gold]\ue81b Сохранено!");
					return;
				}
				if(args[0].equalsIgnoreCase("path")) {
					if(args.length == 1) {
						player.sendMessage("[gold]\ue81b Место хранения: [lightgray]" + ExamplePlugin.achievementsManager.savePath.absolutePath());
						return;
					} else if(args.length == 2) {
						String path = args[1] + ".json";

						Fi fi = null;
						if(path.startsWith("#")) {
							fi = Core.files.absolute(path.substring(1));
						} else {
							fi = Core.files.local(path);
						}
						if(fi == null) {
							return;
						}
						ExamplePlugin.achievementsManager.savePath = fi;
						player.sendMessage("[gold]\ue81b Установлено место хранения: [lightgray]" + ExamplePlugin.achievementsManager.savePath.absolutePath());
					} else {
						player.sendMessage("[red]Используйте: /achievements <path> [new path] (# вначале для полного пути)");
					}
				}
			}
		});

		adminCommand("bot", "[add/remove/list/start/stop] [id/name] [token]", "Привязать/отвязать телеграм аккаунт", (arg, player) -> {
			if(arg.length == 3) {
				if(arg[0].equalsIgnoreCase("start")) {
					TelegramBot.run(arg[1], arg[2]);
					player.sendMessage("[gold]Бот запущен! [gray](" + arg[1] + " " + arg[2] + ")");
				} else if(arg[0].equalsIgnoreCase("stop")) {
					TelegramBot.stop();
					player.sendMessage("[gold]Бот остановлен!");
				}
				return;
			}
			if(arg.length == 2) {
				try {
					Long id = Long.parseLong(arg[1]);

					if(arg[0].equalsIgnoreCase("add")) {
						if(TelegramBot.followers.contains(id)) {
							player.sendMessage("[lightgray]" + id + "[gold] уже был добавлен!");
						} else {
							TelegramBot.followers.add(id);
							TelegramBot.saveFollowers();
							player.sendMessage("[lightgray]" + id + "[gold] добавлен!");
						}
					} else if(arg[0].equalsIgnoreCase("remove")) {
						player.sendMessage("[lightgray]" + id + "[gold] убран!");
						TelegramBot.followers.add(id);
						TelegramBot.saveFollowers();
					} else if(arg[0].equalsIgnoreCase("list")) {
						TelegramBot.followers.each(e -> {
							player.sendMessage("> " + e);
						});
						TelegramBot.followers.add(id);
					} else {
						player.sendMessage("[red]Неверный аргумент");
					}
				} catch (Exception e) {
					player.sendMessage("[red]" + e.getMessage());
				}
			}
		});
		

		adminCommand("helper", "<add/remove/uuid> [args...]", "Добавить помошника / разрешения", (args, player) -> {
			int code = 0;
			if(args[0].equalsIgnoreCase("add")) code = 1;
			else if(args[0].equalsIgnoreCase("remove")) code = -1;
			if(code == 0) {
				Player found = Groups.player.find(p -> p.uuid().equals(args[0]));
	            if(found == null) found = Groups.player.find(p -> p.name.equals(args[0]));
				if(require(found == null, player, "[red]UIID не найден")) return;
	            
				AdminData data = Admins.adminData(found.getInfo());
				if(require(data == null, player, "[red]Игрок не помошник")) return;
				if(args.length == 1) {
					if(data.permissionsCount() == 0) player.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [lightgray]<empty>", found.plainName()));
					else player.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [gold]@", found.plainName(), data.permissionsAsString(' ')));
					return;
				}
				String[] keys = args[1].split(" ");
				for (int i = 0; i < keys.length; i++) {
					String arg = keys[i];
					if(arg.length() < 2) continue;
					Log.info("Argumet: \"@\" with char \"@\" and value \"@\"", arg, arg.charAt(0), arg.substring(1));
					if(arg.charAt(0) == '+') data.add(arg.substring(1));
					else if(arg.charAt(0) == '-') data.remove(arg.substring(1));
				}
				if(data.permissionsCount() == 0) player.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [lightgray]<empty>", found.plainName()));
				else player.sendMessage(Strings.format("Игрок [gold]@[] имеет разрешения: [gold]@", found.plainName(), data.permissionsAsString(' ')));
				Admins.save();
				return;
			} else {
				if(require(args.length < 2, player, "[red]Слишком мало аргументов")) return;

	            Player found = Groups.player.find(p -> p.uuid().equals(args[1]));
	            if(found == null) found = Groups.player.find(p -> p.name.equals(args[1]));
				if(require(found == null, player, "[red]Игрок не найден")) return;
				if(code == 1) {
					if(Admins.add(found)) player.sendMessage("Игрок [gold]" + found.plainName() + "[] успешно добавлен!");
					else player.sendMessage("[red]Игрок [gold]" + found.plainName() + "[] уже добавлен!");
				} else if(code == -1) {
					if(Admins.remove(found)) player.sendMessage("Игрок [gold]" + found.plainName() + "[] успешно удален!");
					else player.sendMessage("[red]Игрок [gold]" + found.plainName() + "[] не найден!");
				}
				Admins.save();
			}
		});
	}
	
	public void registerPlayersCommands() {
		playerCommand("help", "[страница]", "Список всех команд", (args, player) -> {
			if(require(args.length > 0 && !Strings.canParseInt(args[0]), player, "[red]\"страница\" может быть только числом.")) return;
			final int commandsPerPage = 6;
			int count = 0;
			for (int i = 0; i < playerCommands.size; i++) {
				if(playerCommands.get(i).check(player)) count++;
			}
			int pages = Mathf.ceil((float) count / commandsPerPage);
			int page = (args.length > 0 ? Strings.parseInt(args[0]) : 1)-1;
			if(require(page >= pages || page < 0, player, "[red]\"страница\" должна быть числом между[orange] 1[] и[orange] " + pages + "[red]")) return;
			StringBuilder result = new StringBuilder();
			result.append(Strings.format("[orange]-- Страница команд[lightgray] @[gray]/[lightgray]@[orange] --\n\n", (page + 1), pages));
			int skip = commandsPerPage*page;
			for (int i = 0; i < playerCommands.size; i++) {
				if(!playerCommands.get(i).check(player)) continue;
				if(--skip >= 0) continue;
				if(-skip > commandsPerPage) break;
				PlayerCommand command = playerCommands.get(i);
				result.append("[orange] /").append(command.text).append("[white] ").append(command.parms).append("[lightgray] - ").append(command.desc + (command.admin ? " [red] Только для администраторов" : "")).append("\n");
			}
			player.sendMessage(result.toString());
		});
		
        ObjectMap<String, Timekeeper> cooldowns = new ObjectMap<>();
		playerCommand("votekick", "[игрок] [причина...]", "Проголосовать, чтобы кикнуть игрока по уважительной причине", (args, player) -> {
            if(require(!Config.enableVotekick.bool(), player, "[red]Голосование на этом сервере отключено")) return;
//            if(require(Groups.player.size() < 3, player, "[red]Для участия в голосовании требуется как минимум 3 игрока.")) return;
            if(require(player.isLocal(), player, "[red]Просто кикни их сам, если ты хост")) return;
            boolean permission = Admins.has(player, "votekick");
            if(require(currentlyKicking != null && !(permission && !player.admin), player, "[red]Голосование уже идет")) return;

            if(args.length == 0){
                StringBuilder builder = new StringBuilder();
                builder.append("[orange]Игроки для кика: \n");

                Groups.player.each(p -> !p.admin && p.con != null && p != player, p -> {
                    builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id()).append(")\n");
                });
                player.sendMessage(builder.toString());
            }else if(args.length == 1){
                player.sendMessage("[orange]Для кика игрока вам нужна веская причина. Укажите причину после имени игрока");
            }else{
                Player found;
                if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))){
                    int id = Strings.parseInt(args[0].substring(1));
                    found = Groups.player.find(p -> p.id() == id);
                }else{
                    found = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
                    if(found == null) found = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
                    if(found == null) found = Groups.player.find(p -> Strings.stripGlyphs(p.name).equalsIgnoreCase(Strings.stripGlyphs(args[0])));
                    if(found == null) found = Groups.player.find(p -> Strings.stripColors(p.name).equalsIgnoreCase(Strings.stripColors(args[0])));
                    if(found == null) found = Groups.player.find(p -> Strings.stripColors(Strings.stripGlyphs(p.name)).equalsIgnoreCase(Strings.stripColors(Strings.stripGlyphs(args[0]))));
                }
                if(found != null){
                    if(found == player){
                        player.sendMessage("[red]Ты не можешь голосовать за то, чтобы кикнуть себя");
                    }else if(found.admin){
                        player.sendMessage("[red]Хо-хо-хо, ты действительно ожидал, что сможешь выгнать администратора?");
                    }else if(Admins.has(found, "votekick")){
                        player.sendMessage("[red]Этот игрок защищен пластаном");
                    }else if(Admins.has(found, "whitelist")){
                        player.sendMessage("[red]Этот игрок защищен метастеклом");
                    }else if(found.isLocal()){
                        player.sendMessage("[red]Локальные игроки не могут быть выгнаны");
                    }else if(found.team() != player.team()){
                        player.sendMessage("[red]Кикать можно только игроков из вашей команды");
                    }else{
                    	if(permission) {
                    		kick(found, player.plainName(), args[1]);
                    	} else {
                            Timekeeper vtime = cooldowns.get(player.uuid(), () -> new Timekeeper(NetServer.voteCooldown));
                            if(!vtime.get()){
                                player.sendMessage("[red]Вы должны подождать " + NetServer.voteCooldown/60 + " минут между голосованиями");
                                return;
                            }
                            VoteSession session = new VoteSession(found);
                            TelegramBot.sendToAll("<b><u>" + player.plainName() + "</b></u> started voting for kicking <b><u>" + found.plainName() + "</b></u>");
                            TelegramBot.sendPlayerToAll(found);
                            session.vote(player, 1);
                            Call.sendMessage(Strings.format("[lightgray]Причина:[orange] @[lightgray].", args[1]));
                            vtime.reset();
                            currentlyKicking = session;
                    	}
                    }
                }else{
                    player.sendMessage("[red]Игрок[orange]'" + args[0] + "'[red] не найден.");
                }
            }
        });
		
		playerCommand("vote", "<y/n/c>", "Проголосуйте, чтобы выгнать текущего игрока", (arg, player) -> {
			if(require(currentlyKicking == null, player, "[red]Ни за кого не голосуют")) return;
			if((player.admin || Admins.has(player, "vote")) && arg[0].equalsIgnoreCase("c")){
				Call.sendMessage(Strings.format("[lightgray]Голосование отменено администратором[orange] @[lightgray].", player.name));
				currentlyKicking.task.cancel();
				currentlyKicking = null;
				return;
			}
			if(require(player.isLocal(), player, "[red]Локальные игроки не могут голосовать. Вместо этого кикните игрока сами")) return;

			int sign = switch(arg[0].toLowerCase()){
			case "y", "yes" -> 1;
			case "n", "no" -> -1;
			default -> 0;
			};
			//hosts can vote all they want
			if((currentlyKicking.voted.get(player.uuid(), 2) == sign || currentlyKicking.voted.get(Vars.netServer.admins.getInfo(player.uuid()).lastIP, 2) == sign)){
				player.sendMessage(Strings.format("[red]Вы уже проголосовали за @", arg[0].toLowerCase()));
				return;
			}
			if(require(currentlyKicking.target == player, player, "[red]Ты не можешь голосовать на за себя")) return;
			if(require(currentlyKicking.target.team() != player.team(), player, "[red]Ты не можешь голосовать на за другие команды")) return;
			if(require(sign == 0, player, "[red]Голосуйте либо \"y\" (да), либо \"n\" (нет)")) return;
			currentlyKicking.vote(player, sign);
        });
		
		playerCommand("maps", "[all/custom/default]", "Показывает список доступных карт. Отображает все карты по умолчанию", (arg, player) -> {
			String types = "all";
			if(arg.length == 0) types = maps.getShuffleMode().name();
			else types = arg[0];
			boolean custom  = types.equals("custom")  || types.equals("all");
			boolean def     = types.equals("default") || types.equals("all");
			if(!maps.all().isEmpty()) {
				Seq<Map> all = new Seq<>();
				if(custom) all.addAll(maps.customMaps());
				if(def) all.addAll(maps.defaultMaps());
				if(all.isEmpty()){
					player.sendMessage("Кастомные карт нет на этом сервере, используйте [gold]all []аргумет.");
				}else{
					player.sendMessage("[white]Maps:");
					int id = 0;
					for(Map map : maps.all()){
						id++;
						if((def && !map.custom) || (custom && map.custom)) {
							String mapName = Strings.stripColors(map.name());
							player.sendMessage(Strings.format("[gold]#@ @ [white]| @ [white](@x@, рекорд: @)", 
									id, map.custom ? "Кастомная" : "Дефолтная", mapName, map.width, map.height, map.getHightScore()));
						}
					}
				}
			} else {
				player.sendMessage("Карты не найдены");
			}
		});

		playerCommand("discord", "", "\ue80d Сервера", (arg, player) -> {
			if(discordLink == null) {
				player.sendMessage("[red]\ue80d Ссылка отсутствует");
			} else {
				if(discordLink.isEmpty()) {
					player.sendMessage("[red]\ue80d Ссылка отсутствует");
				} else {
					Call.openURI(player.con, discordLink);
				}
			}
		});
		
		playerCommand("mapinfo", "", "Показывает статистику ресурсов карты", (arg, player) -> {
			final Item itemDrops[] = new Item[] {
					Items.copper,
					Items.lead,
					Items.scrap,
					Items.sand,
					Items.coal,
					Items.titanium,
					Items.thorium
			};
			final Liquid liquidDrops[] = new Liquid[] {
					Liquids.water,
					Liquids.oil,
					Liquids.slag,
					Liquids.cryofluid
			};
			int counter[] = new int[itemDrops.length];
			int lcounter[] = new int[liquidDrops.length];
			int summaryCounter = 0;
			int typesCounter = 0;
			for(int x = 0; x < world.width(); x++){
				for(int y = 0; y < world.height(); y++) {
					if(world.tile(x, y).block() != Blocks.air) continue;
					Item floor = world.tile(x, y).floor().itemDrop;
					Item overlay = world.tile(x, y).overlay().itemDrop;
					Liquid lfloor = world.tile(x, y).floor().liquidDrop;
					Liquid loverlay = world.tile(x, y).overlay().liquidDrop;
					for (int i = 0; i < counter.length; i++) {
						if(itemDrops[i] == overlay || itemDrops[i] == floor) {
							if(counter[i] == 0) {
								typesCounter++;
							}
							counter[i]++;
							summaryCounter++;
						}
					}
					for (int i = 0; i < liquidDrops.length; i++) {
						if(liquidDrops[i] == loverlay || liquidDrops[i] == lfloor) {
							lcounter[i]++;
						}
					}
				}
			}
			StringBuilder worldInfo = new StringBuilder();
			if(summaryCounter == 0) return;
			worldInfo.append("Информация о карте:\n");
			worldInfo.append("[gold]Название: [lightgray]" + Vars.state.map.name() + "\n");
			worldInfo.append("[gold]Рекорд: [lightgray]" + Vars.state.map.getHightScore() + "\n");
			worldInfo.append("[white]Ресурсы:\n");
			for (int i = 0; i < counter.length; i++) {
				float cv = ((float)counter[i])*typesCounter/summaryCounter/3f;
				if(cv > 1/3f) cv = 1/3f;
				int percent = (int) Math.ceil(counter[i]*100d/summaryCounter);
				Color c = new Color(Color.HSBtoRGB(cv, 1, 1));
				worldInfo.append(oreBlocksEmoji[i]);
				worldInfo.append('[');
				worldInfo.append(String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()));
				worldInfo.append("]: ");
				if(counter[i] > 0) {
					worldInfo.append(counter[i]);
					worldInfo.append(" (");
					worldInfo.append(percent);
					worldInfo.append("%)");
				} else {
					worldInfo.append("-");
				}
				worldInfo.append("\n[white]");
			}

			worldInfo.append("Жидкости:");
			boolean isLFound = false;
			for (int i = 0; i < lcounter.length; i++) {
				if(lcounter[i] > 0) {
					worldInfo.append("\n[white]");
					worldInfo.append(liquidsEmoji[i]);
					worldInfo.append("[lightgray]: ");
					worldInfo.append(counter[i]);
					isLFound = true;
				}
			}
			if(!isLFound) {
				worldInfo.append(" [red]нет");
			}
			player.sendMessage(worldInfo.toString());
		});

		playerCommand("skipmap", "Начать голосование за пропуск карты", (arg, player) -> {
			if(require(player.team() == Team.derelict, player, "[red]Вы не можете использовать эту команду")) return;
			if(require(currentlyMapSkipping[0] != null, player, "[red]Голосование уже идет: [gold]/smvote <y/n>")) return;
			SkipmapVoteSession session = new SkipmapVoteSession(currentlyMapSkipping);
			session.vote(player, 1);
			currentlyMapSkipping[0] = session;
		});

		playerCommand("smvote", "<y/n>", "Проголосовать за/протов пропуск карты", (arg, player) -> {
			if(require(player.team() == Team.derelict, player, "[red]Вы не можете использовать эту команду")) return;
			if(require(currentlyMapSkipping[0] == null, player, "[red]Нет открытого голосования")) return;
			if(require(player.isLocal(), player, "[red]Локальные игроки не могут голосовать")) return;
			if(require((currentlyMapSkipping[0].voted.contains(player.uuid()) || currentlyMapSkipping[0].voted.contains(Vars.netServer.admins.getInfo(player.uuid()).lastIP)), player, "[red]Ты уже проголосовал. Молчи!")) return;
			String voteSign = arg[0].toLowerCase();
			int sign = 0;
			if(voteSign.equals("y")) sign = +1;
			if(voteSign.equals("n")) sign = -1;
			if(require(sign == 0, player, "[red]Голосуйте либо \"y\" (да), либо \"n\" (нет)")) return;
			currentlyMapSkipping[0].vote(player, sign);
		});

		playerCommand("pluginfo", "info about pluging", (arg, player) -> {
			player.sendMessage(""
					+ "[green] Agzam's plugin " + ExamplePlugin.VERSION + "\n"
					+ "[gray]========================================================\n"
					+ "[white] Added [royal]skip map[white] commands\n"
					+ "[white] Added protection from [violet]thorium reactors[white]\n"
					+ "[white] Added map list command\n"
					+ "[white] Added fill items by type to core command\n"
					+ "[white] Added Map recourses statistic command\n"
					+ "[white] and more other\n"
					+ "[gray] Download: github.com/Agzam4/Mindustry-plugin");
		});
	}

	public void kick(Player found, String name, String reason) {
		int minutes = 5;
		Integer last = lastkickTime.get(found.uuid());
		if(last != null) minutes = last;
		minutes = Math.min(minutes, 60);
		lastkickTime.put(found.uuid(), minutes*2);
        TelegramBot.sendToAll(Strings.format("Выдан бан на <b>@</b> минут\nПричина: <i>@</i>\nБан выдал: <i>@</i>", minutes, reason, name));
        TelegramBot.sendPlayerToAll(found);
		found.kick(Strings.format("Вы были забанены на [red]@[] минут\nПричина: [orange]@[white]\nБан выдал: [orange]@[]\nОбжаловать: @", minutes, reason, name, discordLink), minutes * 60 * 1000);
		if(discordLink != null) {
			if(!discordLink.isEmpty()) Call.openURI(found.con, discordLink);
		}
		Call.sendMessage(Strings.format("[white]Игрок [orange]@[white] забанен на [orange]@[] минут [lightgray](причина: @)", found.plainName(), minutes, reason));
	}

	private boolean require(boolean b, Player player, String string) {
		if(b) player.sendMessage(string);
		return b;
	}

	public class SkipmapVoteSession {

		float voteDuration = 3 * 60;
		ObjectSet<String> voted = new ObjectSet<>();
		SkipmapVoteSession[] map;
		Timer.Task task;
		int votes;

		int votesRequiredSkipmap;

		public SkipmapVoteSession(SkipmapVoteSession[] map){
			this.map = map;
			votesRequiredSkipmap = votesRequiredSkipmap();
			this.task = Timer.schedule(() -> {
				if(!checkPass()){
					Call.sendMessage("[lightgray]Голосование закончилось. Недостаточно голосов, чтобы пропустить карту");
					map[0] = null;
					task.cancel();
				}
			}, voteDuration);
		}

		void vote(Player player, int d){
			votes += d;
			voted.addAll(player.uuid()); // FIXME: , Vars.netServer.admins.getInfo(player.uuid()).lastIP
			Call.sendMessage(Strings.format("[" + GameWork.colorToHex(player.color) + "]@[lightgray] проголосовал " + (d > 0 ? "[green]за" : "[red]против") + "[] пропуска карты[accent] (@/@)\n[lightgray]Напишите[orange] /smvote <y/n>[], чтобы проголосовать [green]за[]/[red]против",
					player.name, votes, votesRequiredSkipmap));
			checkPass();
		}

		boolean checkPass(){
			if(votes >= votesRequiredSkipmap) {
				Call.sendMessage("[gold]Голосование закончилось. Карта успешно пропущена!");
				Events.fire(new GameOverEvent(Team.derelict));
				map[0] = null;
				task.cancel();
				return true;
			}
			return false;
		}

		void update() {
			votesRequiredSkipmap = votesRequiredSkipmap();
		}
	}
	
	 public class VoteSession {
	        Player target;
	        ObjectIntMap<String> voted = new ObjectIntMap<>();
	        Timer.Task task;
	        int votes;

	        public VoteSession(Player target){
	            this.target = target;
	            this.task = Timer.schedule(() -> {
	                if(!checkPass()){
	                    Call.sendMessage(Strings.format("[lightgray]Голосование провалено. Недостаточно голосов, чтобы кикнуть [orange] @[lightgray].", target.name));
	                    TelegramBot.sendToAll("Голосование провалено");
		                currentlyKicking = null;
	                    task.cancel();
	                }
	            }, NetServer.voteDuration);
	        }

	        public void vote(Player player, int d){
	            int lastVote = voted.get(player.uuid(), 0) | voted.get(Vars.netServer.admins.getInfo(player.uuid()).lastIP, 0);
	            votes -= lastVote;

	            votes += d;
	            voted.put(player.uuid(), d);
	            voted.put(Vars.netServer.admins.getInfo(player.uuid()).lastIP, d);

	            Call.sendMessage(Strings.format("[lightgray]@[lightgray] проголосовал за кик[orange] @[lightgray].[accent] (@/@)\n[lightgray]Напиши[orange] /vote <y/n>[] чтобы проголосовать",
	                player.name, target.name, votes, votesRequired()));

	            checkPass();
	        }

	        public boolean checkPass(){
	            if(votes >= votesRequired()){
	                Call.sendMessage(Strings.format("[orange]Голосование принято. [red] @[orange] забанен на @ минут", target.name, (NetServer.kickDuration / 60)));
	                TelegramBot.sendToAll(Strings.format("Голосование принято. <b><u>@</u></b> забанен на @ минут", target.name, (NetServer.kickDuration / 60)));
	                Groups.player.each(p -> p.uuid().equals(target.uuid()), p -> p.kick(KickReason.vote, NetServer.kickDuration * 1000));
	                currentlyKicking = null;
	                task.cancel();
	                return true;
	            }
	            return false;
	        }

	        public int votesRequired(){
	            return 2 + (Groups.player.size() > 4 ? 1 : 0);
	        }
	    }

	public void stopSkipmapVoteSession() {
		for (int i = 0; i < currentlyMapSkipping.length; i++) {
			if(currentlyMapSkipping[i] == null) continue;
			currentlyMapSkipping[i].task.cancel();
			currentlyMapSkipping[i].votes = Integer.MIN_VALUE;
			currentlyMapSkipping[i] = null;
		}
	}

	public int votesRequiredSkipmap(){
		if(Groups.player.size() == 1) return 1;
		if(Groups.player.size() == 2) return 2;
		return (int) Math.ceil(Groups.player.size()*0.45);
	}

	public void clearDoors() {
		doorsCoordinates.clear();
	}

	public static Sound sound(int id) {
		if(id < 0) return null;
		if(id >= sounds.size) return null;
		return sounds.get(id);
	}
	
}
