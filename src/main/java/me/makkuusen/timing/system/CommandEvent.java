package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.event.EventResults;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CommandAlias("event")
public class CommandEvent extends BaseCommand {

    @Default
    @Description("Active events")
    public static void onActiveEvents(Player player) {
        var list = EventDatabase.getEvents().stream().filter(event -> event.isActive()).collect(Collectors.toList());
        list.sort(Comparator.comparingLong(Event::getDate));
        player.sendMessage("§aActive events right now:");
        for (Event event : list) {
            player.sendMessage("§a" + event.getDisplayName() + " §2(§a" + event.getState().name() + "§2) - §a" + ApiUtilities.niceDate(event.getDate()) + "§2 by §a" + Database.getPlayer(event.getUuid()).getNameDisplay());
        }
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
        if (event.getEventSchedule().getCurrentRound() != null){
            sender.sendMessage("§aRound: " + event.getEventSchedule().getCurrentRound());
        }

        sender.sendMessage("§aState: " + event.getState());
    }

    @CommandPermission("event.admin")
    @Subcommand("create")
    @CommandCompletion("<name>")
    public static void onCreate(Player player, @Single String name) {
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
    @Subcommand("round finish")
    public static void onRoundFinish(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        if (event.eventSchedule.getRound().get().finish(event)) {
            player.sendMessage("§aRound has been finished!");
        } else {
            player.sendMessage("§cRound could not be finished");
        }
    }

    @Subcommand("round results")
    @CommandCompletion("<roundNumber>")
    public static void onRoundResults(Player player, Integer index, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        List<Driver> results = EventResults.generateRoundResults(event.getEventSchedule().getRound(index).get().getHeats());
        if (results.size() != 0) {
            player.sendMessage("§2Round results for event §a" + event.getDisplayName());
            int pos = 1;
            for (Driver d : results) {
                player.sendMessage("§2" + pos++ + ". §a" + d.getTPlayer().getName());

            }
        } else {
            player.sendMessage("§cRound has not been finished");
        }
    }

    @Subcommand("spectate")
    @CommandCompletion("@event")
    public static void onSpectate(Player player, Event event) {
        event.addSpectator(player.getUniqueId());
        EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
        player.sendMessage("§aYou are now spectating " + event.getDisplayName());
    }

    @Subcommand("quick")
    public static void onQuickCreate(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        event.quickCreate();
        player.sendMessage("§aYou have made a quickEvent");

    }
}
