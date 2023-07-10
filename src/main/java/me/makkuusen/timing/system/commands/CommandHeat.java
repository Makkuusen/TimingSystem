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
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.event.EventResults;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.text.Error;
import me.makkuusen.timing.system.text.Success;
import me.makkuusen.timing.system.text.TextButtons;
import me.makkuusen.timing.system.text.TextUtilities;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@CommandAlias("heat")
public class CommandHeat extends BaseCommand {
    public static TimingSystem plugin;

    @Default
    @Subcommand("list")
    public static void onHeats(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                plugin.sendMessage(player, Error.NO_EVENT_SELECTED);
                return;
            }
        }
        var messages = event.eventSchedule.getHeatList(event);
        messages.forEach(player::sendMessage);
    }

    @Subcommand("info")
    @CommandCompletion("@heat")
    public static void onHeatInfo(Player player, Heat heat) {
        player.sendMessage(Component.empty());
        player.sendMessage(TextButtons.getRefreshButton().clickEvent(ClickEvent.runCommand("/heat info " + heat.getName())).append(TextUtilities.space()).append(TextUtilities.getTitleLine(Component.text(heat.getName()).color(TextUtilities.textHighlightColor).append(TextUtilities.space()).append(TextUtilities.getParenthesized(heat.getHeatState().name())))).append(TextUtilities.space()).append(Component.text("[View Event]").color(TextButtons.buttonColor).clickEvent(ClickEvent.runCommand("/event info " + heat.getEvent().getDisplayName())).hoverEvent(TextButtons.getClickToViewHoverEvent())));

        if (player.hasPermission("event.admin") && heat.getHeatState() != HeatState.FINISHED) {
            player.sendMessage(Component.text("[Load]").color(NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/heat load " + heat.getName())).hoverEvent(HoverEvent.showText(Component.text("Click to load heat"))).append(Component.space()).append(Component.text("[Reset]").color(NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/heat reset " + heat.getName())).hoverEvent(HoverEvent.showText(Component.text("Click to reset heat")))).append(Component.space()).append(Component.text("[Start]").color(NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/heat start " + heat.getName())).hoverEvent(HoverEvent.showText(Component.text("Click to start heat")))).append(Component.space()).append(Component.text("[Finish]").color(NamedTextColor.GRAY).clickEvent(ClickEvent.runCommand("/heat finish " + heat.getName())).hoverEvent(HoverEvent.showText(Component.text("Click to finish heat")))));
        }

        if (heat.getTimeLimit() != null) {
            var message = Component.text("Time limit: ").color(TextUtilities.textDarkColor);

            if (!heat.isFinished() && player.hasPermission("event.admin")) {
                message = message.append(TextButtons.getEditButton((heat.getTimeLimit() / 1000) + "s").clickEvent(ClickEvent.suggestCommand("/heat set timelimit " + heat.getName() + " ")));
            } else {
                message = message.append(TextUtilities.highlight((heat.getTimeLimit() / 1000) + "s"));
            }
            player.sendMessage(message);
        }
        if (heat.getStartDelay() != null) {
            var message = Component.text("Start delay: ").color(TextUtilities.textDarkColor);

            if (!heat.isFinished() && player.hasPermission("event.admin")) {
                message = message.append(TextButtons.getEditButton((heat.getStartDelay()) + "ms").clickEvent(ClickEvent.suggestCommand("/heat set startdelay " + heat.getName() + " ")));
            } else {
                message = message.append(TextUtilities.highlight((heat.getStartDelay()) + "ms"));
            }
            player.sendMessage(message);
        }

        if (heat.getTotalLaps() != null) {
            var message = Component.text("Laps: ").color(TextUtilities.textDarkColor);

            if (!heat.isFinished() && player.hasPermission("event.admin")) {
                message = message.append(TextButtons.getEditButton(String.valueOf(heat.getTotalLaps())).clickEvent(ClickEvent.suggestCommand("/heat set laps " + heat.getName() + " ")));
            } else {
                message = message.append(TextUtilities.highlight(String.valueOf(heat.getTotalLaps())));
            }
            player.sendMessage(message);
        }
        if (heat.getTotalPits() != null) {
            var message = Component.text("Pits: ").color(TextUtilities.textDarkColor);

            if (!heat.isFinished() && player.hasPermission("event.admin")) {
                message = message.append(TextButtons.getEditButton(String.valueOf(heat.getTotalPits())).clickEvent(ClickEvent.suggestCommand("/heat set pits " + heat.getName() + " ")));
            } else {
                message = message.append(TextUtilities.highlight(String.valueOf(heat.getTotalPits())));
            }
            player.sendMessage(message);
        }

        var maxDriversMessage = Component.text("Max drivers: ").color(TextUtilities.textDarkColor);

        if (!heat.isFinished() && player.hasPermission("event.admin")) {
            maxDriversMessage = maxDriversMessage.append(TextButtons.getEditButton(String.valueOf(heat.getMaxDrivers())).clickEvent(ClickEvent.suggestCommand("/heat set maxdrivers " + heat.getName() + " ")));
        } else {
            maxDriversMessage = maxDriversMessage.append(TextUtilities.highlight(String.valueOf(heat.getMaxDrivers())));
        }
        player.sendMessage(maxDriversMessage);

        if (heat.getFastestLapUUID() != null) {
            Driver d = heat.getDrivers().get(heat.getFastestLapUUID());
            player.sendMessage(TextUtilities.dark("Fastest lap:").append(TextUtilities.space()).append(TextUtilities.highlight(ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()))).append(TextUtilities.space()).append(TextUtilities.dark("by")).append(TextUtilities.space()).append(TextUtilities.highlight(d.getTPlayer().getName())));
        }

        var driverMessage = Component.text("Drivers:").color(TextUtilities.textDarkColor);

        if (!heat.isFinished() && player.hasPermission("event.admin")) {
            driverMessage = driverMessage.append(TextUtilities.space()).append(TextButtons.getAddButton().clickEvent(ClickEvent.suggestCommand("/heat add " + heat.getName() + " ")));
        }

        player.sendMessage(driverMessage);

        for (Driver d : heat.getStartPositions()) {
            var message = TextUtilities.tab().append(Component.text(d.getStartPosition() + ": " + d.getTPlayer().getName()).color(NamedTextColor.WHITE));

            if (!heat.isFinished() && player.hasPermission("event.admin")) {
                message = message.append(TextUtilities.tab()).append(TextButtons.getMoveButton().clickEvent(ClickEvent.suggestCommand("/heat set driverposition " + heat.getName() + " " + d.getTPlayer().getName() + " ")).hoverEvent(HoverEvent.showText(Component.text("Change position")))).append(Component.space()).append(TextButtons.getRemoveButton().clickEvent(ClickEvent.suggestCommand("/heat delete driver " + heat.getName() + " " + d.getTPlayer().getName())));
            }

            player.sendMessage(message);
        }
    }

    @Subcommand("start")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatStart(Player player, Heat heat) {
        if (heat.startCountdown()) {
            plugin.sendMessage(player, Success.HEAT_COUNTDOWN_STARTED);
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_START_HEAT);
    }

    @Subcommand("finish")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatFinish(Player player, Heat heat) {
        if (heat.finishHeat()) {
            plugin.sendMessage(player, Success.HEAT_FINISHED);
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_FINISH_HEAT);
    }

    @Subcommand("load")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatLoad(Player player, Heat heat) {

        var state = heat.getHeatState();
        if (state != HeatState.SETUP) {
            if (!heat.resetHeat()) {
                plugin.sendMessage(player, Error.FAILED_TO_RESET_HEAT);
                return;
            }
        }

        if (heat.loadHeat()) {
            if (state == HeatState.SETUP) {
                EventAnnouncements.broadcastSpectate(heat.getEvent());
            }
            plugin.sendMessage(player, Success.HEAT_LOADED);
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_LOAD_HEAT);

    }

    @Subcommand("reset")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatReset(Player player, Heat heat) {
        if (heat.resetHeat()) {
            EventAnnouncements.broadcastReset(heat);
            plugin.sendMessage(player, Success.HEAT_RESET);
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_RESET_HEAT);
    }

    @Subcommand("delete")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatRemove(Player player, Heat heat) {
        if (EventDatabase.removeHeat(heat)) {
            plugin.sendMessage(player, Success.REMOVED_HEAT, "%heat%", heat.getName());
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_REMOVE_HEAT);
    }

    @Subcommand("create")
    @CommandCompletion("@round")
    @CommandPermission("event.admin")
    public static void onHeatCreate(Player player, Round round, @Optional Event event) {
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
        round.createHeat(round.getHeats().size() + 1);
        plugin.sendMessage(player, Success.CREATED_HEAT, "%round%", round.getDisplayName());
    }

    @Subcommand("set laps")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat <laps>")
    public static void onHeatSetLaps(Player player, Heat heat, Integer laps) {
        heat.setTotalLaps(laps);
        plugin.sendMessage(player, Success.SAVED);
    }

    @Subcommand("set pits")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat <pits>")
    public static void onHeatSetPits(Player player, Heat heat, Integer pits) {
        if (heat.getRound() instanceof QualificationRound) {
            plugin.sendMessage(player, Error.CAN_NOT);
        } else {
            heat.setTotalPits(pits);
            plugin.sendMessage(player, Success.SAVED);
        }
    }

    @Subcommand("set startdelay")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat <h/m/s>")
    public static void onHeatStartDelay(Player player, Heat heat, String startDelay) {
        Integer delay = ApiUtilities.parseDurationToMillis(startDelay);
        if (delay == null) {
            plugin.sendMessage(player, Error.TIME_FORMAT);
            return;
        }
        heat.setStartDelayInTicks(delay);
        plugin.sendMessage(player, Success.SAVED);

    }

    @Subcommand("set timelimit")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat <h/m/s>")
    public static void onHeatSetTime(Player player, Heat heat, String time) {
        Integer timeLimit = ApiUtilities.parseDurationToMillis(time);
        if (timeLimit == null) {
            plugin.sendMessage(player, Error.TIME_FORMAT);
            return;
        }
        heat.setTimeLimit(timeLimit);
        plugin.sendMessage(player, Success.SAVED);
    }

    @Subcommand("set maxdrivers")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat <max>")
    public static void onHeatMaxDrivers(Player player, Heat heat, Integer maxDrivers) {
        heat.setMaxDrivers(maxDrivers);
        plugin.sendMessage(player, Success.SAVED);
    }

    @Subcommand("set driverposition")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat @players <[+/-]pos>")
    public static void onHeatSetDriverPosition(Player sender, Heat heat, String playerName, String position) {
        TPlayer tPlayer = Database.getPlayer(playerName);
        if (tPlayer == null) {
            plugin.sendMessage(sender, Error.PLAYER_NOT_FOUND);
            return;
        }
        if (heat.getDrivers().get(tPlayer.getUniqueId()) == null) {
            plugin.sendMessage(sender, Error.PLAYER_NOT_FOUND);
            return;
        }
        Driver driver = heat.getDrivers().get(tPlayer.getUniqueId());
        if (heat.isRacing()) {
            plugin.sendMessage(sender, Error.HEAT_ALREADY_STARTED);
            return;
        }
        if (getParsedIndex(position) == null) {
            plugin.sendMessage(sender, Error.NUMBER_FORMAT);
            return;
        }
        int parsedIndex = Objects.requireNonNull(getParsedIndex(position));
        int pos;
        if (getParsedRemoveFlag(position)) {
            pos = driver.getStartPosition() - parsedIndex;
        } else if (getParsedAddFlag(position)) {
            pos = driver.getStartPosition() + parsedIndex;
        } else {
            pos = parsedIndex;
        }

        if (pos > heat.getDrivers().size()) {
            plugin.sendMessage(sender, Error.CAN_NOT);
            return;
        }

        if (pos < 1) {
            plugin.sendMessage(sender, Error.CAN_NOT);
            return;
        }

        if (pos == driver.getStartPosition()) {
            plugin.sendMessage(sender, Error.CAN_NOT);
            return;
        }


        if (heat.setDriverPosition(driver, pos)) {
            plugin.sendMessage(sender, Success.DRIVER_NEW_START_POSITION, "%driver%", driver.getTPlayer().getName(), "%pos%", String.valueOf(pos));
            if (heat.getHeatState() == HeatState.LOADED) {
                heat.reloadHeat();
            }
            return;
        }
        plugin.sendMessage(sender, Error.GENERIC);

    }

    @Subcommand("set reversegrid")
    @CommandCompletion("@heat <%>")
    public static void onReverseGrid(Player player, Heat heat, @Optional Integer percentage) {
        if (percentage == null) {
            percentage = 100;
        }
        heat.reverseGrid(percentage);
        if (heat.getHeatState() == HeatState.LOADED) {
            heat.reloadHeat();
        }
        plugin.sendMessage(player, Success.HEAT_REVERSED_GRID, "%percent%", String.valueOf(percentage));
    }

    @Subcommand("add")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat @players ")
    public static void onHeatAddDriver(Player sender, Heat heat, String playerName) {
        if (heat.getRound().getRoundIndex() != heat.getEvent().getEventSchedule().getCurrentRound() && heat.getRound().getRoundIndex() != 1) {
            plugin.sendMessage(sender, Error.ADD_DRIVER_FUTURE_ROUND);
            return;
        }

        if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
            plugin.sendMessage(sender, Error.HEAT_FULL);
            return;
        }
        TPlayer tPlayer = Database.getPlayer(playerName);
        if (tPlayer == null) {
            plugin.sendMessage(sender, Error.PLAYER_NOT_FOUND);
            return;
        }

        for (Heat h : heat.getRound().getHeats()) {
            if (h.getDrivers().get(tPlayer.getUniqueId()) != null) {
                plugin.sendMessage(sender, Error.PLAYER_ALREADY_IN_ROUND);
                return;
            }
        }

        if (EventDatabase.heatDriverNew(tPlayer.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
            plugin.sendMessage(sender, Success.ADDED_DRIVER);
            if (heat.getHeatState() == HeatState.LOADED) {
                heat.addDriverToGrid(heat.getDrivers().get(tPlayer.getUniqueId()));
            }
            return;
        }

        plugin.sendMessage(sender, Error.FAILED_TO_ADD_DRIVER);
    }


    @Subcommand("delete driver")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat @players")
    public static void onHeatRemoveDriver(Player sender, Heat heat, String playerName) {
        TPlayer tPlayer = Database.getPlayer(playerName);
        if (tPlayer == null) {
            plugin.sendMessage(sender, Error.PLAYER_NOT_FOUND);
            return;
        }
        if (heat.getDrivers().get(tPlayer.getUniqueId()) == null) {
            plugin.sendMessage(sender, Error.PLAYER_NOT_FOUND);
            return;
        }
        if (heat.isRacing()) {
            if (heat.disqualifyDriver(heat.getDrivers().get(tPlayer.getUniqueId()))) {
                if (tPlayer.getPlayer() != null) {
                    if (tPlayer.getPlayer().getVehicle() != null && tPlayer.getPlayer().getVehicle() instanceof Boat boat) {
                        boat.remove();
                    }
                    Location loc = tPlayer.getPlayer().getBedSpawnLocation() == null ? tPlayer.getPlayer().getWorld().getSpawnLocation() : tPlayer.getPlayer().getBedSpawnLocation();
                    tPlayer.getPlayer().teleport(loc);
                }
                plugin.sendMessage(sender, Success.DRIVER_DISQUALIFIED);
                return;
            }
           plugin.sendMessage(sender, Error.FAILED_TO_DISQUALIFY_DRIVER);
        } else {
            boolean reload = false;
            if (heat.getHeatState() == HeatState.LOADED) {
                heat.resetHeat();
                reload = true;
            }
            if (heat.removeDriver(heat.getDrivers().get(tPlayer.getUniqueId()))) {
                boolean removeSpectator = true;
                for (Round round : heat.getEvent().getEventSchedule().getRounds()) {
                    for (Heat h : round.getHeats()) {
                        if (h.getDrivers().containsKey(tPlayer.getUniqueId())) {
                            removeSpectator = false;
                            break;
                        }
                    }
                }
                if (removeSpectator) {
                    heat.getEvent().removeSpectator(tPlayer.getUniqueId());
                }
                plugin.sendMessage(sender, Success.DRIVER_REMOVED);
                if (reload) {
                    heat.loadHeat();
                }
                return;
            }
            plugin.sendMessage(sender,Error.FAILED_TO_REMOVE_DRIVER);
        }
    }

    @Subcommand("quit")
    public static void onHeatDriverQuit(Player player) {
        if (EventDatabase.getDriverFromRunningHeat(player.getUniqueId()).isEmpty()) {
            plugin.sendMessage(player, Error.NOT_NOW);
            return;
        }
        Driver driver = EventDatabase.getDriverFromRunningHeat(player.getUniqueId()).get();
        if (driver.getHeat().disqualifyDriver(driver)) {
            if (player.getVehicle() != null && player.getVehicle() instanceof Boat boat) {
                boat.remove();
            }
            Location loc = player.getBedSpawnLocation() == null ? player.getWorld().getSpawnLocation() : player.getBedSpawnLocation();
            player.teleport(loc);
            plugin.sendMessage(player, Success.HEAT_ABORTED);
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_ABORT_HEAT);
    }


    @Subcommand("add alldrivers")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatAddDrivers(Player sender, Heat heat) {
        if (heat.getRound().getRoundIndex() != heat.getEvent().getEventSchedule().getCurrentRound() && heat.getRound().getRoundIndex() != 1) {
            plugin.sendMessage(sender, Error.ADD_DRIVER_FUTURE_ROUND);
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
                plugin.sendMessage(sender, Error.HEAT_FULL);
                return;
            }
            boolean inOtherHeat = false;
            for (Heat h : heat.getRound().getHeats()) {
                if (h.getDrivers().get(player.getUniqueId()) != null) {
                    inOtherHeat = true;
                    break;
                }
            }
            if (inOtherHeat) {
                continue;
            }
            if (heat.getDrivers().get(player.getUniqueId()) != null) {
                continue;
            }
            if (EventDatabase.heatDriverNew(player.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
                continue;
            }
            if (heat.getHeatState() == HeatState.LOADED) {
                heat.addDriverToGrid(heat.getDrivers().get(player.getUniqueId()));
            }
        }
        plugin.sendMessage(sender, Success.ADDED_ALL_DRIVERS);
    }


    @Subcommand("results")
    @CommandCompletion("@heat @players")
    public static void onHeatResults(Player sender, Heat heat, @Optional String name) {

        if (name != null) {
            TPlayer tPlayer = Database.getPlayer(name);
            if (tPlayer == null) {
                plugin.sendMessage(sender, Error.PLAYER_NOT_FOUND);
                return;
            }
            if (heat.getDrivers().get(tPlayer.getUniqueId()) == null) {
                plugin.sendMessage(sender, Error.PLAYER_NOT_FOUND);
                return;
            }
            Driver driver = heat.getDrivers().get(tPlayer.getUniqueId());
            sender.sendMessage(TextUtilities.getTitleLine(TextUtilities.dark("Results for").append(TextUtilities.space()).append(TextUtilities.highlight(tPlayer.getName())).append(Component.space()).append(TextUtilities.dark("in")).append(Component.space()).append(TextUtilities.highlight(heat.getName()))));

            sender.sendMessage(TextUtilities.dark("Position:").append(Component.space()).append(TextUtilities.highlight(driver.getPosition().toString())));
            sender.sendMessage(TextUtilities.dark("Start position:").append(Component.space()).append(TextUtilities.highlight(String.valueOf(driver.getStartPosition()))));

            var maybeBestLap = driver.getBestLap();
            maybeBestLap.ifPresent(lap -> sender.sendMessage(TextUtilities.dark("Fastest lap:").append(Component.space()).append(TextUtilities.highlight(ApiUtilities.formatAsTime(lap.getLapTime())))));
            int count = 1;
            for (Lap l : driver.getLaps()) {
                String lap = "&dLap " + count + ": &h" + ApiUtilities.formatAsTime(l.getLapTime());
                if (l.equals(maybeBestLap.get())) {
                    lap += " &d(F)";
                }
                if (l.isPitted()) {
                    lap += " &d(P)";
                }
                sender.sendMessage(lap);
                sender.sendMessage(plugin.getText(sender, lap));
                count++;
            }
            return;
        }
        if (heat.getHeatState() == HeatState.FINISHED) {

            sender.sendMessage(TextUtilities.getTitleLine("Results for heat", heat.getName()));
            if (heat.getFastestLapUUID() != null) {
                Driver d = heat.getDrivers().get(heat.getFastestLapUUID());
                sender.sendMessage(TextUtilities.dark("Fastest lap:").append(TextUtilities.space()).append(TextUtilities.highlight(ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()))).append(TextUtilities.space()).append(TextUtilities.dark("by")).append(TextUtilities.space()).append(TextUtilities.highlight(d.getTPlayer().getName())));
            }
            List<Driver> result = EventResults.generateHeatResults(heat);
            if (heat.getRound() instanceof FinalRound) {
                for (Driver d : result) {
                    sender.sendMessage(TextUtilities.dark(d.getPosition() + ".").append(TextUtilities.space()).append(TextUtilities.highlight(d.getTPlayer().getName())).append(TextUtilities.hyphen()).append(TextUtilities.highlight(String.valueOf(d.getLaps().size()))).append(TextUtilities.dark("laps in")).append(Component.space()).append(TextUtilities.highlight(ApiUtilities.formatAsTime(d.getFinishTime()))));

                }
            } else {
                for (Driver d : result) {
                    sender.sendMessage(TextUtilities.dark(d.getPosition() + ".").append(TextUtilities.space()).append(TextUtilities.highlight(d.getTPlayer().getName())).append(TextUtilities.hyphen()).append(TextUtilities.highlight((d.getBestLap().isPresent() ? ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) : "0"))));
                }
            }
        } else {
            sender.sendMessage("§cHeat has not been finished");
        }
    }

    @Subcommand("sort tt")
    @CommandCompletion("@heat")
    @CommandPermission("event.admin")
    public static void onSortByTT(Player player, Heat heat) {
        if (heat.getHeatState() == HeatState.FINISHED) {
            plugin.sendMessage(player, Error.NOT_NOW);
            return;
        }
        if (heat.isRacing()) {
            plugin.sendMessage(player, Error.HEAT_ALREADY_STARTED);
            return;
        }
        if (heat.getStartPositions().size() == 0) {
            plugin.sendMessage(player, Error.NOT_NOW);
            return;
        }
        if (heat.getRound().getRoundIndex() != heat.getEvent().getEventSchedule().getCurrentRound() && heat.getRound().getRoundIndex() != 1) {
            plugin.sendMessage(player, Error.SORT_DRIVERS_FUTURE_ROUND);
            return;
        }

        List<TimeTrialFinish> driversWithBestTimes = heat.getEvent().getTrack().getTopList().stream().filter(tt -> heat.getDrivers().containsKey(tt.getPlayer().getUniqueId())).toList();
        List<Driver> allDrivers = new ArrayList<>(heat.getStartPositions());
        List<Driver> noTT = new ArrayList<>();

        int i = 1;
        for (Driver driver : allDrivers) {
            boolean match = false;
            for (TimeTrialFinish finish : driversWithBestTimes) {
                if (finish.getPlayer() == driver.getTPlayer()) {
                    heat.setDriverPosition(driver, driversWithBestTimes.indexOf(finish) + 1);
                    i++;
                    match = true;
                    break;
                }
            }
            if (!match) {
                noTT.add(driver);
            }
        }

        for (Driver driver : noTT) {
            heat.setDriverPosition(driver, i);
            i++;
        }

        if (heat.getHeatState() == HeatState.LOADED) {
            heat.reloadHeat();
        }

        plugin.sendMessage(player, Success.HEAT_SORTED_BY_TIME);
    }

    @Subcommand("sort random")
    @CommandCompletion("@heat")
    @CommandPermission("event.admin")
    public static void onSortByRandom(Player player, Heat heat) {
        if (heat.getHeatState() == HeatState.FINISHED) {
            plugin.sendMessage(player, Error.NOT_NOW);
            return;
        }
        if (heat.isRacing()) {
            plugin.sendMessage(player, Error.HEAT_ALREADY_STARTED);
            return;
        }
        if (heat.getStartPositions().size() == 0) {
            plugin.sendMessage(player, Error.NOT_NOW);
            return;
        }
        if (heat.getRound().getRoundIndex() != heat.getEvent().getEventSchedule().getCurrentRound() && heat.getRound().getRoundIndex() != 1) {
            plugin.sendMessage(player, Error.SORT_DRIVERS_FUTURE_ROUND);
            return;
        }

        List<Driver> randomDrivers = new ArrayList<>(heat.getStartPositions());
        Collections.shuffle(randomDrivers);

        for (int i = 0; i < randomDrivers.size(); i++) {
            heat.setDriverPosition(randomDrivers.get(i), i + 1);
        }

        if (heat.getHeatState() == HeatState.LOADED) {
            heat.reloadHeat();
        }

        plugin.sendMessage(player, Success.HEAT_SORTED_BY_RANDOM);
    }

    private static boolean getParsedRemoveFlag(String index) {
        return index.startsWith("-");
    }

    private static boolean getParsedAddFlag(String index) {
        return index.startsWith("+");
    }

    private static Integer getParsedIndex(String index) {
        if (index.startsWith("-")) {
            index = index.substring(1);
        } else if (index.startsWith("+")) {
            index = index.substring(1);
        }
        try {
            return Integer.parseInt(index);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

