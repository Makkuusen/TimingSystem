package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Broadcast;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackLocation;
import me.makkuusen.timing.system.track.TrackRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("race")
public class CommandRace extends BaseCommand {
    public static Event event;
    public static Round round;
    public static Heat heat;

    @CommandPermission("race.admin")
    @Subcommand("start")
    public static void onStart(Player player) {

        if (heat == null) {
            Text.send(player, Error.RACE_NOT_FOUND);

            return;
        }
        if (heat.startCountdown()) {
            Text.send(player, Success.HEAT_COUNTDOWN_STARTED);
            return;
        }
        Text.send(player, Error.FAILED_TO_START_HEAT);

    }

    @CommandPermission("race.admin")
    @Subcommand("end")
    public void onEnd(Player player) {
        if (event == null) {
            Text.send(player, Error.RACE_NOT_FOUND);
            return;
        }
        if (heat != null && heat.getHeatState() == HeatState.RACING) {
            heat.finishHeat();
        } else if (heat != null && heat.getHeatState() == HeatState.LOADED) {
            heat.resetHeat();
        }

        deleteEvent();
        Text.send(player, Success.RACE_FINISHED);

    }

    @CommandPermission("race.admin")
    @Subcommand("create")
    @CommandCompletion("@track laps pits")
    public void onCreate(Player player, Track track, @Optional Integer laps, @Optional Integer pits) {

        if (heat != null) {
            if (heat.isFinished()) {
                deleteEvent();
            } else {
                Text.send(player, Error.RACE_IN_PROGRESS, "%track%", heat.getEvent().getTrack().getDisplayName());
                return;
            }
        }

        if (!track.isOpen()) {
            Text.send(player, Error.TRACK_IS_CLOSED);
            return;
        }

        if (!track.hasRegion(TrackRegion.RegionType.START)) {
            Text.send(player, Error.GENERIC);
            return;
        }

        if (!track.hasTrackLocation(TrackLocation.Type.GRID)) {
            Text.send(player, Error.GENERIC);
            return;
        }


        String name = "QuickRace";
        var maybeEvent = EventDatabase.eventNew(player.getUniqueId(), name);
        if (maybeEvent.isEmpty()) {
            Text.send(player, Error.GENERIC);
            return;
        }

        event = maybeEvent.get();
        event.setTrack(track);

        if (!EventDatabase.roundNew(event, RoundType.FINAL, 1)) {
            Text.send(player, Error.FAILED_TO_CREATE_ROUND);
            return;
        }

        var maybeRound = event.getEventSchedule().getRound(1);

        if (maybeRound.isEmpty()) {
            Text.send(player, Error.GENERIC);
            return;
        }

        round = maybeRound.get();

        round.createHeat(1);

        var maybeHeat = round.getHeat("R1F1");

        if (maybeHeat.isEmpty()) {
            Text.send(player, Error.FAILED_TO_CREATE_HEAT);
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
                heat.setTotalLaps(3);
            }

            if (pits != null) {
                heat.setTotalPits(Math.min(laps, pits));
            } else {
                heat.setTotalPits(0);
            }
        }

        var state = heat.getHeatState();
        if (state != HeatState.SETUP) {
            if (!heat.resetHeat()) {
                Text.send(player, Error.FAILED_TO_RESET_HEAT);
                return;
            }
        }

        if (!heat.loadHeat()) {
            Text.send(player,Error.FAILED_TO_LOAD_HEAT);
            deleteEvent();
            return;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {

            if (heat.getDrivers().containsKey(p.getUniqueId())) {
                continue;
            }
            p.sendMessage(Component.empty());
            p.sendMessage(Text.get(p, Broadcast.CLICK_TO_JOIN_RACE, "%track%", event.getTrack().getDisplayName(), "%laps%", String.valueOf(heat.getTotalLaps())).clickEvent(ClickEvent.runCommand("/race join")));
            p.sendMessage(Component.empty());
        }
    }

    @Subcommand("join")
    public static void onClickToJoin(Player player) {

        if (heat == null) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        if (heat.getHeatState() != HeatState.LOADED) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        if (heat.getDrivers().get(player.getUniqueId()) != null) {
            Text.send(player, Error.ALREADY_SIGNED_RACE);

            return;
        }

        if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
            Text.send(player, Error.RACE_FULL);
            return;
        }

        if (EventDatabase.heatDriverNew(player.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
            Text.send(player, Success.SIGNED_RACE);
            heat.addDriverToGrid(heat.getDrivers().get(player.getUniqueId()));
            return;
        }

        Text.send(player, Error.NOT_NOW);
    }

    private void deleteEvent() {
        EventDatabase.removeEventHard(event);
        event = null;
        round = null;
        heat = null;
    }
}
