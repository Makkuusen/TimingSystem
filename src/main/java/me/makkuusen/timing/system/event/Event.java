package me.makkuusen.timing.system.event;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.FinalHeat;
import me.makkuusen.timing.system.heat.QualyHeat;
import me.makkuusen.timing.system.participant.FinalDriver;
import me.makkuusen.timing.system.participant.QualyDriver;
import me.makkuusen.timing.system.race.RaceDriver;
import me.makkuusen.timing.system.race.RaceSpectator;
import me.makkuusen.timing.system.track.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class Event {

    public static TimingSystem plugin;
    private String id;
    private String displayName;
    private String date;
    HashMap<UUID, RaceSpectator> raceSpectators = new HashMap<>();
    HashMap<UUID, RaceDriver> raceDrivers = new HashMap<>();
    private EventSchedule eventSchedule;
    private EventState state = EventState.SETUP;
    Track track;

    public enum EventState {
        SETUP, RUNNING, FINISHED
    }

    public Event(String name){
        this.id = name;
        this.displayName = name;
        this.eventSchedule = new EventSchedule();
    }

    public boolean quickSetup(List<TPlayer> tDrivers,long qualyTime, int laps, int pitstops) {
        if (track == null) {
            return false;
        }
        String heatNameQ = "Qualy";
        QualyHeat qualyHeat = new QualyHeat(this,track,heatNameQ, qualyTime);
        String heatName = "Final";
        FinalHeat finalHeat = new FinalHeat(this, track, heatName, laps, pitstops);
        for (TPlayer tPlayer : tDrivers) {
            qualyHeat.addDriver(new QualyDriver(tPlayer, qualyHeat));
            finalHeat.addDriver(new FinalDriver(tPlayer, finalHeat));
        }
        eventSchedule.createQuickSchedule(List.of(qualyHeat),List.of(finalHeat));
        return true;
    }

    public List<String> getHeatList() {
        List<String> message = new ArrayList<>();
        message.add("§2--- Heats for §a" + displayName + " §2---");
        message.addAll(eventSchedule.listHeats());
        return message;
    }

    public List<String> getRawHeatList() {
        return eventSchedule.getRawHeats();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString(){
        return id;
    }
}
