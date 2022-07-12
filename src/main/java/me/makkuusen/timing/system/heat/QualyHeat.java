package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QualyHeat extends Heat {

    private long timeLimit;
    private QualyScoreboard qualyScoreboard;
    public QualyHeat(Event event, Track track, String name, long timeLimit){
        super(event, track, name);
        this.timeLimit = timeLimit;
    }

    @Override
    public boolean startHeat() {
        if (getHeatState() != HeatState.LOADED && getHeatState() != HeatState.SETUP) {
            return false;
        }
        qualyScoreboard = new QualyScoreboard(this);
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
    public boolean passLap(Driver driver) {
        if (getHeatState() != HeatState.RACING) {
            return false;
        }
        if (timeIsOver()) {
            driver.finish();
            return true;
        }
        if (allDriversFinished()){
            finishHeat();
            return true;
        }
        driver.passLap();
        return true;
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
        Scoreboard board = qualyScoreboard.getScoreboard();
        getParticipants().stream()
                .filter(participant -> participant.getTPlayer().getPlayer() != null)
                .forEach(participant -> participant.getTPlayer().getPlayer().setScoreboard(board));
    }

    @Override
    public void resetHeat() {
        qualyScoreboard = null;
        setHeatState(HeatState.SETUP);
        setStartTime(null);
        setEndTime(null);
        getDrivers().values().stream().forEach(driver -> driver.reset());
    }

    private boolean timeIsOver(){
        if (Duration.between(getStartTime(),TimingSystem.currentTime).toMillis() > timeLimit) {
            return true;
        }
        return false;
    }

    private boolean allDriversFinished(){
        return getDrivers().values().stream().noneMatch(driver -> !driver.isFinished());
    }
}
