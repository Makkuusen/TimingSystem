package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
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
import me.makkuusen.timing.system.text.Errors;
import me.makkuusen.timing.system.text.TextButtons;
import me.makkuusen.timing.system.text.TextUtilities;
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

    @Default
    @Subcommand("list")
    public static void onRounds(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage(Errors.NO_EVENT_SELECTED.message());
                return;
            }
        }
        var messages = event.eventSchedule.getRoundList(event);
        messages.forEach(message -> player.sendMessage(message));
        return;
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
                player.sendMessage(Errors.NO_EVENT_SELECTED.message());
                return;
            }
        }

        if (event.getTrack() == null) {
            player.sendMessage("§cYou need to select a track first");
            return;
        }
        if (event.getTrack().isStage() && roundType.equals(RoundType.QUALIFICATION)) {
            player.sendMessage("§cThis track does not support qualification");
            return;
        }

        if (EventDatabase.roundNew(event, roundType, event.getEventSchedule().getRounds().size() + 1)) {
            player.sendMessage("§aCreated " + roundType.name() + " round.");
            return;
        }
        player.sendMessage("§cCould not create new round");
    }

    @Subcommand("delete")
    @CommandCompletion("@round")
    public static void onDelete(Player player, Round round) {
        if (EventDatabase.removeRound(round)){
            player.sendMessage("§a" + round.getDisplayName() + " was removed.");
            return;
        }
        player.sendMessage("§c" + round.getDisplayName() + " could not be removed");
    }

    @Subcommand("info")
    @CommandCompletion("@round")
    public static void onRoundInfo(Player player, Round round) {

        player.sendMessage("");
        player.sendMessage(TextButtons.getRefreshButton().clickEvent(ClickEvent.runCommand("/round info " + round.getName()))
                .append(TextUtilities.space())
                .append(TextUtilities.getTitleLine(
                        Component.text(round.getDisplayName()).color(TextUtilities.textHighlightColor)
                                .append(TextUtilities.space())
                                .append(TextUtilities.getParenthisied(round.getState().name())))
                )
                .append(TextUtilities.space())
                .append(Component.text("[View Event]").color(TextButtons.buttonColor).clickEvent(ClickEvent.runCommand("/event info " + round.getEvent().getDisplayName())).hoverEvent(TextButtons.getClickToViewHoverEvent()))
        );

        var heatsMessage = Component.text("Heats:").color(TextUtilities.textDarkColor);

        if (player.hasPermission("event.admin")) {
            heatsMessage.append(TextUtilities.tab())
                    .append(TextButtons.getAddButton("Heat").clickEvent(ClickEvent.runCommand("/heat create " + round.getName())).hoverEvent(TextButtons.getClickToAddHoverEvent()));
        }
        player.sendMessage(heatsMessage);

        for (Heat heat : round.getHeats()) {

            var message = TextUtilities.tab()
                    .append(Component.text(heat.getName()).color(TextUtilities.textHighlightColor))
                    .append(TextUtilities.tab())
                    .append(TextButtons.getViewButton().clickEvent(ClickEvent.runCommand("/heat info " + heat.getName())).hoverEvent(TextButtons.getClickToViewHoverEvent()));

            if (player.hasPermission("event.admin")) {
                message = message.append(TextUtilities.space())
                        .append(TextButtons.getRemoveButton().clickEvent(ClickEvent.suggestCommand("/heat delete " + heat.getName())));
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
                player.sendMessage(Errors.NO_EVENT_SELECTED.message());
                return;
            }
        }
        if (event.eventSchedule.getRound().get().finish(event)) {
            player.sendMessage("§aRound has been finished!");
        } else {
            player.sendMessage("§cRound could not be finished");
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
                player.sendMessage(Errors.NO_EVENT_SELECTED.message());
                return;
            }
        }
        List<Driver> results = EventResults.generateRoundResults(round.getHeats());

        if (results.size() != 0) {
            player.sendMessage(TextUtilities.getTitleLine("Round results for event", event.getDisplayName()));
            int pos = 1;
            if (round instanceof FinalRound){
                for (Driver d : results) {
                    player.sendMessage(TextUtilities.dark(pos++ + ".")
                            .append(TextUtilities.space())
                            .append(TextUtilities.highlight(d.getTPlayer().getName()))
                            .append(TextUtilities.hyphen())
                            .append(TextUtilities.highlight(String.valueOf(d.getLaps().size())))
                            .append(TextUtilities.dark("laps in"))
                            .append(Component.space())
                            .append(TextUtilities.highlight(ApiUtilities.formatAsTime(d.getFinishTime())))
                    );
                }
            } else {
                for (Driver d : results) {
                    player.sendMessage(TextUtilities.dark(pos++ + ".")
                            .append(TextUtilities.space())
                            .append(TextUtilities.highlight(d.getTPlayer().getName()))
                            .append(TextUtilities.hyphen())
                            .append(TextUtilities.highlight((d.getBestLap().isPresent() ? ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) : "0")))
                    );
                }
            }
        } else {
            player.sendMessage("§cRound has not been finished");
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
                player.sendMessage(Errors.NO_EVENT_SELECTED.message());
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
                    player.sendMessage(TextUtilities.error("Drivers can not be removed from " + h.getName() + " because it is either running or finished."));
                    return;
                }

                List<Driver> drivers = new ArrayList<>();
                drivers.addAll(h.getDrivers().values());
                for (Driver d : drivers) {
                    h.removeDriver(d);
                }
            }
            player.sendMessage(TextUtilities.success("Drivers have been removed!"));

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
            player.sendMessage(Errors.NO_EVENT_SELECTED.message());
            return;
        }

        boolean random = sort.equalsIgnoreCase("random");

        if (event.getState() != Event.EventState.SETUP) {
            player.sendMessage(TextUtilities.error("Event has already been started and drivers can't be distributed"));
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
                listOfSubscribers.addAll(event.getSubscribers().values().stream().map(Subscriber::getTPlayer).collect(Collectors.toList()));
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
                listOfSubscribers.addAll(subscriberMap.values().stream().map(Subscriber::getTPlayer).collect(Collectors.toList()));
            }
            Collections.shuffle(listOfSubscribers);

            if (!random) {
                tPlayerList.addAll(getSortedList(listOfSubscribers, event.getTrack()));
            } else {
                tPlayerList.addAll(getRandomList(listOfSubscribers));
            }

            for (Heat heat : heats) {
                player.sendMessage(TextUtilities.getTitleLine("Adding drivers to", heat.getName()));
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
                player.sendMessage(TextUtilities.warn("Drivers left out: "));
                String message = "";
                message += tPlayerList.pop().getName();

                while (!tPlayerList.isEmpty()) {
                    message += ", " + tPlayerList.pop().getName();
                }
                player.sendMessage(TextUtilities.warn(message));
            }
        } else {
            player.sendMessage(TextUtilities.error("Round could not be found"));
        }
    }

    public static List<TPlayer> getSortedList(List<TPlayer> players, Track track) {
        List<TPlayer> tPlayerList = new ArrayList<>();
        List<TimeTrialFinish> driversWithBestTimes = track.getTopList().stream().filter(tt -> players.contains(tt.getPlayer())).collect(Collectors.toList());
        for (var finish : driversWithBestTimes) {
            tPlayerList.add(finish.getPlayer());
        }

        for (var subscriber : players){
            if (!tPlayerList.contains(subscriber)){
                tPlayerList.add(subscriber);
            }
        }

        return tPlayerList;
    }

    public static List<TPlayer> getRandomList(List<TPlayer> players) {
        List<TPlayer> tPlayerList = new ArrayList<>();
        for (var subscriber : players){
            if (!tPlayerList.contains(subscriber)){
                tPlayerList.add(subscriber);
            }
        }
        Collections.shuffle(tPlayerList);
        return tPlayerList;
    }



    public static boolean heatAddDriver(Player sender, TPlayer tPlayer, Heat heat, boolean random) {
        if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
            return false;
        }

        for (Heat h : heat.getRound().getHeats()) {
            if (h.getDrivers().get(tPlayer.getUniqueId()) != null) {
                return false;
            }
        }

        if (EventDatabase.heatDriverNew(tPlayer.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
            var bestTime = heat.getEvent().getTrack().getBestFinish(tPlayer);
            sender.sendMessage(TextUtilities.dark(heat.getDrivers().size() + ":")
                    .append(TextUtilities.space())
                    .append(TextUtilities.highlight(tPlayer.getName()))
                    .append(TextUtilities.hyphen())
                    .append(TextUtilities.highlight((bestTime == null ? "(None)" : ApiUtilities.formatAsTime(bestTime.getTime()))))
            );
            return true;
        }

        return false;
    }

}
