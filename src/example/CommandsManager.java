package example;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer;
import arc.util.CommandHandler.Command;
import example.events.ServerEvent;
import example.events.ServerEventsManager;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.game.EventType.BlockBuildBeginEvent;
import mindustry.game.EventType.BlockDestroyEvent;
import mindustry.game.EventType.BuildSelectEvent;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerBanEvent;
import mindustry.game.EventType.PlayerIpBanEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.maps.Map;
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

import static example.Emoji.*;
import static mindustry.Vars.*;

public class CommandsManager {

	public static ArrayList<String> adminCommands;

	public static SkipmapVoteSession[] currentlyMapSkipping = {null};
	public static ArrayList<String> extraStarsUIDD;
	public static boolean chatFilter;
	public static String discordLink = "";
	public static int doorsCup = 100;

	private static Team admin;
	private static ArrayList<Integer> doorsCoordinates;

	private String brushOwner = "";
	private Block brushBlock;
	private Floor brushFloor;
	private OverlayFloor brushOverlay;

	private boolean isServerAttackModeEnabled;
	private Seq<String> serverAttackModeRegex;

	public void init() {
		discordLink = Core.settings.getString(ExamplePlugin.PLUGIN_NAME + "-discord-link", null);
		doorsCup = Core.settings.getInt(ExamplePlugin.PLUGIN_NAME + "-doors-cup", Integer.MAX_VALUE);
		discordLink = Core.settings.getString(ExamplePlugin.PLUGIN_NAME + "-discord-link", null);
		chatFilter = Core.settings.getBool(ExamplePlugin.PLUGIN_NAME + "-chat-filter", false);
		isServerAttackModeEnabled = false;
		serverAttackModeRegex = new Seq<String>();
		extraStarsUIDD = new ArrayList<>();

		admin = Team.all[10];
		admin.name = "admin";

		doorsCoordinates = new ArrayList<>();

		adminCommands = new ArrayList<>();
		adminCommands.add("fillitems");
		adminCommands.add("admin");
		adminCommands.add("chatfilter");
		adminCommands.add("dct");
		adminCommands.add("event");
		adminCommands.add("team");
		adminCommands.add("sandbox");
		adminCommands.add("unit");
		adminCommands.add("bans");
		adminCommands.add("unban");
		adminCommands.add("m");
		adminCommands.add("js");
		adminCommands.add("link");
		adminCommands.add("pardon");
		adminCommands.add("doorscup");
		adminCommands.add("brush");
		adminCommands.add("etrigger");
		adminCommands.add("extrastar");
		adminCommands.add("achievements");
		adminCommands.add("whitelist");
		adminCommands.add("wlsize");
		adminCommands.add("attackmode");

		Events.on(PlayerJoin.class, e -> {
			if(e.player == null) return;
			checkPlayer(e.player);
		});
		
		Events.on(PlayerBanEvent.class, e -> {
			if(extraStarsUIDD.contains(e.uuid)) {
				netServer.admins.unbanPlayerID(e.uuid);
			}
		});

		Events.on(PlayerIpBanEvent.class, e -> {
			Seq<PlayerInfo> admins = Vars.netServer.admins.getAdmins();
			for (int i = 0; i < admins.size; i++) {
				if(admins.get(i).ips.contains(e.ip)) {
					netServer.admins.unbanPlayerIP(e.ip);
					return;
				}
			}
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

		// "/brush" command
		Events.on(TapEvent.class, event -> {
			Player player = event.player;
			if(player == null) return;

			if(player.admin()) {
				if(!player.name.equals(brushOwner)) return;
				if(event.tile == null) return;
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
		});

		// "/chatfilter" command
		Vars.netServer.admins.addChatFilter((player, text) -> {
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

	private void checkPlayer(Player player) {
		if(player.admin) return;
		if(!isServerAttackModeEnabled) return;
		String name = player.plainName().replaceAll(" ", "_");
		for (int i = 0; i < serverAttackModeRegex.size; i++) {
			if(Pattern.compile(serverAttackModeRegex.get(i)).matcher(name).find()) {
				player.kick(KickReason.serverClose, (5 * 60) *1000); // 5 minutes
				Log.info(player.uuid() + " (" + name + ") was banned on 5 min by " + serverAttackModeRegex.get(i) + " reg");
				return;
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

	public void registerClientCommands(CommandHandler handler) {
		handler.removeCommand("help");

		handler.<Player>register("help", "[страница]", "Список всех команд", (args, player) -> {
			boolean isAdmin = player.admin();
			int coummandsCount = handler.getCommandList().size;
			if(!isAdmin) coummandsCount -= adminCommands.size();

			if(args.length > 0 && !Strings.canParseInt(args[0])){
				player.sendMessage("[scarlet]\"страница\" может быть только числом.");
				return;
			}
			int commandsPerPage = 6;
			int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
			int pages = Mathf.ceil((float)coummandsCount / commandsPerPage);

			page--;

			if(page >= pages || page < 0){
				player.sendMessage("[scarlet]\"страница\" должна быть числом между[orange] 1[] и[orange] " + pages + "[scarlet].");
				return;
			}

			StringBuilder result = new StringBuilder();
			result.append(Strings.format("[orange]-- Страница команд[lightgray] @[gray]/[lightgray]@[orange] --\n\n", (page + 1), pages));

			for(int i = commandsPerPage * page; i < Math.min(commandsPerPage * (page + 1), handler.getCommandList().size); i++){
				Command command = handler.getCommandList().get(i);
				boolean isAdminCommand = adminCommands.indexOf(command.text) != -1;
				if(isAdminCommand && !isAdmin) continue;
				result.append("[orange] /").append(command.text).append("[white] ").append(command.paramText).append("[lightgray] - ").append(command.description + (isAdminCommand ? " [red] Только для администраторов" : "")).append("\n");
			}
			player.sendMessage(result.toString());
		});

		registerPlayersCommands(handler);
		registerAdminCommands(handler);
	}


	public void registerAdminCommands(CommandHandler handler) {

		handler.<Player>register("admin", "[add/remove] [name]", "Добавить/удалить админа", (arg, player) -> {
			if(player.admin()) {
				if(arg.length != 2 || !(arg[0].equals("add") || arg[0].equals("remove"))){
					player.sendMessage("[red]Second parameter must be either 'add' or 'remove'.");
					return;
				}

				boolean add = arg[0].equals("add");

				PlayerInfo target;
				Player playert = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[1])));
				if(playert != null){
					target = playert.getInfo();
				}else{
					target = Vars.netServer.admins.getInfoOptional(arg[1]);
					playert = Groups.player.find(p -> p.getInfo() == target);
				}

				if(playert.uuid() == player.uuid()) {
					player.sendMessage("[red] Вы не можете изменить свой статус");
					return;
				}

				if(target != null){
					if(add){
						Vars.netServer.admins.adminPlayer(target.id, playert == null ? target.adminUsid : playert.usid());
					}else{
						Vars.netServer.admins.unAdminPlayer(target.id);
					}
					if(playert != null) playert.admin = add;
					player.sendMessage("[gold]Изменен статус администратора игрока: [" + GameWork.colorToHex(playert.color) + "]" + Strings.stripColors(target.lastName));
				}else{
					player.sendMessage("[red]Игрока с таким именем или ID найти не удалось. При добавлении администратора по имени убедитесь, что он подключен к Сети; в противном случае используйте его UUID");
				}
				Vars.netServer.admins.save();
			}
		});

		handler.<Player>register("fillitems", "[item] [count]", "Заполните ядро предметами", (arg, player) -> {
			if(player.admin()) {
				try {
					final Item serpuloItems[] = {
							Items.scrap, Items.copper, Items.lead, Items.graphite, Items.coal, Items.titanium, Items.thorium, Items.silicon, Items.plastanium,
							Items.phaseFabric, Items.surgeAlloy, Items.sporePod, Items.sand, Items.blastCompound, Items.pyratite, Items.metaglass
					};

					final Item erekirOnlyItems[] = {
							Items.beryllium, Items.tungsten, Items.oxide, Items.carbide, Items.fissileMatter, Items.dormantCyst
					};

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
					if(item == null) {
						if(itemname.equals("\uf82a")) 	item = Items.blastCompound;
						else if(itemname.equals("\uf833")) 	item = Items.coal;
						else if(itemname.equals("\uf838")) 	item = Items.copper;
						else if(itemname.equals("\uf835")) 	item = Items.graphite;
						else if(itemname.equals("\uf837")) 	item = Items.lead;
						else if(itemname.equals("\uf836")) 	item = Items.metaglass;
						else if(itemname.equals("\uf82d")) 	item = Items.phaseFabric;
						else if(itemname.equals("\uf82e")) 	item = Items.plastanium;
						else if(itemname.equals("\uf829")) 	item = Items.pyratite;
						else if(itemname.equals("\uf834")) 	item = Items.sand;
						else if(itemname.equals("\uf830")) 	item = Items.scrap;
						else if(itemname.equals("\uf82f")) 	item = Items.silicon;
						else if(itemname.equals("\uf82b")) 	item = Items.sporePod;
						else if(itemname.equals("\uf82c")) 	item = Items.surgeAlloy;
						else if(itemname.equals("\uf831")) 	item = Items.thorium;
						else if(itemname.equals("\uf832")) 	item = Items.titanium;
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
			}
		});

		handler.<Player>register("chatfilter", "[on/off]", "Включить/выключить фильтр чата", (arg, player) -> {
			if(player.admin()) {
				if(arg.length == 0) {
					player.sendMessage("Недостаточно аргументов");
					return;
				}
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
			}
		});

		handler.<Player>register("dct", "[time]", "Установить интервал (секунд/10) обновлений данных", (arg, player) -> {
			if(player.admin()) {
				if(arg.length == 0) {
					player.sendMessage("Интервал обновлений: " +  ExamplePlugin.dataCollect.getSleepTime() + " секунд/10");
					return;
				}
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
			} else {
				player.sendMessage("[red]Команда только для администраторов");
			}
		});

		handler.<Player>register("event", "[id] [on/off/faston]", "Включить/выключить событие", (arg, player) -> {
			if(player.admin()) {
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
			} else {
				player.sendMessage("[red]Команда только для администраторов");
			}
		});

		handler.<Player>register("team", "[player] [team]", "Установить команду для игрока", (arg, player) -> {
			if(player.admin()) {
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
						if(team.name.equals(admin.name)) {
							targetPlayer.unit().healTime(.01f);
							targetPlayer.unit().healthMultiplier(100);
							targetPlayer.unit().maxHealth(1000f);
							targetPlayer.unit().hitSize(0);
							targetPlayer.unit().apply(StatusEffects.invincible, Float.MAX_VALUE);

							admin.rules().infiniteResources = true;
							admin.rules().cheat = true;
							admin.rules().infiniteAmmo = true;
							admin.rules().blockDamageMultiplier = Float.MAX_VALUE;
							admin.rules().blockHealthMultiplier = Float.MAX_VALUE;
							admin.rules().buildSpeedMultiplier = 100;
							admin.rules().unitDamageMultiplier = Float.MAX_VALUE;
						}
						player.sendMessage("Игрок " + targetPlayer.name() + " отправлен в команду [#" + team.color + "]" + team.name);
						targetPlayer.sendMessage("Вы отправлены в команду [#" + team.color + "]" + team.name);
					}

					return;
				}
			} else {
				player.sendMessage("[red]Команда только для администраторов");
			}
		});

		handler.<Player>register("config", "[name] [set/add] [value...]", "Конфикурация сервера", (arg, player) -> {
			if(player.admin()) {
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
			} else {
				player.sendMessage("[red]Команда только для администраторов");
			}
		});

		handler.<Player>register("sandbox", "[on/off] [team]", "Бесконечные ресурсы", (arg, player) -> {
			if(player.admin()) {
				if(arg.length == 0) {
					player.sendMessage("[gold]infiniteResources: [gray]" + Vars.state.rules.infiniteResources);

				} else {
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
				}
			} else {
				player.sendMessage("[red]Команда только для администраторов");
			}
		});

		handler.<Player>register("unit", "[type] [inPlayer]", "Создает юнита, list для списка", (arg, player) -> {
			if(player.admin()) {
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
					e.printStackTrace();
					player.sendMessage(e.getLocalizedMessage());
				}
			}
		});

		handler.<Player>register("unban", "<ip/ID/all>", "Completely unban a person by IP or ID.", (arg, player) -> {
			if(player.admin()) {
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
			}
		});

		handler.<Player>register("bans", "List all banned IPs and IDs.", (arg, player) -> {
			if(player.admin()) {
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
			}
		});

		handler.<Player>register("reloadmaps", "Перезагрузить карты", (arg, player) -> {
			if(player.admin()) {
				int beforeMaps = maps.all().size;
				maps.reload();
				if(maps.all().size > beforeMaps) {
					player.sendMessage("[gold]" + (maps.all().size - beforeMaps) + " новых карт было найдено");
				}else if(maps.all().size < beforeMaps) {
					player.sendMessage("[gold]" + (beforeMaps - maps.all().size) + " карт было удалено");
				}else{
					player.sendMessage("[gold]Карты перезагружены");
				}
			}
		});

		handler.<Player>register("js", "<script...>", "Запустить JS", (arg, player) -> {
			if(player.admin()) {
				player.sendMessage("[gold]" + mods.getScripts().runConsole(arg[0]));
			}
		});

		handler.<Player>register("link", "<link> [player]", "Отправить ссылку всем/игроку", (arg, player) -> {
			if(player.admin()) {
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
			}
		});

		handler.<Player>register("setdiscord", "[link]", "\ue80d Сервера", (arg, player) -> {
			if(player.admin()) {
				if(arg.length == 1) {
					discordLink = arg[0];
					Core.settings.put(ExamplePlugin.PLUGIN_NAME + "-discord-link", discordLink);
					player.sendMessage("[gold]\ue80d Готово!");
				}
			}
		});

		handler.<Player>register("pardon", "<ID> [index]", "Прощает выбор игрока по ID и позволяет ему присоединиться снова.", (arg, player) -> {
			if(player.admin()) {

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
				PlayerInfo info = infos.get(index);

				if(info != null){
					info.lastKicked = 0;
					netServer.admins.kickedIPs.remove(info.lastIP);
					player.sendMessage("Pardoned player: " + info.plainLastName() + " [lightfray](of " + infos.size + " find)");
				}else{
					player.sendMessage("[red]That ID can't be found");
				}
			}
		});

		handler.<Player>register("doorscup", "[count]", "Устанавливает лимит дверей", (arg, player) -> {
			if(player.admin()) {
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
			}
		});

		handler.<Player>register("brush", "[none/block/floor/overlay] [block/none]", "Устанваливает кисточку", (arg, player) -> {
			if(player.admin()) {
				brushOwner = player.name;
				if(arg.length == 1) {
					if(arg[0].equalsIgnoreCase("none")) {
						brushBlock = null;
						brushFloor = null;
						brushOverlay = null;
						player.sendMessage("[gold]Кисть отчищена");
					} else if(arg[0].equalsIgnoreCase("block")) {
						if(brushBlock == null) player.sendMessage("[gold]К кисти не привязан блок");
						else player.sendMessage("[gold]К кисти привязан блок: [lightgray]" + brushBlock.name);
					} else if(arg[0].equalsIgnoreCase("floor")) {
						if(brushBlock == null) player.sendMessage("[gold]К кисти не привязана поверхность");
						else player.sendMessage("[gold]К кисти привязана поверхность: [lightgray]" + brushFloor.name);
					} else if(arg[0].equalsIgnoreCase("overlay")) {
						if(brushBlock == null) player.sendMessage("[gold]К кисти не привязано покрытие");
						else player.sendMessage("[gold]К кисти привязан блок: [lightgray]" + brushOverlay.name);
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
							player.sendMessage("[red]Блок не найден");
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
							player.sendMessage("[red]Поверхность не найдена");
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
							player.sendMessage("[red]Покрытие не найдено");
						} catch (ClassCastException e2) {
							player.sendMessage("[red]Это не покрытие");
						} catch (IllegalArgumentException | IllegalAccessException e) {
							player.sendMessage("[red]Доступ к покрытию заблокирован");
						}
					}
				}
			}
		});

		handler.<Player>register("etrigger", "<trigger> [args...]", "Устанваливает кисточку", (args, player) -> {
			if(player.admin()) {
				ExamplePlugin.eventsManager.trigger(player, args);
			} else {
				player.sendMessage("[red]Команда только для администраторов");
			}
		});

		handler.<Player>register("extrastar", "[add/remove] [uidd/name]", "", (args, player) -> {
			if(player.admin()) {
				if(args.length == 0) {
					if(extraStarsUIDD.isEmpty()) {
						player.sendMessage("[gold]Нет игроков");
					} else {
						StringBuilder sb = new StringBuilder("[gold]Игроки с дополнительными звездами:[white]");
						for (int i = 0; i < extraStarsUIDD.size(); i++) {
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
			}
		});
		
		handler.<Player>register("attack", "<on/off/add/remove/list> [args...]", "Настройка автокика во время атаки", (args, player) -> {
			if(player.admin()) {
				if(args.length == 1) {
					if(args[0].equalsIgnoreCase("on")) {
						player.sendMessage("[green]Режим атаки включен!");
						isServerAttackModeEnabled = true;
						for (int i = 0; i < Groups.player.size(); i++) {
							checkPlayer(Groups.player.index(i));
						}
						return;
					}
					if(args[0].equalsIgnoreCase("off")) {
						player.sendMessage("[red]Режим атаки выключен!");
						isServerAttackModeEnabled = false;
						return;
					}
					if(args[0].equalsIgnoreCase("list")) {
						if(serverAttackModeRegex.size == 0) {
							player.sendMessage("[lightgray]Нет условий для кика");
							return;
						}
						for (int i = 0; i < serverAttackModeRegex.size; i++) {
							player.sendMessage("[gold]#" + i + " [lightgray]" + serverAttackModeRegex.get(i));
						}
						return;
					}
				} else if(args.length == 2) {
					if(args[0].equalsIgnoreCase("add")) {
						String reg = args[1];
						if(serverAttackModeRegex.contains(reg)) {
							player.sendMessage("[red]Такое правило уже есть");
						} else {
							serverAttackModeRegex.add(reg);
							player.sendMessage("[gold]Успешно добавлено");
						}
						return;
					}
					if(args[0].equalsIgnoreCase("remove")) {
						String reg = args[1];
						if(serverAttackModeRegex.contains(reg)) {
							serverAttackModeRegex.remove(reg);
							player.sendMessage("[gold]Успешно удалено!");
						} else {
							try {
								int index = Integer.parseInt(reg);
								if(index < 0) {
									player.sendMessage("[red]Индекс должен быть больше 0");
									return;
								}
								if(index < serverAttackModeRegex.size) {
									player.sendMessage("[red]Индекс должен быть меньше " + serverAttackModeRegex.size);
									return;
								}
								serverAttackModeRegex.remove(index);
								player.sendMessage("[gold]Успешно удалено!");
							} catch (NumberFormatException e) {
								player.sendMessage("[red]Второй аргумент - правило или его индекс: " + e.getMessage());
							}
						}
						return;
					}
				}
			}
		});

		handler.<Player>register("achievements", "<load/save/path> [args...]", "Управление достижениями", (args, player) -> {
			if(player.admin()) {
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
			}
		});
	}

	public void registerPlayersCommands(CommandHandler handler) {
		/**
		 * List of server maps
		 */
		handler.<Player>register("maps", "[all/custom/default]", "Показывает список доступных карт. Отображает все карты по умолчанию", (arg, player) -> {

			String types = "all";

			if(arg.length == 0) {
				types = maps.getShuffleMode().name();
			} else {
				types = arg[0];
			}

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
					for(Map map : all){
						String mapName = Strings.stripColors(map.name()).replace(' ', '_');
						if(map.custom){
							player.sendMessage(" [gold]Кастомная [white]| " + mapName + " (" + map.width + "x" + map.height + ")");
						}else{
							player.sendMessage(" [gray]Дефолтная [white]| " + mapName + " (" + map.width + "x" + map.height + ")");
						}
					}
				}
			} else {
				player.sendMessage("Карты не найдены");
			}
		});


		handler.<Player>register("discord", "", "\ue80d Сервера", (arg, player) -> {
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

		/**
		 * Map recourses statistic
		 */
		handler.<Player>register("mapinfo", "", "Показывает статистику ресурсов карты", (arg, player) -> {

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

		/**
		 * Map skipping
		 */

		handler.<Player>register("skipmap", "Начать голосование за пропуск карты", (arg, player) -> {
			if(currentlyMapSkipping[0] == null) {
				SkipmapVoteSession session = new SkipmapVoteSession(currentlyMapSkipping);
				session.vote(player, 1);
				currentlyMapSkipping[0] = session;
			} else {
				player.sendMessage("[scarlet]Голосование уже идет: [gold]/smvote <y/n>");
			}
		});

		handler.<Player>register("smvote", "<y/n>", "Проголосовать за/протов пропуск карты", (arg, player) -> {
			if(currentlyMapSkipping[0] == null){
				player.sendMessage("[scarlet]Nobody is being voted on.");
			}else{
				if(player.isLocal()){
					player.sendMessage("[scarlet]Локальные игроки не могут голосовать.");
					return;
				}

				if((currentlyMapSkipping[0].voted.contains(player.uuid()) || currentlyMapSkipping[0].voted.contains(Vars.netServer.admins.getInfo(player.uuid()).lastIP))){
					player.sendMessage("[scarlet]Ты уже проголосовал. Молчи!");
					return;
				}

				String voteSign = arg[0].toLowerCase();

				int sign = 0;
				if(voteSign.equals("y")) sign = +1;
				if(voteSign.equals("n")) sign = -1;

				if(sign == 0){
					player.sendMessage("[scarlet]Голосуйте либо \"y\" (да), либо \"n\" (нет)");
					return;
				}

				currentlyMapSkipping[0].vote(player, sign);
			}
		});

		handler.<Player>register("pluginfo", "info about pluging", (arg, player) -> {
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
}
