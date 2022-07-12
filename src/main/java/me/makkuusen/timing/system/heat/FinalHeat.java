package me.makkuusen.timing.system.heat;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.FinalDriver;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class FinalHeat extends Heat {

    private int totalLaps;
    private int totalPits;
    private FinalScoreboard finalScoreboard;

    public FinalHeat(Event event, Track track, String name, int totalLaps, int totalPits){
        super(event, track, name);
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;
    }

    @Override
    public boolean startHeat() {
        if (getHeatState() != HeatState.LOADED && getHeatState() != HeatState.SETUP) {
             return false;
        }
        finalScoreboard = new FinalScoreboard(this);
        setHeatState(HeatState.RACING);
        setStartTime(TimingSystem.currentTime);
        List<Driver> pos = new ArrayList<>();
        pos.addAll(getDrivers().values());
        setPositions(pos);
        EventAnnouncements.sendStartSound(this);
        getDrivers().values().stream().forEach(driver -> driver.setStartTime(TimingSystem.currentTime));
        return true;
    }

    @Override
    public void resetHeat() {
        finalScoreboard = null;
        setHeatState(HeatState.SETUP);
        setStartTime(null);
        setEndTime(null);
        getDrivers().values().stream().forEach(driver -> driver.reset());
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
        setEndTime(TimingSystem.currentTime);
        ApiUtilities.clearScoreboards();
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
        Scoreboard board = finalScoreboard.getScoreboard();
        getParticipants().stream()
                .filter(participant -> participant.getTPlayer().getPlayer() != null)
                .forEach(participant -> participant.getTPlayer().getPlayer().setScoreboard(board));
    }
}
