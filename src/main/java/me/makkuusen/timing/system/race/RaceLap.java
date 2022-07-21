package me.makkuusen.timing.system.race;

import me.makkuusen.timing.system.track.Track;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class RaceLap {

    private final RaceDriver raceDriver;
    private final Track track;
    private Instant lapEnd;
    private Instant lapStart;
    private boolean hasPitted;
    private final ArrayList<Instant> checkpoints = new ArrayList<>();

    public RaceLap(RaceDriver raceDriver) {
        this.raceDriver = raceDriver;
        this.track = raceDriver.race.getTrack();
        hasPitted = false;
    }

    public void setCheckpoint(int checkpoint, Instant timeStamp) {
        checkpoints.set(checkpoint, timeStamp);
    }

    public void setLapEnd(Instant timeStamp) {
        lapEnd = timeStamp;
    }

    public void setLapStart(Instant timeStamp) {
        lapStart = timeStamp;
    }

    public Instant getLapStart() {
        return lapStart;
    }

    public Instant getLapEnd() {
        return lapEnd;
    }

    public long getLaptime() {
        if (lapEnd == null || lapStart == null) {
            return -1;
        }
        long lapTime = Duration.between(lapStart, lapEnd).toMillis();
        lapTime = Math.round(lapTime / 50) * 50;
        return lapTime;
    }

    public RaceDriver getRaceDriver() {
        return raceDriver;
    }

    public int getLatestCheckpoint() {
        return checkpoints.size();
    }

    public int getNextCheckpoint() {
        if (track.getCheckpoints().size() >= checkpoints.size()) {
            return checkpoints.size() + 1;
        }
        return checkpoints.size();
    }

    public boolean hasPassedAllCheckpoints() {
        return checkpoints.size() == track.getCheckpoints().size();
    }

    public void passNextCheckpoint(Instant timeStamp) {
        checkpoints.add(timeStamp);
    }

    public Instant getPassedCheckpointTime(int checkpoint) {
        if (checkpoints.size() == 0 || checkpoint == 0) {
            return lapStart;
        }
        return checkpoints.get(checkpoint - 1);
    }

    public boolean hasPitted() {
        return hasPitted;
    }

    public void setHasPitted() {
        hasPitted = true;
    }
}
