package me.makkuusen.timing.system.participant;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.DriverScoreboard;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class Driver extends Participant implements Comparable<Driver> {

    private int id;
    private Heat heat;
    private Integer position;
    private int startPosition;
    private int pits;
    private Instant startTime;
    private Instant endTime;
    private DriverState state;
    private DriverScoreboard scoreboard;
    private List<Lap> laps = new ArrayList<>();

    public Driver(DbRow data) {
        super(data);
        id = data.get("id");
        heat = EventDatabase.getHeat(data.getInt("heatId")).get();
        position = data.getInt("position");
        startPosition = data.getInt("startPosition");
        startTime = data.getLong("startTime") == null ? null : Instant.ofEpochMilli(data.getLong("startTime"));
        endTime = data.getLong("endTime") == null ? null : Instant.ofEpochMilli(data.getLong("endTime"));
        pits = data.getInt("pitstops");
        state = isFinished() ? DriverState.FINISHED : DriverState.SETUP;
    }

    public void updateScoreboard(){
        if (getTPlayer().getPlayer() == null) {
            if (scoreboard != null) {
                scoreboard.removeScoreboard();
                scoreboard = null;
            }
            return;
        }
        if (scoreboard == null){
            scoreboard = new DriverScoreboard(getTPlayer(), this);
        }
        scoreboard.setDriverLines();
    }

    public void finish() {
        finishLap();
        setEndTime(TimingSystem.currentTime);
        state = DriverState.FINISHED;
    }

    public void disqualify(){
        state = DriverState.FINISHED;
    }

    public void start() {
        state = DriverState.RUNNING;
        newLap();
    }

    public void passLap() {
        finishLap();
        newLap();
    }

    public boolean passPit() {
        if (!getCurrentLap().isPitted()) {
            setPits(pits + 1);
            EventAnnouncements.broadcastPit(getHeat(), this, pits);
            getCurrentLap().setPitted(true);
            return true;
        }
        return false;
    }

    private void finishLap() {
        getCurrentLap().setLapEnd(TimingSystem.currentTime);
        if (heat.getFastestLapUUID() == null || getCurrentLap().getLapTime() < heat.getDrivers().get(heat.getFastestLapUUID()).getBestLap().get().getLapTime() || getCurrentLap().equals(heat.getDrivers().get(heat.getFastestLapUUID()).getBestLap().get())) {
            EventAnnouncements.broadcastFastestLap(heat, this, getCurrentLap().getLapTime());
            heat.setFastestLapUUID(getTPlayer().getUniqueId());
        } else {
            EventAnnouncements.sendLapTime(this, getCurrentLap().getLapTime());
        }
        ApiUtilities.msgConsole(getTPlayer().getName() + " finished lap in: " + ApiUtilities.formatAsTime(getCurrentLap().getLapTime()));
    }

    public void reset() {
        state = DriverState.SETUP;
        setEndTime(null);
        setStartTime(null);
        laps = new ArrayList<>();
        setPosition(startPosition);
        removeScoreboard();
        scoreboard = null;
        setPits(0);
    }

    public void removeScoreboard() {
        if (scoreboard != null) {
            scoreboard.removeScoreboard();
        }
    }

    public boolean isFinished(){
        return endTime != null;
    }

    public boolean isRunning(){
        return state == DriverState.RUNNING || state == DriverState.LOADED || state == DriverState.STARTING;
    }

    public boolean isInPit(Location playerLoc) {
        var inPitRegions = heat.getEvent().getTrack().getRegions(TrackRegion.RegionType.INPIT);
        for (TrackRegion trackRegion : inPitRegions) {
            if (trackRegion.contains(playerLoc)){
                return true;
            }
        }
        return false;
    }

    private void newLap() {
        laps.add(new Lap(this, heat.getEvent().getTrack()));
    }

    public long getFinishTime() {
        return Duration.between(startTime, endTime).toMillis();
    }

    public void setPosition(int position) {
        this.position = position;
        DB.executeUpdateAsync("UPDATE `ts_drivers` SET `position` = " + position + " WHERE `id` = " + id + ";");
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
        DB.executeUpdateAsync("UPDATE `ts_drivers` SET `startPosition` = " + startPosition + " WHERE `id` = " + id + ";");
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
        if (startTime == null) {
            DB.executeUpdateAsync("UPDATE `ts_drivers` SET `startTime` = NULL WHERE `id` = " + id + ";");
        } else {
            DB.executeUpdateAsync("UPDATE `ts_drivers` SET `startTime` = " + startTime.toEpochMilli() + " WHERE `id` = " + id + ";");
        }
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        if (endTime == null) {
            DB.executeUpdateAsync("UPDATE `ts_drivers` SET `endTime` = NULL WHERE `id` = " + id + ";");
        } else {
            DB.executeUpdateAsync("UPDATE `ts_drivers` SET `endTime` = " + endTime.toEpochMilli() + " WHERE `id` = " + id + ";");
        }
    }

    public void setPits(int pits) {
        this.pits = pits;
        DB.executeUpdateAsync("UPDATE `ts_drivers` SET `pitstops` = " + pits + " WHERE `id` = " + getId() + ";");
    }

    public void setState(DriverState state) {
        this.state = state;
    }

    public @Nullable Lap getCurrentLap() {
        return laps.get(laps.size() - 1);
    }

    public void removeUnfinishedLap(){
        if(laps.size() > 0 && getCurrentLap().getLapEnd() == null) {
            laps.remove(getCurrentLap());
        }
    }

    public Optional<Lap> getBestLap() {
        if (getLaps().size() == 0) {
            return Optional.empty();
        }
        if (getLaps().get(0).getLapTime() == -1) {
            return Optional.empty();
        }
        Lap bestLap = getLaps().get(0);
        for (Lap lap : getLaps()) {
            if (lap.getLapTime() != -1 && lap.getLapTime() < bestLap.getLapTime()) {
                bestLap = lap;
            }
        }
        return Optional.of(bestLap);
    }


    public void onShutdown(){
        if (scoreboard != null) {
            scoreboard.removeScoreboard();
        }
    }

    public Instant getTimeStamp(int lap, int checkpoint) {
        var heat = getHeat();
        if (lap > heat.getTotalLaps()) {
            return getLaps().get(heat.getTotalLaps() - 1).getLapEnd();
        }

        return getLaps().get(lap - 1).getCheckpointTime(checkpoint);
    }

    public long getTimeGap(Driver comparingDriver) {

        if (heat.getRound() instanceof QualificationRound) {
            if (getBestLap().isEmpty()) {
                return 0;
            }

            if (comparingDriver.getBestLap().isEmpty()) {
                return 0;
            }

            if (comparingDriver.equals(this)) {
                return 0;
            }

            // returns time-difference
            return getBestLap().get().getLapTime() - comparingDriver.getBestLap().get().getLapTime();
        } else {

        if (getLaps().size() < 1) {
            return 0;
        }

        long timeDiff;
        if (getPosition() < comparingDriver.getPosition()) {
            if (comparingDriver.isFinished()) {
                return Duration.between(getEndTime(), comparingDriver.getEndTime()).toMillis();
            }

            if (comparingDriver.getLaps().size() > 0 && comparingDriver.getCurrentLap() != null) {
                Instant timeStamp = comparingDriver.getTimeStamp(comparingDriver.getLaps().size(), comparingDriver.getCurrentLap().getLatestCheckpoint());
                Instant fasterTimeStamp = getTimeStamp(comparingDriver.getLaps().size(), comparingDriver.getCurrentLap().getLatestCheckpoint());
                timeDiff = Duration.between(fasterTimeStamp, timeStamp).toMillis();
                return timeDiff;
            }
        }

        if (getPosition() > comparingDriver.getPosition()) {
            if (isFinished()) {
                return Duration.between(comparingDriver.getEndTime(), getEndTime()).toMillis();
            }
                Instant timeStamp = getTimeStamp(getLaps().size(), getCurrentLap().getLatestCheckpoint());
                Instant fasterTimeStamp = comparingDriver.getTimeStamp(getLaps().size(), getCurrentLap().getLatestCheckpoint());
                timeDiff = Duration.between(fasterTimeStamp, timeStamp).toMillis();
                return timeDiff;
        }
        return 0;
        }
    }


    @Override
    public int compareTo(@NotNull Driver o) {
        if (heat.getRound() instanceof QualificationRound) {
            return compareToQualification(o);
        } else {
            return compareToFinaldriver(o);
        }
    }

    private int compareToQualification(Driver o){
        var bestLap = getBestLap();
        var oBestLap = o.getBestLap();
        if (bestLap.isEmpty() && oBestLap.isEmpty()) {
            return 0;
        } else if (bestLap.isPresent() && oBestLap.isEmpty()) {
            return -1;
        } else if (bestLap.isEmpty()) {
            return 1;
        }

        var lapTime = bestLap.get().getLapTime();
        var oLapTime = oBestLap.get().getLapTime();
        if (lapTime < oLapTime) {
            return -1;
        } else if (lapTime > oLapTime) {
            return 1;
        }

        return 0;
    }

    private int compareToFinaldriver(Driver o){
        if (isFinished() && !o.isFinished()) {
            return -1;
        } else if (!isFinished() && o.isFinished()) {
            return 1;
        } else if (isFinished() && o.isFinished()) {
            // Make sure a disqualified driver don't rank better on endtime with fewer laps.
            if (getLaps().size() < o.getLaps().size()) {
                return 1;
            } else {
                return getEndTime().compareTo(o.getEndTime());
            }
        }

        if (getLaps().size() > o.getLaps().size()) {
            return -1;
        } else if (getLaps().size() < o.getLaps().size()) {
            return 1;
        }

        if (getLaps().size() == 0) {
            return 0;
        }

        Lap lap = getCurrentLap();
        Lap oLap = o.getCurrentLap();

        if (lap.getLatestCheckpoint() > oLap.getLatestCheckpoint()) {
            return -1;
        } else if (lap.getLatestCheckpoint() < oLap.getLatestCheckpoint()) {
            return 1;
        }

        if (lap.getLatestCheckpoint() == 0) {
            return 0;
        } else if (lap.getLatestCheckpoint() == 0) {
            return lap.getLapStart().compareTo(oLap.getLapStart());
        }

        Instant last = lap.getCheckpointTime(lap.getLatestCheckpoint());
        Instant oLast = oLap.getCheckpointTime(lap.getLatestCheckpoint());
        return last.compareTo(oLast);
    }
}
