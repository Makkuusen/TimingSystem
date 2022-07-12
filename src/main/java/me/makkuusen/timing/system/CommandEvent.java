package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("event")
public class CommandEvent extends BaseCommand {

    @Default
    @Subcommand("help")
    @Description("Displays help")
    public static void onHelp(Player player) {
        if (player.isOp() || player.hasPermission("event.command.help"))
        {
            player.sendMessage("§2/event help");
        }
    }

    @Subcommand("info")
    @CommandCompletion("@event")
    public static void onInfo(CommandSender sender, Event event) {
        sender.sendMessage("§aEvent name: " + event.getDisplayName());
    }

    @Subcommand("create")
    @CommandCompletion("id")
    public static void onCreate(Player player, String[] arguments) {
        if (arguments.length >= 1) {
            EventDatabase.eventNew(player.getUniqueId(), arguments[0]);
            player.sendMessage("§aCreated event " + arguments[0]);
        }
    }

    @Subcommand("quickstart")
    @CommandCompletion("@event")
    public static void onQuickSetup(Player player, Event event){
        List<TPlayer> tPlayers = new ArrayList<>();
        Bukkit.getOnlinePlayers().stream().forEach(p -> tPlayers.add(TimingSystem.players.get(p.getUniqueId())));
        event.setTrack(TrackDatabase.getTrack("newbie").get());
        event.quickSetup(tPlayers, 60000, 3, 1);
        player.sendMessage("§aDid a quick setup for " + event.getId());
    }
}
