package me.makkuusen.timing.system.round;

import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.event.EventResults;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.participant.Driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public abstract class Round {

    private int id;
    private final Event event;
    private Integer roundIndex;
    private final List<Heat> heats = new ArrayList<>();
    private RoundType type;
    private RoundState state;


    public enum RoundState{
        SETUP, RUNNING, FINISHED
    }

    public Round(DbRow data) {
        id = data.getInt("id");
        event = EventDatabase.getEvent(data.getInt("eventId")).get();
        roundIndex = data.getInt("roundIndex");
        type = RoundType.valueOf(data.getString("type"));
        state = data.get("state") != null ? RoundState.valueOf(data.getString("state")) : RoundState.SETUP;
    }

    public List<Heat> getHeats() {
        return heats;
    }

    public void addHeat(Heat heat) {
        heats.add(heat);
    }

    public boolean removeHeat(Heat heat) {
        if (heat.getHeatState() != HeatState.FINISHED && heat.getEvent().getState() != Event.EventState.FINISHED && heats.contains(heat)) {
            heats.remove(heat);
            for (Heat _heat : heats) {
                if (heat.getHeatNumber() < _heat.getHeatNumber()) {
                    _heat.setHeatNumber(_heat.getHeatNumber() - 1);
                }
            }
            return true;
        }
        return false;
    }

    public Optional<Heat> getHeat(String heatName) {
        return heats.stream().filter(Heat -> heatName.equalsIgnoreCase(Heat.getName())).findFirst();
    }

    public List<String> getRawHeats() {
        List<String> heatList = new ArrayList<>();
        heats.stream().forEach(heat -> heatList.add(heat.getName()));
        return heatList;
    }

    public void initRound(List<Driver> drivers) {
        if (getHeats().isEmpty()) {
            int maxDrivers = getEvent().getTrack().getGridLocations().size();
            int heats = drivers.size() / maxDrivers;
            if (drivers.size() % maxDrivers != 0) {
                heats++;
            }
            for (int i = 0; i < heats; i++) {
                createHeat(i+1);
            }
        }
        addDriversToHeats(drivers);
    }

    public abstract void createHeat(int heatNumber);

    public boolean finish(Event event) {
        if (event.getState() != Event.EventState.RUNNING) {
            return false;
        }
        if (getHeats().stream().anyMatch(Heat::isActive)){
            return false;
        }

        List<Driver> drivers = EventResults.generateRoundResults(getHeats());
        if (getHeats().size() > 1) {
            broadcastResults(event, drivers);
        }

        if (getEvent().getEventSchedule().isLastRound()) {
            return true;
        }

        getEvent().getEventSchedule().getNextRound().get().initRound(drivers);

        event.eventSchedule.nextRound();
        return true;
    }

    public abstract void broadcastResults(Event event, List<Driver> drivers);

    public void addDriversToHeats(List<Driver> drivers){
        int i = 0;
        for (Heat heat : getHeats()) {
            int startPos = 1;
            for (; i < drivers.size(); i++) {
                if (startPos > heat.getMaxDrivers()){
                    break;
                }
                EventDatabase.heatDriverNew(drivers.get(i).getTPlayer().getUniqueId(), heat, startPos++);
            }
        }
    }
}
