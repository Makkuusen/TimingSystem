package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.event.EventResults;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("event")
public class CommandEvent extends BaseCommand {

    @Default
    @Subcommand("help")
    @Description("Displays help")
    public static void onHelp(Player player) {
        player.sendMessage("§2/event help");
    }

    @CommandPermission("event.admin")
    @Subcommand("start")
    public static void onStart(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        if (event.start()) {
            player.sendMessage("§aEvent has started");
            Event finalEvent = event;
            event.getSpectators().values().stream().forEach(spectator -> EventDatabase.setPlayerSelectedEvent(spectator.getTPlayer().getUniqueId(), finalEvent));
            return;
        }
        player.sendMessage("§cEvent couldn't start. Setup is not finished");
    }

    @Subcommand("info")
    @CommandCompletion("@event")
    public static void onInfo(CommandSender sender, Event event) {
        sender.sendMessage("§aEvent name: " + event.getDisplayName());
        if (event.getTrack() == null) {
            sender.sendMessage("§aTrack: None");
        } else {
            sender.sendMessage("§aTrack: " + event.getTrack().getDisplayName());
        }

        sender.sendMessage("§aState: " + event.getState());
    }

    @CommandPermission("event.admin")
    @Subcommand("create")
    @CommandCompletion("<name>")
    public static void onCreate(Player player, String name) {
        if (EventDatabase.eventNew(player.getUniqueId(), name)) {
            player.sendMessage("§aCreated event " + name);
            return;
        }
        player.sendMessage("§cCould not create event with name " + name);
    }

    @CommandPermission("event.admin")
    @Subcommand("delete")
    @CommandCompletion("@event")
    public static void onRemove(Player player, Event event){
        EventDatabase.removeEvent(event);
        player.sendMessage("§aThe event was removed");
    }

    @Subcommand("select")
    @CommandCompletion("@event")
    public static void onSelectEvent(Player player, Event event) {
        EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
        player.sendMessage("§aSelected new event");
    }

    @CommandPermission("event.admin")
    @Subcommand("set track")
    @CommandCompletion("@track")
    public static void onSetTrack(Player player, Track track) {
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

    @CommandPermission("event.admin")
    @Subcommand("finish qualification")
    public static void onFinishQualification(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        if (event.finishQualification()) {
            player.sendMessage("§aQualification has been finished. Get ready for finals!");
        } else {
            player.sendMessage("§cEvent is not in qualification mode");
        }
    }

    @CommandPermission("event.admin")
    @Subcommand("finish finals")
    public static void onFinishFinals(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        if (event.finishFinals()) {
            player.sendMessage("§aFinals and the event has been finished. It's podium time!");
        } else {
            player.sendMessage("§cEvent is not in finals mode");
        }
    }

    @Subcommand("results finals")
    public static void onResultsFinals(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        List<Driver> finalResults = EventResults.generateFinalResults(event.getEventSchedule().getFinalHeatList());
        if (finalResults.size() != 0 && event.getState() == Event.EventState.FINISHED) {
            player.sendMessage("§2Final results for event §a" + event.getDisplayName());
            int pos = 1;
            for (Driver d : finalResults) {
                if (d.isFinished()) {
                    player.sendMessage("§2" + pos++ + ". §a" + d.getTPlayer().getName() + "§2 - §a" + d.getLaps().size() + " §2laps in §a" + ApiUtilities.formatAsTime(d.getFinishTime()));
                } else {
                    player.sendMessage("§2" + pos++ + ". §a" + d.getTPlayer().getName());
                }
            }
        } else {
            player.sendMessage("§cFinals has not been finished");
        }
    }

    @Subcommand("results qualification")
    public static void onResultsQualification(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        List<Driver> qualyResults = EventResults.generateQualificationResults(event.getEventSchedule().getQualifyHeatList());
        if (qualyResults.size() != 0) {
            player.sendMessage("§2Qualifying results for event §a" + event.getDisplayName());
            int pos = 1;
            for (Driver d : qualyResults) {
                player.sendMessage("§2" + pos++ + ". §a" + d.getTPlayer().getName() + "§2 - §a"  + (d.getBestLap().isPresent() ? ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) : "0"));
            }
        } else {
            player.sendMessage("§cQualification results are empty");
        }
    }
    @Subcommand("spectate")
    @CommandCompletion("@event")
    public static void onSpectate(Player player, Event event){
        event.addSpectator(player.getUniqueId());
        EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
        player.sendMessage("§aYou are now spectating " + event.getDisplayName());
    }
}
