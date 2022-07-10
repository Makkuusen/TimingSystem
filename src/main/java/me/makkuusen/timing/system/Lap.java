package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

@Getter
@Setter
public class Lap implements Comparable<Lap>{

    private Driver driver;
    private Track track;
    private Instant lapEnd;
    private Instant lapStart;
    private boolean pitted;
    private ArrayList<Instant> checkpoints = new ArrayList<>();

    public Lap(Driver driver, Track track){
        this.driver = driver;
        this.track = track;
        this.lapStart = TimingSystem.currentTime;
    }

    public void startLap() {

    }

    public long getLapTime() {
        if(lapEnd == null || lapStart == null)
        {
            return -1;
        }
        long lapTime = Duration.between(lapStart, lapEnd).toMillis();
        return ApiUtilities.roundToTick(lapTime);
    }

    public int getNextCheckpoint() {
        if (track.getCheckpoints().size() >= checkpoints.size())
        {
            return checkpoints.size() + 1;
        }
        return checkpoints.size();
    }

    public boolean hasPassedAllCheckpoints()
    {
        return checkpoints.size() == track.getCheckpoints().size();
    }

    public void passNextCheckpoint(Instant timeStamp)
    {
        checkpoints.add(timeStamp);
    }

    public int getLatestCheckpoint(){
        return checkpoints.size();
    }

    public Instant getCheckpointTime(int checkpoint) {
        if (checkpoints.size() == 0 || checkpoint == 0) {
            return lapStart;
        }
        return checkpoints.get(checkpoint - 1);
    }

    @Override
    public int compareTo(@NotNull Lap lap) {
        if (getLapTime() < lap.getLapTime()) {
            return -1;
        } else if (getLapTime() > lap.getLapTime()) {
            return 1;
        } else {
            return 0;
        }
    }
}
