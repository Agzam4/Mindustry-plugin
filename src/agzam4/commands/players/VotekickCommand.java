package agzam4.commands.players;

import agzam4.Images;
import agzam4.CommandsManager.ReceiverType;
import agzam4.CommandsManager.ResultSender;
import agzam4.Game;
import agzam4.admins.Admins;
import agzam4.bot.Bots;
import agzam4.bot.TelegramBot;
import agzam4.bot.Bots.NotifyTag;
import agzam4.commands.CommandHandler;
import agzam4.commands.Permissions;
import agzam4.managers.Kicks;
import agzam4.utils.Log;
import agzam4.votes.Cooldowns;
import agzam4.votes.KickVoteSession;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.core.NetServer;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration.Config;

public class VotekickCommand extends CommandHandler<Player> {

	public static Cooldowns<String> cooldowns = new Cooldowns<>(NetServer.voteCooldown);
    
	{
		parms = "[игрок] [причина...]";
		desc = "Проголосовать, чтобы кикнуть игрока по уважительной причине";
	}
	
	@Override
	public void command(String[] args, ResultSender sender, Player player, ReceiverType type) {
		try {
            if(require(!Config.enableVotekick.bool(), sender, "[red]Голосование на этом сервере отключено")) return;
            if(require(player.isLocal(), sender, "[red]Просто кикни их сам, если ты хост")) return;
            boolean permission = Admins.has(player, "votekick");
            if(require(KickVoteSession.current != null && !(permission && !player.admin), sender, "[red]Голосование уже идет")) return;

            if(args.length == 0){
                StringBuilder builder = new StringBuilder();
                builder.append("[orange]Игроки для кика: \n");

                Groups.player.each(p -> !p.admin && p.con != null && p != sender, p -> {
                    builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id()).append(")\n");
                });
                sender.sendMessage(builder.toString());
            }else if(args.length == 1){
                sender.sendMessage("[orange]Для кика игрока вам нужна веская причина. Укажите причину после имени игрока");
            }else{
            	String reason = args[1];
            	if(reason.equalsIgnoreCase("g") || reason.equalsIgnoreCase("г")) reason = "гриф";
            	if(reason.equalsIgnoreCase("f") || reason.equalsIgnoreCase("ф")) reason = "фрикикер";
            	
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
                    if(found == sender){
                        sender.sendMessage("[red]Ты не можешь голосовать за то, чтобы кикнуть себя");
                    }else if(found.admin){
                        sender.sendMessage("[red]Хо-хо-хо, ты действительно ожидал, что сможешь выгнать администратора?");
                    }else if(Admins.has(found, "votekick")){
                        sender.sendMessage("[red]Этот игрок защищен пластаном");
                    }else if(Admins.has(found, Permissions.whitelist)){
                        sender.sendMessage("[red]Этот игрок защищен метастеклом");
                    }else if(found.isLocal()){
                        sender.sendMessage("[red]Локальные игроки не могут быть выгнаны");
                    }else if(!permission && found.team() != player.team()){
                        sender.sendMessage("[red]Кикать можно только игроков из вашей команды");
                    }else{
                    	if(permission) {
                    		Kicks.kick(player, found, reason);
                    	} else {
                            var vtime = cooldowns.get(player.uuid());
                            if(vtime.cooldown()){
                                sender.sendMessage("[red]Вы должны подождать " + NetServer.voteCooldown/60 + " минут между голосованиями");
                                return;
                            }
                            KickVoteSession session = new KickVoteSession(player, found, reason);
                            Bots.notify(NotifyTag.votekick, Strings.format("<code>@</code> started voting for kicking <code>@</code> (<code>#@</code>): <code>@</code>", TelegramBot.strip(player.name), TelegramBot.strip(found.name), found.id, TelegramBot.strip(reason)));
                            Bots.notify(NotifyTag.votekick, Images.screenshot(found));
                            
                            session.vote(player, 1);
                            Call.sendMessage(Strings.format("[lightgray]Причина:[orange] @[lightgray].", reason));
                            
                            vtime.start();
                            session.passListener = () -> vtime.stop();
                            
                            KickVoteSession.current = session;
                    	}
                    }
                }else{
                    sender.sendMessage("[red]Игрок[orange]'" + args[0] + "'[red] не найден.");
                }
            }
		} catch (Exception e) {
			Log.err(e);
			sender.sendMessage("[red]" + e.getLocalizedMessage());
		}
	}

	@Override
	public Seq<String> complete(String[] args, Player receiver, ReceiverType type) {
		if(args.length == 0) return Game.playersNames();
		if(args.length == 1) return Seq.with("гриф", "фрикикер", "подрыв");
		if(args.length == 2 && (args[1].equalsIgnoreCase("гриф") || args[1].equalsIgnoreCase("подрыв"))) return Seq.with("юнитов", "энергии", "взрывами", "логикой", "конвееров", "реакторов");
		return super.complete(args, receiver, type);
	}
	
}
