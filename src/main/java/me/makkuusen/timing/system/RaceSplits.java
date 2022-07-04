package me.makkuusen.timing.system;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

public class RaceSplits implements Comparable<RaceSplits>{
    public Instant[][] splits;
    private RaceDriver raceDriver;

    public RaceSplits(RaceDriver rd, int laps, int checkpoints)
    {
        splits = new Instant[laps+2][checkpoints+1];
        raceDriver = rd;
    }

    public long getLaptime(int lap){
        if (lap < 1)
        {
            return 0;
        }
        return Duration.between(splits[lap - 1][0], splits[lap][0]).toMillis();
    }

    @Override
    public int compareTo(@NotNull RaceSplits o) {
        if (raceDriver.getLaps() > o.raceDriver.getLaps())
        {
            return -1;
        }
        else if (raceDriver.getLaps() < o.raceDriver.getLaps())
        {
            return 1;
        }

        if (raceDriver.getLatestCheckpoint() > o.raceDriver.getLatestCheckpoint())
        {
            return -1;
        }
        else if (raceDriver.getLatestCheckpoint() < o.raceDriver.getLatestCheckpoint())
        {
            return 1;
        }

        if(o.splits[o.raceDriver.getLaps()][o.raceDriver.getLatestCheckpoint()] != null){
            return splits[raceDriver.getLaps()][raceDriver.getLatestCheckpoint()].compareTo(o.splits[o.raceDriver.getLaps()][o.raceDriver.getLatestCheckpoint()]);
        }
        return -1;
    }

    public RaceDriver getRaceDriver() {
        return raceDriver;
    }
}
