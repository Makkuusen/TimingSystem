package me.makkuusen.timing.system.round;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.EventDatabase;

public class BCCQualy extends QualificationRound {

    public BCCQualy(DbRow data) {
        super(data);
    }

    public void createHeat(int heatNumber){
        var maybeHeat = EventDatabase.heatNew(this, heatNumber);
        if (maybeHeat.isPresent()) {
            var nextHeat = maybeHeat.get();
            nextHeat.setTimeLimit(TimingSystem.configuration.getTimeLimit());
        }
    }
}
