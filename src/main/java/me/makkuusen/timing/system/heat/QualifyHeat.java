package me.makkuusen.timing.system.heat;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;

import java.time.Duration;

@Setter
@Getter
public class QualifyHeat extends Heat {

    private int timeLimit;

    public QualifyHeat(DbRow data) {
        super(data);
        timeLimit = data.getInt("timeLimit");
        setScoreboard(new QualyScoreboard(this));
    }

    @Override
    public boolean passLap(Driver driver) {
        if (getHeatState() != HeatState.RACING) {
            return false;
        }
        if (timeIsOver()) {
            updatePositions();
            driver.finish();
            EventAnnouncements.sendFinishSound(driver);
            if (allDriversFinished()){
                finishHeat();
            }
            return true;
        }

        driver.passLap();
        return true;
    }

    private boolean timeIsOver(){
        if (Duration.between(getStartTime(),TimingSystem.currentTime).toMillis() > timeLimit) {
            return true;
        }
        return false;
    }

    public void setTimeLimit(int timeLimit){
        this.timeLimit = timeLimit;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `timeLimit` = " + timeLimit + " WHERE `id` = " + getId() + ";");
    }
}
