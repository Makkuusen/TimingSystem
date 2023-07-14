package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.theme.messages.Broadcast;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("race")
public class CommandRace extends BaseCommand {
    public static TimingSystem plugin;
    public static Event event;
    public static Round round;
    public static Heat heat;

    @CommandPermission("race.admin")
    @Subcommand("start")
    public void onStart(Player player) {

        if (heat == null) {
            plugin.sendMessage(player, Error.RACE_NOT_FOUND);

            return;
        }
        if (heat.startCountdown()) {
            plugin.sendMessage(player, Success.HEAT_COUNTDOWN_STARTED);
            return;
        }
        plugin.sendMessage(player, Error.FAILED_TO_START_HEAT);

    }

    @CommandPermission("race.admin")
    @Subcommand("end")
    public void onEnd(Player player) {
        if (event == null) {
            plugin.sendMessage(player, Error.RACE_NOT_FOUND);
            return;
        }
        if (heat != null && heat.getHeatState() == HeatState.RACING) {
            heat.finishHeat();
        } else if (heat != null && heat.getHeatState() == HeatState.LOADED) {
            heat.resetHeat();
        }

        deleteEvent();
        plugin.sendMessage(player, Success.RACE_FINISHED);

    }

    @CommandPermission("race.admin")
    @Subcommand("create")
    @CommandCompletion("@track laps pits")
    public void onCreate(Player player, Track track, @Optional Integer laps, @Optional Integer pits) {

        if (heat != null) {
            if (heat.isFinished()) {
                deleteEvent();
            } else {
                plugin.sendMessage(player, Error.RACE_IN_PROGRESS, "%track%", heat.getEvent().getTrack().getDisplayName());
                return;
            }
        }

        if (!track.isOpen()) {
            plugin.sendMessage(player, Error.TRACK_IS_CLOSED);
            return;
        }


        String name = "QuickRace";
        var maybeEvent = EventDatabase.eventNew(player.getUniqueId(), name);
        if (maybeEvent.isEmpty()) {
            plugin.sendMessage(player, Error.GENERIC);
            return;
        }

        event = maybeEvent.get();
        event.setTrack(track);

        if (!EventDatabase.roundNew(event, RoundType.FINAL, 1)) {
            plugin.sendMessage(player, Error.FAILED_TO_CREATE_ROUND);
            return;
        }

        var maybeRound = event.getEventSchedule().getRound(1);

        if (maybeRound.isEmpty()) {
            plugin.sendMessage(player, Error.GENERIC);
            return;
        }

        round = maybeRound.get();

        round.createHeat(1);

        var maybeHeat = round.getHeat("R1F1");

        if (maybeHeat.isEmpty()) {
            plugin.sendMessage(player, Error.FAILED_TO_CREATE_HEAT);
            return;
        }

        heat = maybeHeat.get();


        if (track.isStage()) {
            heat.setTotalLaps(1);
            heat.setTotalPits(0);
        } else {
            if (laps != null && laps > 0) {
                heat.setTotalLaps(laps);
            } else {
                heat.setTotalLaps(5);
            }

            if (track.hasRegion(TrackRegion.RegionType.PIT) && pits == null) {
                heat.setTotalPits(1);
            } else if (pits != null) {
                heat.setTotalPits(Math.min(laps, pits));
            } else {
                heat.setTotalPits(0);
            }
        }

        var state = heat.getHeatState();
        if (state != HeatState.SETUP) {
            if (!heat.resetHeat()) {
                plugin.sendMessage(player, Error.FAILED_TO_RESET_HEAT);
                return;
            }
        }

        if (!heat.loadHeat()) {
            plugin.sendMessage(player,Error.FAILED_TO_LOAD_HEAT);
            deleteEvent();
            return;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {

            if (heat.getDrivers().containsKey(p.getUniqueId())) {
                continue;
            }
            p.sendMessage(Component.empty());
            p.sendMessage(plugin.getText(p, Broadcast.CLICK_TO_JOIN_RACE, "%track%", event.getTrack().getDisplayName(), "%laps%", String.valueOf(heat.getTotalLaps())).clickEvent(ClickEvent.runCommand("/race join")));
            p.sendMessage(Component.empty());
        }
    }

    @Subcommand("join")
    public void onClickToJoin(Player player) {

        if (heat == null) {
            plugin.sendMessage(player, Error.NOT_NOW);
            return;
        }

        if (heat.getHeatState() != HeatState.LOADED) {
            plugin.sendMessage(player, Error.NOT_NOW);
            return;
        }

        if (heat.getDrivers().get(player.getUniqueId()) != null) {
            plugin.sendMessage(player, Error.ALREADY_SIGNED_RACE);

            return;
        }

        if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
            plugin.sendMessage(player, Error.RACE_FULL);
            return;
        }

        if (EventDatabase.heatDriverNew(player.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
            plugin.sendMessage(player, Success.SIGNED_RACE);
            heat.addDriverToGrid(heat.getDrivers().get(player.getUniqueId()));
            return;
        }

        plugin.sendMessage(player, Error.NOT_NOW);
    }

    private void deleteEvent() {
        EventDatabase.removeEventHard(event);
        event = null;
        round = null;
        heat = null;
    }
}
