package me.makkuusen.timing.system.round;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;

import java.util.List;

public class BCCSprintRace extends FinalRound {

    public BCCSprintRace(DbRow data) {
        super(data);
    }

    public String getName(){
        return "R" + getRoundIndex() + "-SprintRace";
    }

    public String getDisplayName(){
        return "Round " + getRoundIndex() + " - SprintRace";
    }

    public void addDriversToHeats(List<Driver> drivers){
        int i = 0;
        for (Heat heat : getHeats()) {

            int startPos = (int) Math.round(drivers.size() * 0.75);
            int startPosCounter = startPos;
            boolean countdown = true;
            for (; i < drivers.size(); i++) {
                if (startPosCounter > heat.getMaxDrivers()){
                    break;
                }
                if (startPosCounter == 0) {
                    countdown = false;
                    startPosCounter = startPos + 1;
                }
                if (countdown) {
                    EventDatabase.heatDriverNew(drivers.get(i).getTPlayer().getUniqueId(), heat, startPosCounter--);
                } else {
                    EventDatabase.heatDriverNew(drivers.get(i).getTPlayer().getUniqueId(), heat, startPosCounter++);
                }
            }
        }
    }

    public void createHeat(int heatNumber){
        var maybeHeat = EventDatabase.heatNew(this, heatNumber);
        if (maybeHeat.isPresent()) {
            var nextHeat = maybeHeat.get();
            nextHeat.setTotalLaps(TimingSystem.configuration.getLaps());
            nextHeat.setTotalPits(0);
        }
    }
}
