package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.event.EventResults;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Subscriber;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
                player.sendMessage("§cYou have no event selected");
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
                player.sendMessage("§cYou have no event selected");
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
    public static void onHeatInfo(Player player, Round round) {
        player.sendMessage("§2Round: §a" + round.getDisplayName());
        player.sendMessage("§2Roundtype: §a" + round.getType().name());
        player.sendMessage("§2Roundstate: §a" + round.getState().name());
        player.sendMessage("§2Heats: " + round.getHeats().size());
    }


    @Subcommand("finish")
    @CommandPermission("event.admin")
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

    @Subcommand("results")
    @CommandCompletion("@round")
    public static void onRoundResults(Player player, Round round, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        List<Driver> results = EventResults.generateRoundResults(round.getHeats());

        if (results.size() != 0) {
            player.sendMessage("§2Round results for event §a" + event.getDisplayName());
            int pos = 1;
            if (round instanceof FinalRound){
                for (Driver d : results) {
                    player.sendMessage("§2" + pos++ + ". §a" + d.getTPlayer().getName() + "§2 - §a" + d.getLaps().size() + " §2laps in §a" + ApiUtilities.formatAsTime(d.getFinishTime()));
                }
            } else {
                for (Driver d : results) {
                    player.sendMessage("§2" + pos++ + ". §a" + d.getTPlayer().getName() + "§2 - §a" + (d.getBestLap().isPresent() ? ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) : "0"));
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
                player.sendMessage("§cYou have no event selected");
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
                    player.sendMessage("§cDrivers can not be removed from " + h.getName() + " because it is either running or finished.");
                    return;
                }

                List<Driver> drivers = new ArrayList<>();
                drivers.addAll(h.getDrivers().values());
                for (Driver d : drivers) {
                    h.removeDriver(d);
                }
            }
            player.sendMessage("§aDrivers have been removed!");

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
            player.sendMessage("§cYou have no event selected");
            return;
        }

        boolean random = sort.equalsIgnoreCase("random");

        if (event.getState() != Event.EventState.SETUP) {
            player.sendMessage("§cEvent has already been started and drivers can't be distributed");
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
                if (reserveSlots > 0) {
                    List<TPlayer> list;
                    if (!random) {
                        list = getSortedList(reserves, event.getTrack());
                    } else {
                        list = getRandomList(reserves);
                    }
                    for (int i = 0; i < reserveSlots; i++) {
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
                player.sendMessage("§2--- Adding drivers to §a" + heat.getName() + " §2---");
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
                player.sendMessage("§6Drivers left out: ");
                String message = "§e";
                message += tPlayerList.pop().getName();

                while (!tPlayerList.isEmpty()) {
                    message += "§6, §e" + tPlayerList.pop().getName();
                }
                player.sendMessage(message);
            }
        } else {
            player.sendMessage("§cRound could not be found");
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
            sender.sendMessage("§2" + heat.getDrivers().size() + ": §a" + tPlayer.getName() + (bestTime == null ? "§2 - §a(None)" : "§2 - §a" + ApiUtilities.formatAsTime(bestTime.getTime())));
            return true;
        }

        return false;
    }

}
