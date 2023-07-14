package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.participant.Subscriber;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.text.Broadcast;
import me.makkuusen.timing.system.text.Info;
import me.makkuusen.timing.system.text.Success;
import me.makkuusen.timing.system.text.TextButtons;
import me.makkuusen.timing.system.text.Error;
import me.makkuusen.timing.system.text.Warning;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.stream.Collectors;

@CommandAlias("event")
public class CommandEvent extends BaseCommand {
    public static TimingSystem plugin;

    @Default
    @Description("Active events")
    public static void onActiveEvents(CommandSender commandSender) {
        if (commandSender instanceof Player player) {
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
        var list = EventDatabase.getEvents().stream().filter(Event::isActive).sorted(Comparator.comparingLong(Event::getDate)).toList();
        commandSender.sendMessage(Component.empty());
        plugin.sendMessage(commandSender, Info.ACTIVE_EVENTS_TITLE);
        Theme theme = Theme.getTheme(commandSender);
        for (Event event : list) {
            commandSender.sendMessage(theme.highlight(event.getDisplayName()).clickEvent(ClickEvent.runCommand("/event info " + event.getDisplayName())).hoverEvent(HoverEvent.showText(Component.text("Click to select event"))).append(Component.space()).append(theme.getParenthesized(event.getState().name())).append(theme.primary(" - ")).append(theme.primary(ApiUtilities.niceDate(event.getDate()))).append(Component.space()).append(theme.primary("by")).append(Component.space()).append(theme.highlight(Database.getPlayer(event.getUuid()).getNameDisplay())));
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
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                return;
            }
        }
        if (event.start()) {
            plugin.sendMessage(player, Success.EVENT_STARTED);
            Event finalEvent = event;
            event.getSpectators().values().forEach(spectator -> EventDatabase.setPlayerSelectedEvent(spectator.getTPlayer().getUniqueId(), finalEvent));
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_START_EVENT);
    }

