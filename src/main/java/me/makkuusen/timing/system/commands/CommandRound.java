package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.event.EventResults;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Subscriber;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.text.Error;
import me.makkuusen.timing.system.text.Info;
import me.makkuusen.timing.system.text.Success;
import me.makkuusen.timing.system.text.TextButtons;
import me.makkuusen.timing.system.text.TextUtilities;
import me.makkuusen.timing.system.text.Warning;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@CommandAlias("round")
public class CommandRound extends BaseCommand {
    public static TimingSystem plugin;
    @Default
    @Subcommand("list")
    public static void onRounds(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                return;
            }
        }
        Theme theme = Database.getPlayer(player).getTheme();
        plugin.sendMessage(player, Info.ROUNDS_TITLE);
        event.eventSchedule.listRounds(theme).forEach(player::sendMessage);
    }

    @Subcommand("create")
    @CommandCompletion("@roundType")
    @CommandPermission("event.admin")
    public static void onCreate(Player player, RoundType roundType, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                return;
            }
        }

        if (event.getTrack() == null) {
            plugin.sendMessage(player, Error.TRACK_NOT_FOUND_FOR_EVENT);
            return;
        }
        if (event.getTrack().isStage() && roundType.equals(RoundType.QUALIFICATION)) {
            plugin.sendMessage(player, Error.QUALIFICATION_NOT_SUPPORTED);
            return;
        }

        if (EventDatabase.roundNew(event, roundType, event.getEventSchedule().getRounds().size() + 1)) {
            plugin.sendMessage(player, Success.CREATED_ROUND, "%round%", roundType.name());
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_CREATE_ROUND);
    }

    @Subcommand("delete")
    @CommandCompletion("@round")
    public static void onDelete(Player player, Round round) {
        if (EventDatabase.removeRound(round)) {
            plugin.sendMessage(player, Success.REMOVED_ROUND, "%round%", round.getDisplayName());
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_REMOVE_ROUND);
    }

    @Subcommand("info")
    @CommandCompletion("@round")
    public static void onRoundInfo(Player player, Round round) {
        Theme theme = Database.getPlayer(player).getTheme();
        player.sendMessage("");
        player.sendMessage(TextButtons.getRefreshButton().clickEvent(ClickEvent.runCommand("/round info " + round.getName())).append(TextUtilities.space()).append(TextUtilities.getTitleLine(Component.text(round.getDisplayName()).color(theme.getSecondary()).append(TextUtilities.space()).append(TextUtilities.getParenthesized(round.getState().name(), theme)), theme)).append(TextUtilities.space()).append(Component.text("[View Event]").color(TextButtons.buttonColor).clickEvent(ClickEvent.runCommand("/event info " + round.getEvent().getDisplayName())).hoverEvent(TextButtons.getClickToViewHoverEvent())));

        var heatsMessage = Component.text("Heats:").color(theme.getPrimary());

        if (player.hasPermission("event.admin")) {
            heatsMessage.append(TextUtilities.tab()).append(TextButtons.getAddButton("Heat").clickEvent(ClickEvent.runCommand("/heat create " + round.getName())).hoverEvent(TextButtons.getClickToAddHoverEvent()));
        }
        player.sendMessage(heatsMessage);

        for (Heat heat : round.getHeats()) {

            var message = TextUtilities.tab().append(Component.text(heat.getName()).color(theme.getSecondary())).append(TextUtilities.tab()).append(TextButtons.getViewButton().clickEvent(ClickEvent.runCommand("/heat info " + heat.getName())).hoverEvent(TextButtons.getClickToViewHoverEvent()));

            if (player.hasPermission("event.admin")) {
                message = message.append(TextUtilities.space()).append(TextButtons.getRemoveButton().clickEvent(ClickEvent.suggestCommand("/heat delete " + heat.getName())));
            }

            player.sendMessage(message);
        }
    }


    @Subcommand("finish")
    @CommandPermission("event.admin")
    public static void onRoundFinish(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                return;
            }
        }
        if (event.eventSchedule.getRound().get().finish(event)) {
            plugin.sendMessage(player, Success.ROUND_FINISHED);
        } else {
            plugin.sendMessage(player, Error.FAILED_TO_FINISH_ROUND);
        }
    }

    @Subcommand("results")
    @CommandCompletion("@round")
    public static void onRoundResults(Player player, Round round, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                return;
            }
        }
        List<Driver> results = EventResults.generateRoundResults(round.getHeats());

        if (results.size() != 0) {
            Theme theme = Database.getPlayer(player).getTheme();
            plugin.sendMessage(player, Info.ROUND_RESULT_TITLE, "%round%", round.getDisplayName());
            int pos = 1;
            if (round instanceof FinalRound) {
                for (Driver d : results) {
                    player.sendMessage(TextUtilities.primary(pos++ + ".", theme).append(TextUtilities.space()).append(TextUtilities.highlight(d.getTPlayer().getName(), theme)).append(TextUtilities.hyphen(theme)).append(TextUtilities.highlight(String.valueOf(d.getLaps().size()), theme)).append(TextUtilities.primary("laps in", theme)).append(Component.space()).append(TextUtilities.highlight(ApiUtilities.formatAsTime(d.getFinishTime()), theme)));
                }
            } else {
                for (Driver d : results) {
                    player.sendMessage(TextUtilities.primary(pos++ + ".", theme).append(TextUtilities.space()).append(TextUtilities.highlight(d.getTPlayer().getName(), theme)).append(TextUtilities.hyphen(theme)).append(TextUtilities.highlight(d.getBestLap().isPresent() ? ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) : "0", theme)));
                }
            }
        } else {
            plugin.sendMessage(player, Error.ROUND_NOT_FINISHED);
        }
    }

    @Subcommand("removeDriversFromRound")
    @CommandPermission("event.admin")
    public static void onRemoveDriversFromHeats(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                return;
            }
        }
        java.util.Optional<Round> maybeRound;
        if (event.eventSchedule.getCurrentRound() == null && event.eventSchedule.getRounds().size() > 0) {
            maybeRound = event.eventSchedule.getRound(1);
        } else {
            maybeRound = event.eventSchedule.getRound();
        }
        if (maybeRound.isPresent()) {
            Round round = maybeRound.get();

            for (Heat h : round.getHeats()) {
                if (h.getHeatState() != HeatState.SETUP) {
                    plugin.sendMessage(player, Error.FAILED_TO_REMOVE_DRIVERS);
                    return;
                }

                List<Driver> drivers = new ArrayList<>(h.getDrivers().values());
                for (Driver d : drivers) {
                    h.removeDriver(d);
                }
            }
            plugin.sendMessage(player, Success.REMOVED_DRIVERS);
        }
    }


    @Subcommand("fillheats")
    @CommandPermission("event.admin")
    @CommandCompletion("random|sorted all|signed|reserves")
    public static void onFillHeats(Player player, String sort, String group) {
        Event event;
        var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
        if (maybeEvent.isPresent()) {
            event = maybeEvent.get();
        } else {
            plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
            return;
        }

        boolean random = sort.equalsIgnoreCase("random");

        if (event.getState() != Event.EventState.SETUP) {
            plugin.sendMessage(player, Error.EVENT_ALREADY_STARTED);
            return;
        }

        java.util.Optional<Round> maybeRound;
        if (event.eventSchedule.getCurrentRound() == null && event.eventSchedule.getRounds().size() > 0) {
            maybeRound = event.eventSchedule.getRound(1);
        } else {
            maybeRound = event.eventSchedule.getRound();
        }
        if (maybeRound.isPresent()) {
            Round round = maybeRound.get();
            var heats = round.getHeats();
            int numberOfDrivers = event.getSubscribers().size();

            int numberOfSlots = heats.stream().mapToInt(Heat::getMaxDrivers).sum();

            LinkedList<TPlayer> tPlayerList = new LinkedList<>();
            LinkedList<TPlayer> excludedList = new LinkedList<>();

            List<TPlayer> listOfSubscribers = new ArrayList<>();
            if (group.equalsIgnoreCase("all")) {
                listOfSubscribers.addAll(event.getSubscribers().values().stream().map(Subscriber::getTPlayer).toList());
                int reserveSlots = numberOfSlots - numberOfDrivers;
                var reserves = event.getReserves().values().stream().map(Subscriber::getTPlayer).collect(Collectors.toList());
                if (reserveSlots > 0 && event.getReserves().values().size() > 0) {
                    List<TPlayer> list;
                    if (!random) {
                        list = getSortedList(reserves, event.getTrack());
                    } else {
                        list = getRandomList(reserves);
                    }
                    int count = Integer.min(reserveSlots, event.getReserves().values().size());
                    for (int i = 0; i < count; i++) {
                        listOfSubscribers.add(list.remove(0));
                    }
                    excludedList.addAll(list);
                } else {
                    excludedList.addAll(reserves);
                }
            } else {
                var subscriberMap = group.equalsIgnoreCase("signed") ? event.getSubscribers() : event.getReserves();
                listOfSubscribers.addAll(subscriberMap.values().stream().map(Subscriber::getTPlayer).toList());
            }
            Collections.shuffle(listOfSubscribers);

            if (!random) {
                tPlayerList.addAll(getSortedList(listOfSubscribers, event.getTrack()));
            } else {
                tPlayerList.addAll(getRandomList(listOfSubscribers));
            }

            for (Heat heat : heats) {
                plugin.sendMessage(player,Success.ADDING_DRIVERS, "%heat%", heat.getName());
                int size = heat.getMaxDrivers() - heat.getDrivers().size();
                for (int i = 0; i < size; i++) {
                    if (tPlayerList.size() < 1) {
                        break;
                    }
                    heatAddDriver(player, tPlayerList.pop(), heat, random);
                }
            }

            tPlayerList.addAll(excludedList);

            if (!tPlayerList.isEmpty()) {
                plugin.sendMessage(player, Warning.DRIVERS_LEFT_OUT);
                StringBuilder message = new StringBuilder();
                message.append(tPlayerList.pop().getName());

                while (!tPlayerList.isEmpty()) {
                    message.append(", ").append(tPlayerList.pop().getName());
                }
                Theme theme = Database.getPlayer(player).getTheme();
                player.sendMessage(TextUtilities.warning(message.toString(), theme));
            }
        } else {
            plugin.sendMessage(player, Error.ROUND_NOT_FOUND);
        }
    }

    public static List<TPlayer> getSortedList(List<TPlayer> players, Track track) {
        List<TPlayer> tPlayerList = new ArrayList<>();
        List<TimeTrialFinish> driversWithBestTimes = track.getTopList().stream().filter(tt -> players.contains(tt.getPlayer())).toList();
        for (var finish : driversWithBestTimes) {
            tPlayerList.add(finish.getPlayer());
        }

        for (var subscriber : players) {
            if (!tPlayerList.contains(subscriber)) {
                tPlayerList.add(subscriber);
            }
        }

        return tPlayerList;
    }

    public static List<TPlayer> getRandomList(List<TPlayer> players) {
        List<TPlayer> tPlayerList = new ArrayList<>();
        for (var subscriber : players) {
            if (!tPlayerList.contains(subscriber)) {
                tPlayerList.add(subscriber);
            }
        }
        Collections.shuffle(tPlayerList);
        return tPlayerList;
    }


    public static void heatAddDriver(Player sender, TPlayer tPlayer, Heat heat, boolean random) {
        if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
            return;
        }

        for (Heat h : heat.getRound().getHeats()) {
            if (h.getDrivers().get(tPlayer.getUniqueId()) != null) {
                return;
            }
        }

        if (EventDatabase.heatDriverNew(tPlayer.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
            var bestTime = heat.getEvent().getTrack().getBestFinish(tPlayer);
            Theme theme = Database.getPlayer(sender).getTheme();
            sender.sendMessage(TextUtilities.primary(heat.getDrivers().size() + ":", theme).append(TextUtilities.space()).append(TextUtilities.highlight(tPlayer.getName(), theme)).append(TextUtilities.hyphen(theme)).append(TextUtilities.highlight(bestTime == null ? "(-)" : ApiUtilities.formatAsTime(bestTime.getTime()), theme)));
        }

    }

}
