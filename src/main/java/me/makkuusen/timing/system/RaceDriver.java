package me.makkuusen.timing.system;

import java.time.Instant;

public class RaceDriver extends RaceParticipant{

    public static TimingSystem plugin;
    private int laps;
    private int pits;
    private RaceSplits raceSplits;
    private boolean[] checkpoints;
    private boolean finished;
    private boolean isRunning;
    private Instant endTime;

    public RaceDriver(TSPlayer tsPlayer, Race race) {
        super(tsPlayer, race);
        this.laps = 0;
        this.pits = 0;
        this.finished = false;
        this.checkpoints = new boolean[race.getTrack().getCheckpoints().size()];
        this.raceSplits = new RaceSplits(this, race.getTotalLaps(), race.getTrack().getCheckpoints().size());
        this.isRunning = false;
    }

    public int getLaps() {
        return laps;
    }

    public void setLaps(int laps) {
        this.laps = laps;
    }

    public int getPits() {
        return pits;
    }

    public void setPits(int pits) {
        this.pits = pits;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished() {
        finished = true;
        isRunning = false;
        raceSplits.setLapTimeStamp(laps + 1, plugin.currentTime);
        endTime = plugin.currentTime;
        race.updatePositions();
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getLapsString(int totalLaps){

        return " (" + laps + "/" + totalLaps + ")";
    }

    public void reset() {
        isRunning = false;
        endTime = null;
        laps = 0;
        pits = 0;
        finished = false;
        checkpoints = new boolean[race.getTrack().getCheckpoints().size()];
    }

    public void passLap() {
        laps++;
        checkpoints = new boolean[race.getTrack().getCheckpoints().size()];
        raceSplits.setLapTimeStamp(laps, plugin.currentTime);
        race.updatePositions();
    }

    public void passCheckpoint(int checkpoint)
    {
        raceSplits.setCheckpointTimeStamp(laps, checkpoint, plugin.currentTime);
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
                    race.updatePositions();
                    return;
                }
            }
        } catch (NullPointerException e) { }
        race.updatePositions();
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

    public int getNextCheckpoint()
    {
        for (int i = 0; i < checkpoints.length; i++)
        {
            if (!this.checkpoints[i])
            {
                return i+1;
            }
        }
        return checkpoints.length;
    }

    public long getLaptime(int lap){
        return raceSplits.getLaptime(lap);
    }

    public void start()
    {
        isRunning = true;
        passLap();
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public RaceSplits getRaceSplits(){
        return raceSplits;
    }

    public void resetRaceSplits(){
        this.raceSplits = new RaceSplits(this, race.getTotalLaps(), race.getTrack().getCheckpoints().size());
    }

}