    @CommandPermission("event.admin")
    @Subcommand("finish")
    public static void onFinish(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                return;
            }
        }
        if (event.finish()) {
            plugin.sendMessage(player, Success.EVENT_FINISHED);
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_FINISH_EVENT);
    }

    @Subcommand("info")
    @CommandCompletion("@event")
    public static void onInfo(CommandSender sender, Event event) {
        if (sender instanceof Player player) {
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
        }

        Theme theme = Theme.getTheme(sender);

        sender.sendMessage("");
        sender.sendMessage(TextButtons.getRefreshButton().clickEvent(ClickEvent.runCommand("/event info " + event.getDisplayName())).append(Component.space()).append(theme.getTitleLine(Component.text(event.getDisplayName()).color(theme.getSecondary()).append(Component.space()).append(theme.getParenthesized(event.getState().name())))));

        net.kyori.adventure.text.TextComponent trackMessage;
        if (event.getTrack() == null) {

            trackMessage = Component.text("Track:").color(theme.getPrimary()).append(Component.space()).append(Component.text("None").color(theme.getSecondary()));

        } else {
            trackMessage = Component.text("Track:").color(theme.getPrimary()).append(Component.space()).append(Component.text(event.getTrack().getDisplayName()).color(theme.getSecondary())).append(Component.space()).append(TextButtons.getViewButton().clickEvent(ClickEvent.runCommand("/track info " + event.getTrack().getCommandName())).hoverEvent(TextButtons.getClickToViewHoverEvent()));

        }
        if (sender.hasPermission("event.admin")) {
            trackMessage = trackMessage.append(Component.space()).append(TextButtons.getEditButton().clickEvent(ClickEvent.suggestCommand("/event set track ")));
        }
        sender.sendMessage(trackMessage);

        var signsMessage = theme.primary("Signs:").append(Component.space());


        if (sender.hasPermission("event.admin")) {
            if (event.isOpenSign()) {
                signsMessage = signsMessage.append(theme.getBrackets("Open").clickEvent(ClickEvent.runCommand("/event set signs closed")).hoverEvent(HoverEvent.showText(Component.text("Click to close"))));
            } else {
                signsMessage = signsMessage.append(theme.getBrackets("Closed").clickEvent(ClickEvent.runCommand("/event set signs open")).hoverEvent(HoverEvent.showText(Component.text("Click to open"))));
            }
        } else {
            signsMessage = event.isOpenSign() ? signsMessage.append(theme.highlight("Open")) : signsMessage.append(theme.highlight("Closed"));
        }

        sender.sendMessage(signsMessage);

        sender.sendMessage(theme.primary("Signed Drivers:").append(Component.space()).append(Component.text(event.getSubscribers().size() + "+" + event.getReserves().size()).color(theme.getSecondary())).append(Component.space()).append(TextButtons.getViewButton().clickEvent(ClickEvent.runCommand("/event signs " + event.getDisplayName())).hoverEvent(TextButtons.getClickToViewHoverEvent())));

        var roundsMessage = Component.text("Rounds:").color(theme.getPrimary()).append(Component.space()).append(Component.text(event.eventSchedule.getRounds().size()).color(theme.getSecondary()));

        if (sender.hasPermission("event.admin")) {
            roundsMessage = roundsMessage.append(theme.tab()).append(TextButtons.getAddButton("Round").clickEvent(ClickEvent.suggestCommand("/round create ")).hoverEvent(TextButtons.getClickToAddHoverEvent()));
        }

        sender.sendMessage(roundsMessage);

        for (Round round : event.eventSchedule.getRounds()) {

            boolean currentRound = round.getRoundIndex() == event.getEventSchedule().getCurrentRound() && event.getState() != Event.EventState.FINISHED;
            var roundMessage = (currentRound ? theme.arrow() : theme.tab()).append(Component.text(round.getDisplayName() + ":").color(theme.getPrimary()));

            if (sender.hasPermission("event.admin") && round.getState() != Round.RoundState.FINISHED) {
                roundMessage = roundMessage.append(Component.space()).append(TextButtons.getAddButton("Heat").clickEvent(ClickEvent.runCommand("/heat create " + round.getName())).hoverEvent(TextButtons.getClickToAddHoverEvent()));

                if (currentRound) {
                    roundMessage = roundMessage.append(Component.space().append(Component.text("[Finish]").color(NamedTextColor.GRAY).clickEvent(ClickEvent.suggestCommand("/round finish")).hoverEvent(HoverEvent.showText(Component.text("Click to finish round")))));
                }

                if (round.getState() != Round.RoundState.RUNNING) {
                    roundMessage = roundMessage.append(Component.space().append(TextButtons.getRemoveButton().clickEvent(ClickEvent.suggestCommand("/round delete " + round.getName()))));
                }
            }

            sender.sendMessage(roundMessage);

            for (Heat heat : round.getHeats()) {
                var heatName = Component.text(heat.getName()).color(theme.getSecondary());
                heatName = heat.getHeatState() == HeatState.FINISHED ? heatName.decorate(TextDecoration.ITALIC) : heatName;
                var heatMessage = theme.tab().append(theme.tab()).append(heatName).append(theme.tab()).append(TextButtons.getViewButton().clickEvent(ClickEvent.runCommand("/heat info " + heat.getName())).hoverEvent(TextButtons.getClickToViewHoverEvent()));

                if (!heat.isFinished() && sender.hasPermission("event.admin")) {
                    heatMessage = heatMessage.append(Component.space()).append(TextButtons.getRemoveButton().clickEvent(ClickEvent.suggestCommand("/heat delete " + heat.getName())));
                }
                sender.sendMessage(heatMessage);
            }
        }
    }

    @CommandPermission("event.admin")
    @Subcommand("create")
    @CommandCompletion("<name> @track")
    public static void onCreate(Player player, @Single String name, @Optional Track track) {
        if (name.equalsIgnoreCase("QuickRace")) {
            plugin.sendMessage(player, Error.INVALID_NAME);
            return;
        }
        var maybeEvent = EventDatabase.eventNew(player.getUniqueId(), name);
        if (maybeEvent.isPresent()) {
            plugin.sendMessage(player, Success.CREATED_EVENT, "%event%", name);
            if (track != null) {
                maybeEvent.get().setTrack(track);
            }
            return;
        }
        plugin.sendMessage(player,Error.FAILED_TO_CREATE_EVENT);
    }

    @CommandPermission("event.admin")
    @Subcommand("delete")
    @CommandCompletion("@event")
    public static void onRemove(Player player, Event event) {
        if (EventDatabase.removeEvent(event)) {
            plugin.sendMessage(player, Success.REMOVED_EVENT, "%event%", event.getDisplayName());
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_REMOVE_EVENT);
    }

    @Subcommand("select")
    @CommandCompletion("@event")
    public static void onSelectEvent(Player player, Event event) {
        EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
        plugin.sendMessage(player, Success.EVENT_SELECTED);
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
            plugin.sendMessage(player, Error.NO_EVENT_SELECTED);;
            return;
        }
        event.setTrack(track);
        plugin.sendMessage(player, Success.TRACK_SELECTED);
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
            plugin.sendMessage(player, Error.NO_EVENT_SELECTED);;
            return;
        }
        if (open.equalsIgnoreCase("open")) {
            event.setOpenSign(true);
            plugin.sendMessage(player, Success.SIGNS_NOW_OPEN);
        } else {
            event.setOpenSign(false);
            plugin.sendMessage(player, Success.SIGNS_NOW_CLOSED);
        }
    }

    @Subcommand("spectate")
    @CommandCompletion("@event")
    public static void onSpectate(Player player, Event event) {
        if (event.isSpectating(player.getUniqueId())) {
            event.removeSpectator(player.getUniqueId());
            plugin.sendMessage(player, Warning.NO_LONGER_SPECTATING, "%event%", event.getDisplayName());
        } else {
            event.addSpectator(player.getUniqueId());
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
            plugin.sendMessage(player, Success.SPECTATING, "%event%", event.getDisplayName());
        }
    }

    @Subcommand("sign")
    @CommandCompletion("@event")
    public static void onSignUp(Player player, Event event, @Optional String name) {
        if (name != null) {

            if (!player.hasPermission("event.sign.others") || !player.hasPermission("event.admin") || !player.isOp()) {
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                return;
            }

            TPlayer tPlayer = Database.getPlayer(name);
            if (tPlayer == null) {
                plugin.sendMessage(player, Error.PLAYER_NOT_FOUND);
                return;
            }

            if (event.isSubscribing(tPlayer.getUniqueId())) {
                if (event.getState() != Event.EventState.SETUP) {
                    plugin.sendMessage(player, Error.EVENT_ALREADY_STARTED);
                    return;
                }
                event.removeSubscriber(tPlayer.getUniqueId());
                plugin.sendMessage(player, Warning.PLAYER_NO_LONGER_SIGNED, "%player%", tPlayer.getName(), "%event%", event.getDisplayName());
            } else {

                if (event.isReserving(tPlayer.getUniqueId())) {
                    event.removeReserve(tPlayer.getUniqueId());
                }
                event.addSubscriber(tPlayer);
                EventDatabase.setPlayerSelectedEvent(tPlayer.getUniqueId(), event);
                plugin.sendMessage(player, Success.PLAYER_SIGNED, "%player%", tPlayer.getName(), "%event%", event.getDisplayName());
            }
            return;
        }

        TPlayer tPlayer = Database.getPlayer(player.getUniqueId());
        if (event.isSubscribing(player.getUniqueId())) {
            if (event.getState() != Event.EventState.SETUP) {
                plugin.sendMessage(player, Error.EVENT_ALREADY_STARTED);
                return;
            }
            event.removeSubscriber(player.getUniqueId());
            plugin.sendMessage(player, Warning.NO_LONGER_SIGNED, "%event%", event.getDisplayName());
        } else {
            if (!event.isOpenSign()) {
                if (!player.hasPermission("event.sign") || !player.hasPermission("event.admin") || !player.isOp()) {
                    plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                    return;
                }
            }

            if (event.isReserving(player.getUniqueId())) {
                event.removeReserve(player.getUniqueId());
            }
            event.addSubscriber(tPlayer);
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
            plugin.sendMessage(player, Success.SIGNED, "%event%", event.getDisplayName());
        }
    }


    @Subcommand("signs")
    public static void onListSigns(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);;
                return;
            }
        }

        Theme theme = Database.getPlayer(player).getTheme();

        int count = 1;
        player.sendMessage(Component.empty());
        var message = plugin.getText(player, Info.SIGNS_TITLE, "%event%", event.getDisplayName());

        if (player.hasPermission("event.admin") || player.hasPermission("event.sign.others")) {
            message = message.append(Component.space()).append(TextButtons.getAddButton().clickEvent(ClickEvent.suggestCommand("/event sign " + event.getDisplayName() + " ")));
        }

        player.sendMessage(message);
        player.sendMessage(Component.empty());

        if (event.getTrack() != null) {
            var sortedList = CommandRound.getSortedList(event.getSubscribers().values().stream().map(Subscriber::getTPlayer).collect(Collectors.toList()), event.getTrack());
            for (TPlayer tPlayer : sortedList) {
                var bestTime = event.getTrack().getBestFinish(tPlayer);
                player.sendMessage(theme.primary(count++ + ":").append(Component.space()).append(theme.highlight(tPlayer.getName())).append(theme.hyphen()).append(theme.highlight(bestTime == null ? "(-)" : ApiUtilities.formatAsTime(bestTime.getTime()))));
            }
        } else {
            for (Subscriber s : event.getSubscribers().values()) {
                player.sendMessage(theme.primary(count++ + ":").append(Component.space()).append(theme.highlight(s.getTPlayer().getName())));
            }
        }


        count = 1;
        message = plugin.getText(player, Info.RESERVES_TITLE, "%event%", event.getDisplayName());

        if (player.hasPermission("event.admin") || player.hasPermission("event.sign.others")) {
            message = message.append(Component.space()).append(TextButtons.getAddButton().clickEvent(ClickEvent.suggestCommand("/event reserve " + event.getDisplayName() + " ")));
        }

        player.sendMessage(message);

        if (event.getTrack() != null) {
            var sortedList = CommandRound.getSortedList(event.getReserves().values().stream().map(Subscriber::getTPlayer).collect(Collectors.toList()), event.getTrack());
            for (TPlayer tPlayer : sortedList) {
                var bestTime = event.getTrack().getBestFinish(tPlayer);
                player.sendMessage(theme.primary(count++ + ":").append(Component.space()).append(theme.highlight(tPlayer.getName())).append(theme.hyphen()).append(theme.highlight(bestTime == null ? "(-)" : ApiUtilities.formatAsTime(bestTime.getTime())))

                );
            }
        } else {
            for (Subscriber s : event.getReserves().values()) {
                player.sendMessage(theme.primary(count++ + ":").append(Component.space()).append(theme.highlight(s.getTPlayer().getName())));
            }
        }
    }

    @Subcommand("reserve")
    @CommandCompletion("@event")
    public static void onReserve(Player player, Event event, @Optional String name) {
        if (name != null) {

            if (!player.hasPermission("event.admin") || !player.isOp()) {
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                return;
            }

            TPlayer tPlayer = Database.getPlayer(name);
            if (tPlayer == null) {
                plugin.sendMessage(player, Error.PLAYER_NOT_FOUND);
                return;
            }

            if (event.isReserving(tPlayer.getUniqueId())) {
                if (event.getState() != Event.EventState.SETUP) {
                    plugin.sendMessage(player, Error.EVENT_ALREADY_STARTED);
                    return;
                }
                event.removeReserve(tPlayer.getUniqueId());

                plugin.sendMessage(player, Warning.PLAYER_NO_LONGER_RESERVE, "%player%", tPlayer.getName(), "%event%", event.getDisplayName());
            } else {
                if (event.isSubscribing(tPlayer.getUniqueId())) {
                    event.removeSubscriber(tPlayer.getUniqueId());
                }
                event.addReserve(tPlayer);
                EventDatabase.setPlayerSelectedEvent(tPlayer.getUniqueId(), event);
                plugin.sendMessage(player, Success.PLAYER_RESERVE, "%player%", tPlayer.getName(), "%event%", event.getDisplayName());
            }
            return;
        }
        var tPlayer = Database.getPlayer(player.getUniqueId());
        if (event.isReserving(player.getUniqueId())) {
            if (event.getState() != Event.EventState.SETUP) {
                plugin.sendMessage(player, Error.EVENT_ALREADY_STARTED);
                return;
            }
            event.removeReserve(player.getUniqueId());
            plugin.sendMessage(player, Warning.NO_LONGER_RESERVE, "%event%", event.getDisplayName());
        } else {
            if (event.isSubscribing(player.getUniqueId())) {
                plugin.sendMessage(player, Error.ALREADY_SIGNED, "%event%", event.getDisplayName());
                return;
            }
            event.addReserve(tPlayer);
            EventDatabase.setPlayerSelectedEvent(player.getUniqueId(), event);
            plugin.sendMessage(player, Success.RESERVE, "%event%", event.getDisplayName());
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
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);;
                return;
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {

            if (event.isSubscribing(p.getUniqueId())) {
                continue;
            }

            if (!event.isOpenSign()) {
                if (!player.hasPermission("event.sign") || !player.hasPermission("event.admin") || !player.isOp()) {
                    continue;
                }
            }
            p.sendMessage(Component.empty());
            p.sendMessage(plugin.getText(p, Broadcast.CLICK_TO_SIGN, "%event%", event.getDisplayName()).clickEvent(ClickEvent.runCommand("/event sign " + event.getDisplayName())));
            p.sendMessage(Component.empty());
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
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);;
                return;
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {

            if (event.isReserving(p.getUniqueId()) || event.isSubscribing(p.getUniqueId())) {
                continue;
            }
            p.sendMessage(Component.empty());
            p.sendMessage(plugin.getText(p, Broadcast.CLICK_TO_RESERVE, "%event%", event.getDisplayName()).clickEvent(ClickEvent.runCommand("/event reserve " + event.getDisplayName())));
            p.sendMessage(Component.empty());
        }
    }
}
