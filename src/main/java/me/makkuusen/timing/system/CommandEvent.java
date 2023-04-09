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
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Subscriber;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.text.TextButtons;
import me.makkuusen.timing.system.text.Errors;
import me.makkuusen.timing.system.text.TextUtilities;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.stream.Collectors;

@CommandAlias("event")
public class CommandEvent extends BaseCommand {

    @Default
    @Description("Active events")
    public static void onActiveEvents(CommandSender commandSender) {
        if (commandSender instanceof Player player ){
            Event event;
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
                onInfo(player, event);
                return;
            }
        }

        onListEvents(commandSender);
    }

    @Subcommand("list")
    public static void onListEvents(CommandSender commandSender) {
        var list = EventDatabase.getEvents().stream().filter(event -> event.isActive()).collect(Collectors.toList());
        list.sort(Comparator.comparingLong(Event::getDate));
        commandSender.sendMessage("");
        commandSender.sendMessage(TextUtilities.dark("Active events right now:"));
        for (Event event : list) {
            commandSender.sendMessage(TextUtilities.highlight(event.getDisplayName())
                    .clickEvent(ClickEvent.runCommand("/event info " + event.getDisplayName()))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to view event")))
                    .append(TextUtilities.space())
                    .append(TextUtilities.getParenthisied(event.getState().name()))
                    .append(TextUtilities.dark(" - "))
                    .append(TextUtilities.dark(ApiUtilities.niceDate(event.getDate())))
                    .append(TextUtilities.space())
                    .append(TextUtilities.dark("by"))
                    .append(TextUtilities.space())
                    .append(TextUtilities.highlight(Database.getPlayer(event.getUuid()).getNameDisplay()))
            );
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
                player.sendMessage(Errors.NO_EVENT_SELECTED.message());
                return;
            }
        }
        if (event.start()) {
            player.sendMessage(TextUtilities.success("Event has started"));
            Event finalEvent = event;
            event.getSpectators().values().stream().forEach(spectator -> EventDatabase.setPlayerSelectedEvent(spectator.getTPlayer().getUniqueId(), finalEvent));
            return;
        }
        player.sendMessage(TextUtilities.error("Event couldn't start. Setup is not finished"));
    }

    @CommandPermission("event.admin")
    @Subcommand("finish")
    public static void onFinish(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage(Errors.NO_EVENT_SELECTED.message());
                return;
            }
        }
        if (event.finish()) {
            player.sendMessage(TextUtilities.success("Event has finished"));
            return;
        }
        player.sendMessage(TextUtilities.error("Event couldn't finish"));
    }

    @Subcommand("info")
    @CommandCompletion("@event")
    public static void onInfo(CommandSender sender, Event event) {
        if (sender instanceof Player player) {
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
        }
        sender.sendMessage("");
        sender.sendMessage(TextButtons.getRefreshButton().clickEvent(ClickEvent.runCommand("/event info " + event.getDisplayName()))
                .append(TextUtilities.space())
                .append(TextUtilities.getTitleLine(
                        Component.text(event.getDisplayName()).color(TextUtilities.textHighlightColor)
                        .append(TextUtilities.space())
                        .append(TextUtilities.getParenthisied(event.getState().name())))
                )
        );

        if (event.getTrack() == null) {
            sender.sendMessage(Component.text("Track:").color(TextUtilities.textDarkColor)
                    .append(TextUtilities.space())
                    .append(Component.text("None").color(TextUtilities.textHighlightColor))
                    .append(TextUtilities.space())
                    .append(TextButtons.getEditButton().clickEvent(ClickEvent.suggestCommand("/event set track ")))
            );
        } else {
            sender.sendMessage(Component.text("Track:").color(TextUtilities.textDarkColor)
                    .append(TextUtilities.space())
                    .append(Component.text(event.getTrack().getDisplayName()).color(TextUtilities.textHighlightColor))
                    .append(TextUtilities.space())
                    .append(TextButtons.getViewButton().clickEvent(ClickEvent.runCommand("/track info " + event.getTrack().getCommandName())).hoverEvent(TextButtons.getClickToViewHoverEvent()))
                    .append(TextUtilities.space())
                    .append(TextButtons.getEditButton().clickEvent(ClickEvent.suggestCommand("/event set track ")))
            );
        }
        sender.sendMessage(Component.text("Signed Drivers:").color(TextUtilities.textDarkColor)
                .append(TextUtilities.space())
                .append(Component.text(event.getSubscribers().size() + "+" + event.getReserves().size()).color(TextUtilities.textHighlightColor))
                .append(TextUtilities.space())
                .append(TextButtons.getViewButton().clickEvent(ClickEvent.runCommand("/event signs " + event.getDisplayName()))
                        .hoverEvent(TextButtons.getClickToViewHoverEvent()))
        );

        if (event.getEventSchedule().getCurrentRound() != null){
            sender.sendMessage(Component.text("Current Round:").color(TextUtilities.textDarkColor)
                    .append(TextUtilities.space())
                    .append(Component.text(event.getEventSchedule().getCurrentRound()).color(TextUtilities.textHighlightColor))
            );
        } else {
            sender.sendMessage(Component.text("Current Round:").color(TextUtilities.textDarkColor)
                    .append(TextUtilities.space())
                    .append(Component.text("None").color(TextUtilities.textHighlightColor))
            );
        }

        sender.sendMessage(Component.text("Rounds:").color(TextUtilities.textDarkColor)
                .append(TextUtilities.space())
                .append(Component.text(event.eventSchedule.getRounds().size()).color(TextUtilities.textHighlightColor))
                .append(TextUtilities.tab())
                .append(TextButtons.getAddButton("Round").clickEvent(ClickEvent.suggestCommand("/round create ")).hoverEvent(TextButtons.getClickToAddHoverEvent()))
        );

        for (Round round : event.eventSchedule.getRounds()) {
            sender.sendMessage(TextUtilities.tab()
                    .append(Component.text(round.getDisplayName() + ":").color(TextUtilities.textDarkColor))
                    .append(TextUtilities.tab())
                    .append(TextButtons.getAddButton("Heat").clickEvent(ClickEvent.runCommand("/heat create " + round.getName())).hoverEvent(TextButtons.getClickToAddHoverEvent()))
            );

            for (Heat heat : round.getHeats()) {
                sender.sendMessage(TextUtilities.tab()
                        .append(TextUtilities.tab())
                        .append(Component.text(heat.getName()).color(TextUtilities.textHighlightColor))
                        .append(TextUtilities.tab())
                        .append(TextButtons.getViewButton().clickEvent(ClickEvent.runCommand("/heat info " + heat.getName())).hoverEvent(TextButtons.getClickToViewHoverEvent()))
                        .append(TextUtilities.space())
                        .append(TextButtons.getRemoveButton().clickEvent(ClickEvent.suggestCommand("/heat delete " + heat.getName())))

                );
            }
        }
    }

    @CommandPermission("event.admin")
    @Subcommand("create")
    @CommandCompletion("<name> @track")
    public static void onCreate(Player player, @Single String name, @Optional Track track) {
        var maybeEvent = EventDatabase.eventNew(player.getUniqueId(), name);
        if (maybeEvent.isPresent()) {
            player.sendMessage(TextUtilities.success("Created event " + name));
            if (track != null) {
                maybeEvent.get().setTrack(track);
            }
            return;
        }
        player.sendMessage(TextUtilities.error("Could not create event " + name));
    }

    @CommandPermission("event.admin")
    @Subcommand("delete")
    @CommandCompletion("@event")
    public static void onRemove(Player player, Event event){
        if (EventDatabase.removeEvent(event)){
            player.sendMessage(TextUtilities.success("The event was removed"));
            return;
        }
        player.sendMessage(TextUtilities.error("The event could not be removed, is there any heat running?"));
    }

    @Subcommand("select")
    @CommandCompletion("@event")
    public static void onSelectEvent(Player player, Event event) {
        EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
        player.sendMessage(TextUtilities.success("Selected event"));
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
            player.sendMessage(Errors.NO_EVENT_SELECTED.message());
            return;
        }
        event.setTrack(track);
        player.sendMessage(TextUtilities.success("Track has been updated"));
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
            player.sendMessage(Errors.NO_EVENT_SELECTED.message());
            return;
        }
        if (open.equalsIgnoreCase("open")) {
            player.sendMessage(TextUtilities.success("Event signs are open"));
            event.setOpenSign(true);
        } else {
            player.sendMessage(TextUtilities.success("Event signs are closed"));
            event.setOpenSign(false);
        }
    }

    @Subcommand("spectate")
    @CommandCompletion("@event")
    public static void onSpectate(Player player, Event event) {
        if (event.isSpectating(player.getUniqueId())) {
            event.removeSpectator(player.getUniqueId());
            player.sendMessage(TextUtilities.success("You no longer spectating " + event.getDisplayName()));
        } else {
            event.addSpectator(player.getUniqueId());
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
            player.sendMessage(TextUtilities.success("You are now spectating " + event.getDisplayName()));
        }
    }

    @Subcommand("sign")
    @CommandCompletion("@event")
    public static void onSignUp(Player player, Event event, @Optional String name) {
        if (name != null) {

            if (!player.hasPermission("event.sign.others") || !player.hasPermission("event.admin") || !player.isOp()) {
                player.sendMessage(Errors.PERMISSION_DENIED.message());
                return;
            }

            TPlayer tPlayer = Database.getPlayer(name);
            if (tPlayer == null) {
                player.sendMessage(Errors.PLAYER_NOT_FOUND.message());
                return;
            }

            if (event.isSubscribing(tPlayer.getUniqueId())) {
                if (event.getState() != Event.EventState.SETUP) {
                    player.sendMessage(TextUtilities.error("Event has already started and you can no longer remove signs from the event."));
                    return;
                }
                event.removeSubscriber(tPlayer.getUniqueId());
                player.sendMessage(TextUtilities.success(tPlayer.getName() + " is no longer signed up for " + event.getDisplayName()));
                return;
            } else {

                if (event.isReserving(tPlayer.getUniqueId())) {
                    event.removeReserve(tPlayer.getUniqueId());
                }
                event.addSubscriber(tPlayer);
                EventDatabase.setPlayerSelectedEvent(tPlayer.getUniqueId(), event);
                player.sendMessage(TextUtilities.success( tPlayer.getName() + " is now signed up for " + event.getDisplayName()));
                return;
            }
        }

        TPlayer tPlayer = Database.getPlayer(player.getUniqueId());
        if (event.isSubscribing(player.getUniqueId())) {
            if (event.getState() != Event.EventState.SETUP) {
                player.sendMessage(TextUtilities.error("Event has already started and you can no longer remove your sign from the event."));
                return;
            }
            event.removeSubscriber(player.getUniqueId());
            player.sendMessage(TextUtilities.success("You are no longer signed up for " + event.getDisplayName()));
        } else {
            if (!event.isOpenSign()) {
                if (!player.hasPermission("event.sign") || !player.hasPermission("event.admin") || !player.isOp()) {
                    player.sendMessage(Errors.PERMISSION_DENIED.message());
                    return;
                }
            }

            if (event.isReserving(player.getUniqueId())) {
                event.removeReserve(player.getUniqueId());
            }
            event.addSubscriber(tPlayer);
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
            player.sendMessage(TextUtilities.success("You are now signed up for " + event.getDisplayName()));
        }
    }


    @Subcommand("signs")
    @CommandPermission("event.admin")
    public static void onListSigns(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage(Errors.NO_EVENT_SELECTED.message());
                return;
            }
        }

        int count = 1;
        player.sendMessage("");
        player.sendMessage(TextUtilities.getTitleLine("Signs for", event.getDisplayName())
                .append(Component.space())
                .append(TextButtons.getAddButton().clickEvent(ClickEvent.suggestCommand("/event sign " + event.getDisplayName() + " "))));
        if (event.getTrack() != null) {
            var sortedList = CommandRound.getSortedList(event.getSubscribers().values().stream().map(Subscriber::getTPlayer).collect(Collectors.toList()), event.getTrack());
            for (TPlayer tPlayer : sortedList) {
                var bestTime = event.getTrack().getBestFinish(tPlayer);
                player.sendMessage(TextUtilities.dark(count++ + ":")
                        .append(TextUtilities.space())
                        .append(TextUtilities.highlight(tPlayer.getName()))
                        .append(TextUtilities.hyphen())
                        .append(TextUtilities.highlight((bestTime == null ? "(None)" : ApiUtilities.formatAsTime(bestTime.getTime()))))
                );
            }
        } else {
            for (Subscriber s : event.getSubscribers().values()) {
                player.sendMessage(TextUtilities.dark(count++ + ":")
                        .append(TextUtilities.space())
                        .append(TextUtilities.highlight(s.getTPlayer().getName()))
                );
            }
        }


        count = 1;
        player.sendMessage(TextUtilities.getTitleLine("Reserves for", event.getDisplayName()).append(Component.space())
                .append(TextButtons.getAddButton().clickEvent(ClickEvent.suggestCommand("/event reserve " + event.getDisplayName() + " "))));
        if (event.getTrack() != null) {
            var sortedList = CommandRound.getSortedList(event.getReserves().values().stream().map(Subscriber::getTPlayer).collect(Collectors.toList()), event.getTrack());
            for (TPlayer tPlayer : sortedList) {
                var bestTime = event.getTrack().getBestFinish(tPlayer);
                player.sendMessage(TextUtilities.dark(count++ + ":")
                        .append(TextUtilities.space())
                        .append(TextUtilities.highlight(tPlayer.getName()))
                        .append(TextUtilities.hyphen())
                        .append(TextUtilities.highlight((bestTime == null ? "(None)" : ApiUtilities.formatAsTime(bestTime.getTime()))))

                );
            }
        } else {
            for (Subscriber s : event.getReserves().values()) {
                player.sendMessage(TextUtilities.dark(count++ + ":")
                        .append(TextUtilities.space())
                        .append(TextUtilities.highlight(s.getTPlayer().getName()))
                );
            }
        }
    }

    @Subcommand("reserve")
    @CommandCompletion("@event")
    public static void onReserve(Player player, Event event, @Optional String name) {
        if (name != null) {

            if (!player.hasPermission("event.admin") || !player.isOp()) {
                player.sendMessage(Errors.PERMISSION_DENIED.message());
                return;
            }

            TPlayer tPlayer = Database.getPlayer(name);
            if (tPlayer == null) {
                player.sendMessage(Errors.PLAYER_NOT_FOUND.message());
                return;
            }

            if (event.isReserving(tPlayer.getUniqueId())) {
                if (event.getState() != Event.EventState.SETUP) {
                    player.sendMessage(TextUtilities.error("Event has already started and you can no longer remove reserves from the event."));
                    return;
                }
                event.removeReserve(tPlayer.getUniqueId());
                player.sendMessage(TextUtilities.success(tPlayer.getName() + " is no longer signed up as reserve for " + event.getDisplayName()));
                return;
            } else {
                if (event.isSubscribing(tPlayer.getUniqueId())) {
                    event.removeSubscriber(tPlayer.getUniqueId());
                }
                event.addReserve(tPlayer);
                EventDatabase.setPlayerSelectedEvent(tPlayer.getUniqueId(), event);
                player.sendMessage(TextUtilities.success(tPlayer.getNameDisplay() + " is now signed up as reserve for " + event.getDisplayName()));
                return;
            }
        }
        var tPlayer = Database.getPlayer(player.getUniqueId());
        if (event.isReserving(player.getUniqueId())) {
            if (event.getState() != Event.EventState.SETUP) {
                player.sendMessage(TextUtilities.error("Event has already started and you can no longer remove your sign from the event."));
                return;
            }
            event.removeReserve(player.getUniqueId());
            player.sendMessage(TextUtilities.success("You are no longer signed up as reserve for " + event.getDisplayName()));
        } else {
            if (event.isSubscribing(player.getUniqueId())) {
                player.sendMessage(TextUtilities.error("You are already signed up for " + event.getDisplayName()));
                return;
            }
            event.addReserve(tPlayer);
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
            player.sendMessage(TextUtilities.success("You are now signed up as reserve for " + event.getDisplayName()));
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
                player.sendMessage(Errors.NO_EVENT_SELECTED.message());
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
                player.sendMessage(Errors.NO_EVENT_SELECTED.message());
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
