package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class FinalHeat extends Heat {

    public static TimingSystem plugin;

    private int totalLaps;
    private int totalPits;
    private RaceScoreboard raceScoreboard;
    private List<RaceDriver> livePositioning = new ArrayList<>();

    public FinalHeat(TimingSystem plugin, Track track, int totalLaps, int totalPits){
        super(plugin, track);
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;
    }

    @Override
    public boolean startHeat() {
        if (getHeatState() != HeatState.LOADED) {
             return false;
        }
        setHeatState(HeatState.RACING);
        setStartTime(plugin.currentTime);
        EventAnnouncements.sendStartSound(this);
        return true;
    }

    @Override
    public boolean passLap(Driver driver){
        if (getHeatState() != HeatState.RACING) {
            return false;
        }
        if (!(driver instanceof FinalDriver))
        {
            return false;
        }
        FinalDriver fDriver = (FinalDriver) driver;
        if (totalLaps <= fDriver.getLaps().size() && totalPits <= fDriver.getPits())
        {
            finishDriver(fDriver);
            return true;
        }
        driver.passLap();
        return true;
    }

    private void finishDriver(FinalDriver driver) {
        driver.finish();
        EventAnnouncements.sendFinishSound(driver);
        EventAnnouncements.sendFinishTitle(driver);
        EventAnnouncements.broadcastFinish(this, driver, driver.getFinishTime());
    }

    @Override
    public boolean finishHeat() {
        if (getHeatState() != HeatState.RACING) {
            return false;
        }
        setHeatState(HeatState.FINISHED);
        setEndTime(plugin.currentTime);
        return true;
    }

    @Override
    public void updatePositions() {
        Collections.sort(getPositions());
        int pos = 1;
        for (Driver rd : getPositions())
        {
            rd.setPosition(pos++);
        }
    }
}
