package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Event {

    public static TimingSystem plugin;
    private String name;
    private String date;
    HashMap<UUID, RaceSpectator> raceSpectators = new HashMap<>();
    HashMap<UUID, RaceDriver> raceDrivers = new HashMap<>();
    private EventSchedule eventSchedule;
    Track track;

    public enum EventState {
        SETUP, RUNNING, FINISHED
    }

    public Event(String name){
        this.name = name;
        this.eventSchedule = new EventSchedule();
    }

    public boolean createQuickEvent(List<TPlayer> tDrivers, int laps, int pitstops) {
        if (track == null) {
            return false;
        }
        String heatName = "Final";
        FinalHeat finalHeat = new FinalHeat(plugin, track, heatName, laps, pitstops);
        for (TPlayer tPlayer : tDrivers) {
            finalHeat.addDriver(new FinalDriver(tPlayer, finalHeat));
        }
        eventSchedule.createQuickSchedule(List.of(finalHeat));
        return true;
    }

    public List<String> getHeatList() {
        List<String> message = new ArrayList<>();
        message.add("§2--- Heats for §a" + name + " §2---");
        message.addAll(eventSchedule.listHeats());
        return message;
    }

}
