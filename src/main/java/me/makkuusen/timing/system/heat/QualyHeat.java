package me.makkuusen.timing.system.heat;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;

import java.time.Duration;

@Setter
@Getter
public class QualyHeat extends Heat {

    private long timeLimit;
    public QualyHeat(Event event, String name, long timeLimit){
        super(event, name);
        this.timeLimit = timeLimit;
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
}
