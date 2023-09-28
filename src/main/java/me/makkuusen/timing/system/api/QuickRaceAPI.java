package me.makkuusen.timing.system.api;

import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Broadcast;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackLocation;
import me.makkuusen.timing.system.track.TrackRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class QuickRaceAPI {

    private static Event event;
    private static Round round;
    private static Heat heat;

    private QuickRaceAPI() {
    }

    public static boolean create(UUID playerHost, Track track, int laps, int pits) {
        if(getQuickRaceHeat().isPresent() && heat.isFinished()) deleteEvent();
        else return false;

        if(!track.isOpen() || !track.hasTrackLocation(TrackLocation.Type.GRID) || !track.hasRegion(TrackRegion.RegionType.START)) return false;

        final String name = "QuickRace";
        Optional<Event> maybeEvent = EventDatabase.eventNew(playerHost, name);
        if(maybeEvent.isEmpty()) return false;
        event = maybeEvent.get();

        event.setTrack(track);
        if(!EventDatabase.roundNew(event, RoundType.FINAL, 1)) return false;

        Optional<Round> maybeRound = event.eventSchedule.getRound(1);
        if(maybeRound.isEmpty()) return false;
        round = maybeRound.get();

        Optional<Heat> maybeHeat = round.getHeat("R1F1");
        if(maybeHeat.isEmpty()) return false;
        heat = maybeHeat.get();

        laps = Math.max(1, laps);
        pits = Math.max(0, pits);

        if(track.isStage()) {
            heat.setTotalLaps(1);
            heat.setTotalPits(0);
        } else {
            heat.setTotalLaps(laps);
            heat.setTotalPits(pits);
        }

        HeatState state = heat.getHeatState();
        if (state != HeatState.SETUP && !heat.resetHeat()) return false;

        if (!heat.loadHeat()) {
            deleteEvent();
            return false;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {

            if (heat.getDrivers().containsKey(p.getUniqueId())) {
                continue;
            }
            p.sendMessage(Component.empty());
            p.sendMessage(Text.get(p, Broadcast.CLICK_TO_JOIN_RACE, "%track%", event.getTrack().getDisplayName(), "%laps%", String.valueOf(heat.getTotalLaps())).clickEvent(ClickEvent.runCommand("/race join")));
            p.sendMessage(Component.empty());
        }
        return true;
    }

    public static boolean start() {
        if(heat == null) return false;
        return heat.startCountdown();
    }

    public static boolean end() {
        if (event == null || heat == null) return false;
        if (heat.getHeatState() == HeatState.RACING) {
            heat.finishHeat();
        } else if (heat.getHeatState() == HeatState.LOADED) {
            heat.resetHeat();
        }

        deleteEvent();
        return true;
    }

    public static Optional<Event> getQuickRaceEvent() {
        return Optional.ofNullable(event);
    }

    public static Optional<Heat> getQuickRaceHeat() {
        return Optional.ofNullable(heat);
    }

    public static boolean quickRaceActive() {
        if(getQuickRaceHeat().isPresent()) return heat.isActive();
        return false;
    }

    private static void deleteEvent() {
        EventDatabase.removeEventHard(event);
        event = null;
        round = null;
        heat = null;
    }
}
