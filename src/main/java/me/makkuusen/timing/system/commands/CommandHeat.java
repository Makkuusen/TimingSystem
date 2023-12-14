package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.database.EventDatabase;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.event.EventResults;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.DriverState;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Broadcast;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Hover;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.TextButton;
import me.makkuusen.timing.system.theme.messages.Word;
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

    @Default
    @Subcommand("list")
    @CommandPermission("%permissionheat_list")
    public static void onHeats(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                Text.send(player, Error.NO_EVENT_SELECTED);
                return;
            }
        }
        Text.send(player, Info.HEATS_TITLE, "%event%", event.getDisplayName());
        var messages = event.eventSchedule.getHeatList(TSDatabase.getPlayer(player).getTheme());
        messages.forEach(player::sendMessage);
    }

    @Subcommand("info")
    @CommandCompletion("@heat")
    @CommandPermission("%permissionheat_info")
    public static void onHeatInfo(Player player, Heat heat) {
        Theme theme = TSDatabase.getPlayer(player).getTheme();
        player.sendMessage(Component.empty());
        player.sendMessage(theme.getRefreshButton().clickEvent(ClickEvent.runCommand("/heat info " + heat.getName())).append(Component.space()).append(theme.getTitleLine(Component.text(heat.getName()).color(theme.getSecondary()).append(Component.space()).append(theme.getParenthesized(heat.getHeatState().name()).append(Component.space()).append(theme.getBrackets(Text.get(player, TextButton.VIEW_EVENT), theme.getButton()).clickEvent(ClickEvent.runCommand("/event info " + heat.getEvent().getDisplayName())).hoverEvent(theme.getClickToViewHoverEvent(player)))))));

        Component load = theme.getBrackets(Text.get(player, Word.LOAD), NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/heat load " + heat.getName())).hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_LOAD)));
        Component reset = theme.getBrackets(Text.get(player, Word.RESET), NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/heat reset " + heat.getName())).hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_RESET)));
        Component start = theme.getBrackets(Text.get(player, Word.START), NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/heat start " + heat.getName())).hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_START)));
        Component finish = theme.getBrackets(Text.get(player, Word.FINISH), NamedTextColor.GRAY).clickEvent(ClickEvent.runCommand("/heat finish " + heat.getName())).hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_START)));

        if (player.hasPermission("timingsystem.packs.eventadmin") && heat.getHeatState() != HeatState.FINISHED) {
            player.sendMessage(load.append(Component.space()).append(reset).append(Component.space()).append(start).append(Component.space()).append(finish));
        }

        if (heat.getTimeLimit() != null) {
            var message = Text.get(player, Info.HEAT_INFO_TIME_LIMIT);

            if (!heat.isFinished() && player.hasPermission("timingsystem.packs.eventadmin")) {
                message = message.append(theme.getEditButton(player, (heat.getTimeLimit() / 1000) + "s", theme).clickEvent(ClickEvent.suggestCommand("/heat set timelimit " + heat.getName() + " ")));
            } else {
                message = message.append(theme.highlight((heat.getTimeLimit() / 1000) + "s"));
            }
            player.sendMessage(message);
        }
        if (heat.getStartDelay() != null) {
            var message = Text.get(player, Info.HEAT_INFO_START_DELAY);

            if (!heat.isFinished() && player.hasPermission("timingsystem.packs.eventadmin")) {
                message = message.append(theme.getEditButton(player, (heat.getStartDelay()) + "ms", theme).clickEvent(ClickEvent.suggestCommand("/heat set startdelay " + heat.getName() + " ")));
            } else {
                message = message.append(theme.highlight((heat.getStartDelay()) + "ms"));
            }
            player.sendMessage(message);
        }

        if (heat.getTotalLaps() != null) {
            var message = Text.get(player, Info.HEAT_INFO_LAPS);

            if (!heat.isFinished() && player.hasPermission("timingsystem.packs.eventadmin")) {
                message = message.append(theme.getEditButton(player, String.valueOf(heat.getTotalLaps()), theme).clickEvent(ClickEvent.suggestCommand("/heat set laps " + heat.getName() + " ")));
            } else {
                message = message.append(theme.highlight(String.valueOf(heat.getTotalLaps())));
            }
            player.sendMessage(message);
        }
        if (heat.getTotalPits() != null) {
            var message = Text.get(player, Info.HEAT_INFO_PITS);

            if (!heat.isFinished() && player.hasPermission("timingsystem.packs.eventadmin")) {
                message = message.append(theme.getEditButton(player, String.valueOf(heat.getTotalPits()), theme).clickEvent(ClickEvent.suggestCommand("/heat set pits " + heat.getName() + " ")));
            } else {
                message = message.append(theme.highlight(String.valueOf(heat.getTotalPits())));
            }
            player.sendMessage(message);
        }

        var maxDriversMessage = Text.get(player, Info.HEAT_INFO_MAX_DRIVERS);

        if (!heat.isFinished() && player.hasPermission("timingsystem.packs.eventadmin")) {
            maxDriversMessage = maxDriversMessage.append(theme.getEditButton(player, String.valueOf(heat.getMaxDrivers()), theme).clickEvent(ClickEvent.suggestCommand("/heat set maxdrivers " + heat.getName() + " ")));
        } else {
            maxDriversMessage = maxDriversMessage.append(theme.highlight(String.valueOf(heat.getMaxDrivers())));
        }
        player.sendMessage(maxDriversMessage);

        if (heat.getFastestLapUUID() != null) {
            Driver d = heat.getDrivers().get(heat.getFastestLapUUID());
            player.sendMessage(Text.get(player, Info.HEAT_INFO_FASTEST_LAP, "%time%", ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()), "%player%", d.getTPlayer().getName()));
        }

        var driverMessage = Text.get(player, Info.HEAT_INFO_DRIVERS);

        if (!heat.isFinished() && player.hasPermission("timingsystem.packs.eventadmin")) {
            driverMessage = driverMessage.append(Component.space()).append(theme.getAddButton().clickEvent(ClickEvent.suggestCommand("/heat add " + heat.getName() + " ")));
        }

        player.sendMessage(driverMessage);

        for (Driver d : heat.getStartPositions()) {
            var message = theme.tab().append(Component.text(d.getStartPosition() + ": " + d.getTPlayer().getName()).color(NamedTextColor.WHITE));

            if (!heat.isFinished() && player.hasPermission("timingsystem.packs.eventadmin")) {
                message = message.append(theme.tab()).append(theme.getMoveButton().clickEvent(ClickEvent.suggestCommand("/heat set driverposition " + heat.getName() + " " + d.getTPlayer().getName() + " ")).hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_EDIT_POSITION)))).append(Component.space()).append(theme.getRemoveButton().clickEvent(ClickEvent.suggestCommand("/heat delete driver " + heat.getName() + " " + d.getTPlayer().getName())));
            }

            player.sendMessage(message);
        }
    }

    @Subcommand("start")
    @CommandCompletion("@heat")
    @CommandPermission("%permissionheat_start")
    public static void onHeatStart(Player player, Heat heat) {
        if (heat.startCountdown()) {
            Text.send(player, Success.HEAT_COUNTDOWN_STARTED);
            return;
        }
        Text.send(player, Error.FAILED_TO_START_HEAT);
    }

    @Subcommand("finish")
    @CommandCompletion("@heat")
    @CommandPermission("%permissionheat_finish")
    public static void onHeatFinish(Player player, Heat heat) {
        if (heat.finishHeat()) {
            Text.send(player, Success.HEAT_FINISHED);
            return;
        }
        Text.send(player, Error.FAILED_TO_FINISH_HEAT);
    }

    @Subcommand("load")
    @CommandCompletion("@heat")
    @CommandPermission("%permissionheat_load")
    public static void onHeatLoad(Player player, Heat heat) {
        var state = heat.getHeatState();
        if (state != HeatState.SETUP) {
            if (!heat.resetHeat()) {
                Text.send(player, Error.FAILED_TO_RESET_HEAT);
                return;
            }
        }

        if (heat.loadHeat()) {
            if (state == HeatState.SETUP) {
                EventAnnouncements.broadcastSpectate(heat.getEvent());
            }
            Text.send(player, Success.HEAT_LOADED);
            return;
        }
        Text.send(player, Error.FAILED_TO_LOAD_HEAT);

    }

    @Subcommand("reset")
    @CommandCompletion("@heat")
    @CommandPermission("%permissionheat_reset")
    public static void onHeatReset(Player player, Heat heat) {
        if (heat.resetHeat()) {
            EventAnnouncements.broadcastReset(heat);
            Text.send(player, Success.HEAT_RESET);
            return;
        }
        Text.send(player, Error.FAILED_TO_RESET_HEAT);
    }

    @Subcommand("delete")
    @CommandCompletion("@heat")
    @CommandPermission("%permissionheat_remove")
    public static void onHeatRemove(Player player, Heat heat) {
        if (EventDatabase.removeHeat(heat)) {
            Text.send(player, Success.REMOVED_HEAT, "%heat%", heat.getName());
            return;
        }
        Text.send(player, Error.FAILED_TO_REMOVE_HEAT);
    }

    @Subcommand("create")
    @CommandCompletion("@round")
    @CommandPermission("%permissionheat_create")
    public static void onHeatCreate(Player player, Round round, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                Text.send(player, Error.NO_EVENT_SELECTED);
                return;
            }
        }
        if (event.getTrack() == null) {
            Text.send(player, Error.TRACK_NOT_FOUND_FOR_EVENT);
            return;
        }
        round.createHeat(round.getHeats().size() + 1);
        Text.send(player, Success.CREATED_HEAT, "%round%", round.getDisplayName());
    }

    @Subcommand("set laps")
    @CommandCompletion("@heat <laps>")
    @CommandPermission("%permissionheat_set_laps")
    public static void onHeatSetLaps(Player player, Heat heat, Integer laps) {
        heat.setTotalLaps(laps);
        Text.send(player, Success.SAVED);
    }

    @Subcommand("set pits")
    @CommandCompletion("@heat <pits>")
    @CommandPermission("%permissionheat_set_laps")
    public static void onHeatSetPits(Player player, Heat heat, Integer pits) {
        if (heat.getRound() instanceof QualificationRound) {
            Text.send(player, Error.CAN_NOT);
        } else {
            heat.setTotalPits(pits);
            Text.send(player, Success.SAVED);
        }
    }

    @Subcommand("set startdelay")
    @CommandCompletion("@heat <h/m/s>")
    @CommandPermission("%permissionheat_set_startdelay")
    public static void onHeatStartDelay(Player player, Heat heat, String startDelay) {
        Integer delay = ApiUtilities.parseDurationToMillis(startDelay);
        if (delay == null) {
            Text.send(player, Error.TIME_FORMAT);
            return;
        }
        heat.setStartDelayInTicks(delay);
        Text.send(player, Success.SAVED);

    }

    @Subcommand("set timelimit")
    @CommandCompletion("@heat <h/m/s>")
    @CommandPermission("%permissionheat_set_timelimit")
    public static void onHeatSetTime(Player player, Heat heat, String time) {
        Integer timeLimit = ApiUtilities.parseDurationToMillis(time);
        if (timeLimit == null) {
            Text.send(player, Error.TIME_FORMAT);
            return;
        }
        heat.setTimeLimit(timeLimit);
        Text.send(player, Success.SAVED);
    }

    @Subcommand("set maxdrivers")
    @CommandCompletion("@heat <max>")
    @CommandPermission("%permissionheat_set_maxdrivers")
    public static void onHeatMaxDrivers(Player player, Heat heat, Integer maxDrivers) {
        heat.setMaxDrivers(maxDrivers);
        Text.send(player, Success.SAVED);
    }

    @Subcommand("set driverposition")
    @CommandCompletion("@heat @players <[+/-]pos>")
    @CommandPermission("%permissionheat_set_driverposition")
    public static void onHeatSetDriverPosition(Player sender, Heat heat, String playerName, String position) {
        TPlayer tPlayer = TSDatabase.getPlayer(playerName);
        if (tPlayer == null) {
            Text.send(sender, Error.PLAYER_NOT_FOUND);
            return;
        }
        if (heat.getDrivers().get(tPlayer.getUniqueId()) == null) {
            Text.send(sender, Error.PLAYER_NOT_FOUND);
            return;
        }
        Driver driver = heat.getDrivers().get(tPlayer.getUniqueId());
        if (heat.isRacing()) {
            Text.send(sender, Error.HEAT_ALREADY_STARTED);
            return;
        }
        if (getParsedIndex(position) == null) {
            Text.send(sender, Error.NUMBER_FORMAT);
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
            Text.send(sender, Error.CAN_NOT);
            return;
        }

        if (pos < 1) {
            Text.send(sender, Error.CAN_NOT);
            return;
        }

        if (pos == driver.getStartPosition()) {
            Text.send(sender, Error.CAN_NOT);
            return;
        }


        if (heat.setDriverPosition(driver, pos)) {
            Text.send(sender, Success.DRIVER_NEW_START_POSITION, "%driver%", driver.getTPlayer().getName(), "%pos%", String.valueOf(pos));
            if (heat.getHeatState() == HeatState.LOADED) {
                heat.reloadHeat();
            }
            return;
        }
        Text.send(sender, Error.GENERIC);

    }

    @Subcommand("set reversegrid")
    @CommandCompletion("@heat <%>")
    @CommandPermission("%permissionheat_set_reversegrid")
    public static void onReverseGrid(Player player, Heat heat, @Optional Integer percentage) {
        if (percentage == null) {
            percentage = 100;
        }
        heat.reverseGrid(percentage);
        if (heat.getHeatState() == HeatState.LOADED) {
            heat.reloadHeat();
        }
        Text.send(player, Success.HEAT_REVERSED_GRID, "%percent%", String.valueOf(percentage));
    }

    @Subcommand("add")
    @CommandCompletion("@heat @players")
    @CommandPermission("%permissionheat_add_driver")
    public static void onHeatAddDriver(Player sender, Heat heat, String playerName) {
        if (!heat.getRound().getRoundIndex().equals(heat.getEvent().getEventSchedule().getCurrentRound()) && heat.getRound().getRoundIndex() != 1) {
            Text.send(sender, Error.ADD_DRIVER_FUTURE_ROUND);
            return;
        }

        if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
            Text.send(sender, Error.HEAT_FULL);
            return;
        }
        TPlayer tPlayer = TSDatabase.getPlayer(playerName);
        if (tPlayer == null) {
            Text.send(sender, Error.PLAYER_NOT_FOUND);
            return;
        }

        for (Heat h : heat.getRound().getHeats()) {
            if (h.getDrivers().get(tPlayer.getUniqueId()) != null) {
                Text.send(sender, Error.PLAYER_ALREADY_IN_ROUND);
                return;
            }
        }

        if (EventDatabase.heatDriverNew(tPlayer.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
            Text.send(sender, Success.ADDED_DRIVER);
            if (heat.getHeatState() == HeatState.LOADED) {
                heat.addDriverToGrid(heat.getDrivers().get(tPlayer.getUniqueId()));
            }
            return;
        }

        Text.send(sender, Error.FAILED_TO_ADD_DRIVER);
    }


    @Subcommand("delete driver")
    @CommandCompletion("@heat @players")
    @CommandPermission("%permissionheat_removedriver")
    public static void onHeatRemoveDriver(Player sender, Heat heat, String playerName) {
        TPlayer tPlayer = TSDatabase.getPlayer(playerName);
        if (tPlayer == null) {
            Text.send(sender, Error.PLAYER_NOT_FOUND);
            return;
        }
        if (heat.getDrivers().get(tPlayer.getUniqueId()) == null) {
            Text.send(sender, Error.PLAYER_NOT_FOUND);
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
                Text.send(sender, Success.DRIVER_DISQUALIFIED);
                return;
            }
           Text.send(sender, Error.FAILED_TO_DISQUALIFY_DRIVER);
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
                Text.send(sender, Success.DRIVER_REMOVED);
                if (reload) {
                    heat.loadHeat();
                }
                return;
            }
            Text.send(sender,Error.FAILED_TO_REMOVE_DRIVER);
        }
    }

    @Subcommand("quit")
    @CommandPermission("%permissionheat_quit")
    public static void onHeatDriverQuit(Player player) {
        if (EventDatabase.getDriverFromRunningHeat(player.getUniqueId()).isEmpty()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        Driver driver = EventDatabase.getDriverFromRunningHeat(player.getUniqueId()).get();
        Heat heat = driver.getHeat();
        if (heat.getHeatState() == HeatState.LOADED) {
            heat.resetHeat();
            if (heat.removeDriver(heat.getDrivers().get(player.getUniqueId()))) {
                heat.getEvent().removeSpectator(player.getUniqueId());
            }
            heat.loadHeat();
        }

        if (driver.getState() == DriverState.LOADED && heat.getHeatState() != HeatState.LOADED) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        if (driver.getHeat().disqualifyDriver(driver)) {
            if (player.getVehicle() != null && player.getVehicle() instanceof Boat boat) {
                boat.remove();
            }
            Location loc = player.getBedSpawnLocation() == null ? player.getWorld().getSpawnLocation() : player.getBedSpawnLocation();
            player.teleport(loc);
            Text.send(player, Success.HEAT_ABORTED);
            return;
        }
        Text.send(player, Error.FAILED_TO_ABORT_HEAT);
    }


    @Subcommand("add alldrivers")
    @CommandCompletion("@heat")
    @CommandPermission("%permissionheat_add_all")
    public static void onHeatAddDrivers(Player sender, Heat heat) {
        if (!heat.getRound().getRoundIndex().equals(heat.getEvent().getEventSchedule().getCurrentRound()) && heat.getRound().getRoundIndex() != 1) {
            Text.send(sender, Error.ADD_DRIVER_FUTURE_ROUND);
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
                Text.send(sender, Error.HEAT_FULL);
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
        Text.send(sender, Success.ADDED_ALL_DRIVERS);
    }


    @Subcommand("results")
    @CommandCompletion("@heat @players")
    @CommandPermission("%permissionheat_results")
    public static void onHeatResults(Player sender, Heat heat, @Optional String name) {
        Theme theme = TSDatabase.getPlayer(sender).getTheme();

        if (name != null) {
            TPlayer tPlayer = TSDatabase.getPlayer(name);
            if (tPlayer == null) {
                Text.send(sender, Error.PLAYER_NOT_FOUND);
                return;
            }
            if (heat.getDrivers().get(tPlayer.getUniqueId()) == null) {
                Text.send(sender, Error.PLAYER_NOT_FOUND);
                return;
            }
            Driver driver = heat.getDrivers().get(tPlayer.getUniqueId());
            Text.send(sender, Info.PLAYER_HEAT_RESULT_TITLE, "%player%", tPlayer.getName(), "%heat%", heat.getName());
            Text.send(sender, Info.PLAYER_HEAT_RESULT_POSITION, "%pos%", driver.getPosition().toString());
            Text.send(sender, Info.PLAYER_HEAT_RESULT_START_POSITION, "%pos%", String.valueOf(driver.getStartPosition()));

            var maybeBestLap = driver.getBestLap();
            maybeBestLap.ifPresent(lap -> Text.send(sender, Info.PLAYER_HEAT_RESULT_FASTEST_LAP, "%time%", ApiUtilities.formatAsTime(lap.getLapTime())));
            int count = 1;
            for (Lap l : driver.getLaps()) {
                String lap = "&2" + count + ": &1" + ApiUtilities.formatAsTime(l.getLapTime());
                if (l.equals(maybeBestLap.get())) {
                    lap += " &2(F)";
                }
                if (l.isPitted()) {
                    lap += " &2(P)";
                }
                sender.sendMessage(Text.get(sender, lap));
                count++;
            }
            return;
        }
        if (heat.getHeatState() == HeatState.FINISHED) {

            Text.send(sender, Info.HEAT_RESULT_TITLE, "%heat%", heat.getName());
            if (heat.getFastestLapUUID() != null) {
                Driver d = heat.getDrivers().get(heat.getFastestLapUUID());
                var bestLap = ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime());
                Text.send(sender, Info.HEAT_INFO_FASTEST_LAP, "%time%", bestLap, "%player%", d.getTPlayer().getName());
            }
            List<Driver> result = EventResults.generateHeatResults(heat);
            if (heat.getRound() instanceof FinalRound) {
                for (Driver d : result) {
                    Text.send(sender, Broadcast.HEAT_RESULT_ROW, "%pos%", String.valueOf(d.getPosition() ), "%player%", d.getTPlayer().getName(), "%laps%", String.valueOf(d.getLaps().size()), "%time%", ApiUtilities.formatAsTime(d.getFinishTime()));

                }
            } else {
                for (Driver d : result) {
                    sender.sendMessage(theme.primary(d.getPosition() + ".").append(Component.space()).append(theme.highlight(d.getTPlayer().getName())).append(theme.hyphen()).append(theme.highlight(d.getBestLap().isPresent() ? ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) : "0")));
                }
            }
        } else {
            Text.send(sender, Error.NOT_NOW);
        }
    }

    @Subcommand("sort tt")
    @CommandCompletion("@heat")
    @CommandPermission("%permissionheat_sort_tt")
    public static void onSortByTT(Player player, Heat heat) {
        if (heat.getHeatState() == HeatState.FINISHED) {
            Text.send(player, Error.NOT_NOW);
            return;
        }
        if (heat.isRacing()) {
            Text.send(player, Error.HEAT_ALREADY_STARTED);
            return;
        }
        if (heat.getStartPositions().isEmpty()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }
        if (!Objects.equals(heat.getRound().getRoundIndex(), heat.getEvent().getEventSchedule().getCurrentRound()) && heat.getRound().getRoundIndex() != 1) {
            Text.send(player, Error.SORT_DRIVERS_FUTURE_ROUND);
            return;
        }

        List<TimeTrialFinish> driversWithBestTimes = heat.getEvent().getTrack().getTimeTrials().getTopList().stream().filter(tt -> heat.getDrivers().containsKey(tt.getPlayer().getUniqueId())).toList();
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

        Text.send(player, Success.HEAT_SORTED_BY_TIME);
    }

    @Subcommand("sort random")
    @CommandCompletion("@heat")
    @CommandPermission("%permissionheat_sort_random")
    public static void onSortByRandom(Player player, Heat heat) {
        if (heat.getHeatState() == HeatState.FINISHED) {
            Text.send(player, Error.NOT_NOW);
            return;
        }
        if (heat.isRacing()) {
            Text.send(player, Error.HEAT_ALREADY_STARTED);
            return;
        }
        if (heat.getStartPositions().isEmpty()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }
        if (!Objects.equals(heat.getRound().getRoundIndex(), heat.getEvent().getEventSchedule().getCurrentRound()) && heat.getRound().getRoundIndex() != 1) {
            Text.send(player, Error.SORT_DRIVERS_FUTURE_ROUND);
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

        Text.send(player, Success.HEAT_SORTED_BY_RANDOM);
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

