package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Final extends Heat {

    public static TimingSystem plugin;

    private int totalLaps;
    private int totalPits;
    private RaceState raceState;
    private RaceScoreboard raceScoreboard;
    private List<RaceDriver> livePositioning = new ArrayList<>();

    public Final(Track track, int totalLaps, int totalPits){
        super(track);
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;
    }

    public boolean startHeat() {
        if (raceState != RaceState.LOADED) {
             return false;
        }
        raceState = RaceState.RACING;
        setStartTime(plugin.currentTime);
        EventAnnouncements.sendStartSound(this);
        return true;
    }

    public boolean passLap(Driver driver){
        if (raceState != RaceState.RACING) {
            return false;
        }
        if (!(driver instanceof FinalDriver))
        {
            return false;
        }
        FinalDriver fDriver = (FinalDriver) driver;
        if (totalLaps <= fDriver.getLaps() && totalPits <= fDriver.getPits())
        {
            finishDriver(fDriver);
            return true;
        }
        driver.passLap();
        return true;
    }

    private void finishDriver(FinalDriver driver) {
        driver.setFinished();
        EventAnnouncements.sendFinishSound(driver);
        EventAnnouncements.sendFinishTitle(driver);
        EventAnnouncements.broadcastFinish(this, driver, driver.getFinishTime());
    }

    public boolean finishHeat() {
        if (raceState != RaceState.RACING) {
            return false;
        }
        raceState = RaceState.FINISHED;
        setEndTime(plugin.currentTime);
        return true;
    }
}
