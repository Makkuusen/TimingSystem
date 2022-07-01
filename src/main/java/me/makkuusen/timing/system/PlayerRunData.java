package me.makkuusen.timing.system;


import java.time.Duration;
import java.time.Instant;

public class PlayerRunData
{
    private final RaceTrack track;
    private final Instant startTime;
    private final boolean[] checkpoints;
    private final long bestFinish;

    public PlayerRunData(RaceTrack track, RPlayer player, Instant startTime)
    {
        this.track = track;
        this.startTime = startTime;
        this.checkpoints = new boolean[track.getCheckpoints().size()];
        this.bestFinish = getBestFinish(track.getBestFinish(player));
    }

    private long getBestFinish(RaceFinish raceFinish)
    {
        if (raceFinish == null)
        {
            return -1;
        }
        return raceFinish.getTime();
    }

    public long getCurrentTime()
    {
        return Duration.between(startTime, Instant.now()).toMillis();
    }

    public long getTimeSinceStart(Instant time)
    {
        return Duration.between(startTime, time).toMillis();
    }

    public int getCheckpoints()
    {
        return checkpoints.length;
    }

    public RaceTrack getTrack()
    {
        return track;
    }

    public boolean hasPassedAllCheckpoints()
    {
        for (boolean b : checkpoints)
        {
            if (!b)
            {
                return false;
            }
        }
        return true;
    }

    public void passCheckpoint(int checkpoint)
    {
        checkpoint -= 1;
        try
        {
            for (int i = 0; i < checkpoints.length; i++)
            {
                if (i == checkpoint)
                {
                    this.checkpoints[i] = true;
                }
                else if (!this.checkpoints[i])
                {
                    return;
                }
            }
        } catch (NullPointerException e) { }
    }

    public void passUnorderedCheckpoint(int checkpoint)
    {
        checkpoint -= 1;
        try
        {
            this.checkpoints[checkpoint] = true;
        } catch (NullPointerException e) { }
    }

    public int getLatestCheckpoint()
    {
        for (int i = 0; i < checkpoints.length; i++)
        {
            if (!this.checkpoints[i])
            {
                return i;
            }
        }
        return checkpoints.length;
    }

    public int getPassedCheckpoints()
    {
        int count = 0;
        for (int i = 0; i < checkpoints.length; i++)
        {
            if (this.checkpoints[i])
            {
                count++;
            }
        }

        return count;

    }

    public String getCheckpointsString()
    {
        if (getCheckpoints() > 0)
        {
            return " (" + getPassedCheckpoints() + "/" + getCheckpoints() + ")";
        }
        return "";
    }

    public long getBestFinish()
    {
        return bestFinish;
    }
}
