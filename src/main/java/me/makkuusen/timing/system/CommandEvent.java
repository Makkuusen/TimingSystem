package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.Track;
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

    @Subcommand("start")
    public static void onStart(Player player, @Optional Event event){
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        if (event.start()){
            player.sendMessage("§aEvent has started");
            return;
        }
        player.sendMessage("§cEvent couldn't start. Setup is not finished");
    }

    @Subcommand("info")
    @CommandCompletion("@event")
    public static void onInfo(CommandSender sender, Event event) {
        sender.sendMessage("§aEvent name: " + event.getDisplayName());
        sender.sendMessage("§aTrack: " + event.getTrack().getName());
        sender.sendMessage("§aState: " + event.getState());
    }

    @Subcommand("create")
    @CommandCompletion("<name>")
    public static void onCreate(Player player, String[] arguments) {
        if (arguments.length >= 1) {
            EventDatabase.eventNew(player.getUniqueId(), arguments[0]);
            player.sendMessage("§aCreated event " + arguments[0]);
        }
    }
    @Subcommand("select")
    @CommandCompletion("@event")
    public static void onSelectEvent(Player player, Event event){
        EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
        player.sendMessage("§aSelected new event");
    }

    @Subcommand("set track")
    @CommandCompletion("@track")
    public static void onSetTrack(Player player, Track track){
        Event event;
        var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
        if (maybeEvent.isPresent()) {
            event = maybeEvent.get();
        } else {
            player.sendMessage("§cYou have no event selected");
            return;
        }
        event.setTrack(track);
        player.sendMessage("§aTrack has been updated");

    }

    @Subcommand("quickstart")
    @CommandCompletion("@event")
    public static void onQuickSetup(Player player, Event event){
        List<TPlayer> tPlayers = new ArrayList<>();
        Bukkit.getOnlinePlayers().stream().forEach(p -> {
            tPlayers.add(TimingSystem.players.get(p.getUniqueId()));
        });
        event.setTrack(DatabaseTrack.getTrack("newbie").get());
        event.quickSetup(tPlayers, 60000, 3, 1);
        event.setState(Event.EventState.QUALIFICATION);
        player.sendMessage("§aDid a quick setup for " + event.getId());
    }

    @Subcommand("finish qualification")
    public static void onFinishQualification(Player player, @Optional Event event){
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        if (event.finishQualification()){
            player.sendMessage("§a Qualification has been finished. Get ready for finals!");
        }
    }

    @Subcommand("finish finals")
    public static void onFinishFinals(Player player, @Optional Event event){
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        if (event.finishFinals()){
            player.sendMessage("§a Finals and the event has been finished. It's podium time!");
        }
    }

    @Subcommand("results finals")
    public static void onResultsFinals(Player player, @Optional Event event){
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        List<Driver> finalResults = event.getEventResults().getFinalResults();
        if (finalResults.size() != 0 && event.getState() == Event.EventState.FINISHED) {
            player.sendMessage("§aFinal results for event " + event.getDisplayName());
            int pos = 1;
            for (Driver d : finalResults) {
                player.sendMessage("§a" + pos++ + ". " + d.getTPlayer().getName());
            }
        } else {
            player.sendMessage("§cFinals has not been finished");
        }
    }

    @Subcommand("results qualification")
    public static void onResultsQualification(Player player, @Optional Event event){
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        List<Driver> qualyResults = event.getEventResults().getQualyResults();
        if (qualyResults.size() != 0 && (event.getState() == Event.EventState.FINAL || event.getState() == Event.EventState.FINISHED)) {
            player.sendMessage("§aQualifying results for event " + event.getDisplayName());
            int pos = 1;
            for (Driver d : qualyResults) {
                player.sendMessage("§a" + pos++ + ". " + d.getTPlayer().getName());
            }
        } else {
            player.sendMessage("§cQualification has not been finished");
        }
    }
}
