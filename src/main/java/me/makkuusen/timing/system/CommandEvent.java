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
import me.makkuusen.timing.system.participant.Subscriber;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Comparator;
import java.util.stream.Collectors;

@CommandAlias("event")
public class CommandEvent extends BaseCommand {

    @Default
    @Description("Active events")
    public static void onActiveEvents(CommandSender commandSender) {
        var list = EventDatabase.getEvents().stream().filter(event -> event.isActive()).collect(Collectors.toList());
        list.sort(Comparator.comparingLong(Event::getDate));
        commandSender.sendMessage("§aActive events right now:");
        for (Event event : list) {
            commandSender.sendMessage("§a" + event.getDisplayName() + " §2(§a" + event.getState().name() + "§2) - §a" + ApiUtilities.niceDate(event.getDate()) + "§2 by §a" + Database.getPlayer(event.getUuid()).getNameDisplay());
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

    @CommandPermission("event.admin")
    @Subcommand("finish")
    public static void onFinish(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        if (event.finish()) {
            player.sendMessage("§aEvent has finished");
            return;
        }
        player.sendMessage("§cEvent couldn't finish");
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
        sender.sendMessage("§aSigned Drivers: " + event.getSubscribers().size());
    }

    @CommandPermission("event.admin")
    @Subcommand("create")
    @CommandCompletion("<name> @track")
    public static void onCreate(Player player, @Single String name, @Optional Track track) {
        var maybeEvent = EventDatabase.eventNew(player.getUniqueId(), name);
        if (maybeEvent.isPresent()) {
            player.sendMessage("§aCreated event " + name);
            if (track != null) {
                maybeEvent.get().setTrack(track);
            }
            return;
        }
        player.sendMessage("§cCould not create event " + name);
    }

    @CommandPermission("event.admin")
    @Subcommand("delete")
    @CommandCompletion("@event")
    public static void onRemove(Player player, Event event){
        if (EventDatabase.removeEvent(event)){
            player.sendMessage("§aThe event was removed");
            return;
        }
        player.sendMessage("§cThe event could not be removed, is there any heat running?");
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
    @Subcommand("set signs")
    @CommandCompletion("open|closed")
    public static void setOpen(Player player, String open) {
        Event event;
        var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
        if (maybeEvent.isPresent()) {
            event = maybeEvent.get();
        } else {
            player.sendMessage("§cYou have no event selected");
            return;
        }
        if (open.equalsIgnoreCase("open")) {
            player.sendMessage("§aEvent signs are open");
            event.setOpenSign(true);
        } else {
            player.sendMessage("§aEvent signs are closed");
            event.setOpenSign(false);
        }
    }

    @Subcommand("spectate")
    @CommandCompletion("@event")
    public static void onSpectate(Player player, Event event) {
        if (event.isSpectating(player.getUniqueId())) {
            event.removeSpectator(player.getUniqueId());
            player.sendMessage("§aYou no longer spectating " + event.getDisplayName());
        } else {
            event.addSpectator(player.getUniqueId());
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
            player.sendMessage("§aYou are now spectating " + event.getDisplayName());
        }
    }

    @Subcommand("sign")
    @CommandCompletion("@event")
    public static void onSignUp(Player player, Event event, @Optional String name) {
        if (name != null) {

            if (!player.hasPermission("event.sign.others") || !player.hasPermission("event.admin") || !player.isOp()) {
                player.sendMessage("§cAccess denied");
                return;
            }

            TPlayer tPlayer = Database.getPlayer(name);
            if (tPlayer == null) {
                player.sendMessage("§cCould not find player");
                return;
            }

            if (event.isSubscribing(tPlayer.getUniqueId())) {
                if (event.getState() != Event.EventState.SETUP) {
                    player.sendMessage("§cEvent has already started and you can no longer remove signs from the event.");
                    return;
                }
                event.removeSubscriber(tPlayer.getUniqueId());
                player.sendMessage("§a" + tPlayer.getNameDisplay() + "§a is no longer signed up for " + event.getDisplayName());
                return;
            } else {

                if (event.isReserving(tPlayer.getUniqueId())) {
                    event.removeReserve(tPlayer.getUniqueId());
                }
                event.addSubscriber(tPlayer);
                EventDatabase.setPlayerSelectedEvent(tPlayer.getUniqueId(), event);
                player.sendMessage("§a" + tPlayer.getNameDisplay() + "§a is now signed up for " + event.getDisplayName());
                return;
            }
        }

        TPlayer tPlayer = Database.getPlayer(player.getUniqueId());
        if (event.isSubscribing(player.getUniqueId())) {
            if (event.getState() != Event.EventState.SETUP) {
                player.sendMessage("§cEvent has already started and you can no longer remove your sign from the event.");
                return;
            }
            event.removeSubscriber(player.getUniqueId());
            player.sendMessage("§aYou are no longer signed up for " + event.getDisplayName());
        } else {
            if (!event.isOpenSign()) {
                if (!player.hasPermission("event.sign") || !player.hasPermission("event.admin") || !player.isOp()) {
                    player.sendMessage("§cAccess denied");
                    return;
                }
            }

            if (event.isReserving(player.getUniqueId())) {
                event.removeReserve(player.getUniqueId());
            }
            event.addSubscriber(tPlayer);
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
            player.sendMessage("§aYou are now signed up for " + event.getDisplayName());
        }
    }


    @Subcommand("list signs")
    @CommandPermission("event.admin")
    public static void onListSigns(Player player) {
        Event event;
        var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
        if (maybeEvent.isPresent()) {
            event = maybeEvent.get();
        } else {
            player.sendMessage("§cYou have no event selected");
            return;
        }

        int count = 1;
        player.sendMessage("§2--- Signs for §a" + event.getDisplayName() + " §2---");
        for (Subscriber s : event.getSubscribers().values()) {
            player.sendMessage("§2" + count++ + ": §a" + s.getTPlayer().getName());
        }

        count = 1;
        player.sendMessage("§2--- Reserves for §a" + event.getDisplayName() + " §2---");
        for (Subscriber s : event.getReserves().values()) {
            player.sendMessage("§2" + count++ + ": §a" + s.getTPlayer().getName());
        }
    }

    @Subcommand("reserve")
    @CommandCompletion("@event")
    public static void onReserve(Player player, Event event, @Optional String name) {
        if (name != null) {

            if (!player.hasPermission("event.admin") || !player.isOp()) {
                player.sendMessage("§cAccess denied");
                return;
            }

            TPlayer tPlayer = Database.getPlayer(name);
            if (tPlayer == null) {
                player.sendMessage("§cCould not find player");
                return;
            }

            if (event.isReserving(tPlayer.getUniqueId())) {
                if (event.getState() != Event.EventState.SETUP) {
                    player.sendMessage("§cEvent has already started and you can no longer remove reserves from the event.");
                    return;
                }
                event.removeReserve(tPlayer.getUniqueId());
                player.sendMessage("§a" + tPlayer.getNameDisplay() + "§a is no longer signed up as reserve for " + event.getDisplayName());
                return;
            } else {
                if (event.isSubscribing(tPlayer.getUniqueId())) {
                    event.removeSubscriber(tPlayer.getUniqueId());
                }
                event.addReserve(tPlayer);
                EventDatabase.setPlayerSelectedEvent(tPlayer.getUniqueId(), event);
                player.sendMessage("§a" + tPlayer.getNameDisplay() + "§a is now signed up as reserve for " + event.getDisplayName());
                return;
            }
        }
        var tPlayer = Database.getPlayer(player.getUniqueId());
        if (event.isReserving(player.getUniqueId())) {
            if (event.getState() != Event.EventState.SETUP) {
                player.sendMessage("§cEvent has already started and you can no longer remove your sign from the event.");
                return;
            }
            event.removeReserve(player.getUniqueId());
            player.sendMessage("§aYou are no longer signed up as reserve for " + event.getDisplayName());
        } else {
            if (event.isSubscribing(player.getUniqueId())) {
                player.sendMessage("§cYou are already signed up for " + event.getDisplayName());
                return;
            }
            event.addReserve(tPlayer);
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
            player.sendMessage("§aYou are now signed up as reserve for " + event.getDisplayName());
        }
    }

    @Subcommand("broadcast clicktosign")
    @CommandPermission("event.admin")
    public static void onSendSignUp(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }

        var message = Component.text("§3--> Click to sign up for §b§l" + event.getDisplayName() + " §3<--").clickEvent(ClickEvent.runCommand("/event sign " + event.getDisplayName()));
        for (Player p : Bukkit.getOnlinePlayers()) {

            if (event.isSubscribing(p.getUniqueId())) {
                continue;
            }

            if (!event.isOpenSign()) {
                if (!player.hasPermission("event.sign") || !player.hasPermission("event.admin") || !player.isOp()) {
                    continue;
                }
            }
            p.sendMessage("");
            p.sendMessage(message);
            p.sendMessage("");
        }
    }

    @Subcommand("broadcast clicktoreserve")
    @CommandPermission("event.admin")
    public static void onSendReserve(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }

        var message = Component.text("§3--> Click to sign up as reserve for §b§l" + event.getDisplayName() + " §3<--").clickEvent(ClickEvent.runCommand("/event reserve " + event.getDisplayName()));
        for (Player p : Bukkit.getOnlinePlayers()) {

            if (event.isReserving(p.getUniqueId()) || event.isSubscribing(p.getUniqueId())) {
                continue;
            }
            p.sendMessage("");
            p.sendMessage(message);
            p.sendMessage("");
        }
    }
}
