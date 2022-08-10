package me.makkuusen.timing.system.round;

import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;

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


    enum RoundState{
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

    public boolean removeHeat(Heat IHeat) {
        if (IHeat.getHeatState() != HeatState.FINISHED && IHeat.getEvent().getState() != Event.EventState.FINISHED && heats.contains(IHeat)) {
            heats.remove(IHeat);
            for (Heat _heat : heats) {
                if (IHeat.getHeatNumber() < _heat.getHeatNumber()) {
                    _heat.setHeatNumber(_heat.getHeatNumber() - 1);
                }
            }
            return true;
        }
        return false;
    }

    public Optional<Heat> getHeat(String heatName) {
        return heats.stream().filter(IHeat -> heatName.equalsIgnoreCase(IHeat.getName())).findFirst();
    }

    public List<String> getRawHeats() {
        List<String> heatList = new ArrayList<>();
        heats.stream().forEach(heat -> heatList.add(heat.getName()));
        return heatList;
    }

    public abstract boolean finish(Event event, Round nextRound);


}
