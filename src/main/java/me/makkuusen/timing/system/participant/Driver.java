package me.makkuusen.timing.system.participant;

import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.driver.*;
import me.makkuusen.timing.system.database.EventDatabase;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.heat.DriverScoreboard;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.track.regions.TrackRegion;
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

    public void updateScoreboard() {
        if (getTPlayer().getPlayer() == null) {
            if (scoreboard != null) {
                scoreboard.removeScoreboard();
                scoreboard = null;
            }
            return;
        }
        if (scoreboard == null) {
            scoreboard = new DriverScoreboard(getTPlayer(), this);
        }
        scoreboard.setDriverLines();
    }

    public void finish() {
        finishLap();
        setEndTime(TimingSystem.currentTime);
        state = DriverState.FINISHED;

        DriverFinishHeatEvent e = new DriverFinishHeatEvent(this);
        e.callEvent();
    }

    public void disqualify() {
        state = DriverState.FINISHED;

        DriverDisqualifyEvent e = new DriverDisqualifyEvent(this);
        e.callEvent();
    }

    public void start() {
        state = DriverState.RUNNING;
        newLap();

        DriverStartEvent e = new DriverStartEvent(this);
        e.callEvent();
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

            DriverPassPitEvent e = new DriverPassPitEvent(this, getCurrentLap(), pits);
            e.callEvent();

            return true;
        }
        return false;
    }

    private void finishLap() {
        var oldBest = getBestLap();
        getCurrentLap().setLapEnd(TimingSystem.currentTime);
        boolean isFastestLap = heat.getFastestLapUUID() == null || getCurrentLap().getLapTime() < heat.getDrivers().get(heat.getFastestLapUUID()).getBestLap().get().getLapTime() || getCurrentLap().equals(heat.getDrivers().get(heat.getFastestLapUUID()).getBestLap().get());
        if (isFastestLap) {
            EventAnnouncements.broadcastFastestLap(heat, this, getCurrentLap(), oldBest);
            heat.setFastestLapUUID(getTPlayer().getUniqueId());
        } else {
            if (heat.getRound() instanceof QualificationRound) {
                EventAnnouncements.broadcastQualifyingLap(heat, this, getCurrentLap(), oldBest);
            } else {
                EventAnnouncements.broadcastLapTime(heat, this, getCurrentLap().getLapTime());
            }
        }

        DriverFinishLapEvent e = new DriverFinishLapEvent(this, getCurrentLap(), isFastestLap);
        e.callEvent();

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

    public boolean isFinished() {
        return endTime != null;
    }

    public boolean isRunning() {
        return state == DriverState.RUNNING || state == DriverState.LOADED || state == DriverState.STARTING;
    }

    public boolean isInPit(Location playerLoc) {
        var inPitRegions = heat.getEvent().getTrack().getTrackRegions().getRegions(TrackRegion.RegionType.INPIT);
        for (TrackRegion trackRegion : inPitRegions) {
            if (trackRegion.contains(playerLoc)) {
                return true;
            }
        }
        return false;
    }

    private void newLap() {
        laps.add(new Lap(this, heat.getEvent().getTrack()));
        DriverNewLapEvent e = new DriverNewLapEvent(this, getCurrentLap());
        e.callEvent();
    }

    public long getFinishTime() {
        if(endTime == null) return 0;
        return Duration.between(startTime, endTime).toMillis();
    }

    public void setPosition(int position) {
        this.position = position;
        TimingSystem.getEventDatabase().driverSet(id, "position", String.valueOf(position));
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
        TimingSystem.getEventDatabase().driverSet(id, "startPosition", String.valueOf(startPosition));
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
        TimingSystem.getEventDatabase().driverSet(id, "startTime", startTime == null ? "NULL" : String.valueOf(startTime.toEpochMilli()));
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        TimingSystem.getEventDatabase().driverSet(id, "endTime", endTime == null ? "NULL" : String.valueOf(endTime.toEpochMilli()));
    }

    public void setPits(int pits) {
        this.pits = pits;
        TimingSystem.getEventDatabase().driverSet(id, "pitstops", String.valueOf(pits));
    }

    public void setState(DriverState state) {
        this.state = state;
    }

    public @Nullable Lap getCurrentLap() {
        return laps.get(laps.size() - 1);
    }

    public void removeUnfinishedLap() {
        if (!laps.isEmpty() && getCurrentLap().getLapEnd() == null) {
            laps.remove(getCurrentLap());
        }
    }

    public Optional<Lap> getBestLap() {
        if (getLaps().isEmpty()) {
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


    public void onShutdown() {
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

            if (getLaps().isEmpty()) {
                return 0;
            }

            long timeDiff;
            if (getPosition() < comparingDriver.getPosition()) {
                if (comparingDriver.isFinished()) {
                    return Duration.between(getEndTime(), comparingDriver.getEndTime()).toMillis();
                }

                if (!comparingDriver.getLaps().isEmpty() && comparingDriver.getCurrentLap() != null) {
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

    private int compareToQualification(Driver o) {
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

    private int compareToFinaldriver(Driver o) {
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

        if (getLaps().isEmpty()) {
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
