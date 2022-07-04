package me.makkuusen.timing.system;

import java.time.Instant;

public class RaceDriver {

    public static TimingSystem plugin;
    private RPlayer rPlayer;
    private int laps;
    private int pits;
    private RaceSplits raceSplits;
    private boolean[] checkpoints;
    private boolean finished;
    private boolean isRunning;
    private Instant endTime;
    private Race race;

    public RaceDriver(RPlayer rPlayer, Race race) {
        this.rPlayer = rPlayer;
        this.laps = 0;
        this.pits = 0;
        this.finished = false;
        this.checkpoints = new boolean[race.getTrack().getCheckpoints().size()];
        this.raceSplits = new RaceSplits(this, race.getTotalLaps(), race.getTrack().getCheckpoints().size());
        this.race = race;
        this.isRunning = false;
    }

    public RPlayer getRPlayer() {
        return rPlayer;
    }

    public void setRPlayer(RPlayer rPlayer) {
        this.rPlayer = rPlayer;
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
        raceSplits.splits[laps + 1][0] = plugin.currentTime;
        endTime = plugin.currentTime;
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
        endTime = null;
        laps = 0;
        pits = 0;
        finished = false;
        checkpoints = new boolean[race.getTrack().getCheckpoints().size()];
    }

    public void passLap() {
        laps++;
        checkpoints = new boolean[race.getTrack().getCheckpoints().size()];
        raceSplits.splits[laps][0] = plugin.currentTime;
        race.updatePositions();
    }

    public void passCheckpoint(int checkpoint)
    {

        raceSplits.splits[laps][checkpoint] = plugin.currentTime;
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
        raceSplits.splits[0][0] = plugin.currentTime;
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
