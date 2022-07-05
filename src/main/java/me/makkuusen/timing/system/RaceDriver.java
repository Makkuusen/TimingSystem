package me.makkuusen.timing.system;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RaceDriver extends RaceParticipant implements Comparable<RaceDriver>{

    public static TimingSystem plugin;
    private int laps;
    private int pits;
    private List<RaceLap> raceLaps = new ArrayList<>();
    private boolean finished;
    private boolean isRunning;
    private Instant endTime;

    public RaceDriver(TSPlayer tsPlayer, Race race) {
        super(tsPlayer, race);
        this.laps = 0;
        this.pits = 0;
        this.finished = false;
        this.isRunning = false;
    }

    public int getLaps() {
        return laps;
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
        getCurrentLap().setLapEnd(plugin.currentTime);
        getTSPlayer().getPlayer().sendMessage("§aYou finished lap in: " + ApiUtilities.formatAsTime(getCurrentLap().getLaptime()));
        endTime = plugin.currentTime;
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
    }

    public void passLap() {
        if (laps != 0) {
            getCurrentLap().setLapEnd(plugin.currentTime);
            getTSPlayer().getPlayer().sendMessage("§aYou finished lap in: " + ApiUtilities.formatAsTime(getCurrentLap().getLaptime()));
        }
        laps++;
        RaceLap lap = new RaceLap(this);
        lap.setLapStart(plugin.currentTime);
        raceLaps.add(lap);
    }


    public boolean hasPassedAllCheckpoints() {
        return getCurrentLap().hasPassedAllCheckpoints();
    }

    public int getLatestCheckpoint()
    {
        if (raceLaps.size() == 0){
            return 0;
        }
        return getCurrentLap().getLatestCheckpoint();
    }

    public RaceLap getCurrentLap()
    {
        return raceLaps.get(laps - 1);
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

    public void resetLaps(){
        this.raceLaps = new ArrayList<>();
    }

    @Override
    public int compareTo(@NotNull RaceDriver o) {
        if (finished && !o.finished) { return -1; }
        else if (!finished && o.finished) { return 1; }
        else if (finished && o.finished) { return endTime.compareTo(o.endTime); }

        if (laps > o.laps) { return -1; }
        else if (laps < o.laps) { return 1; }

        if (getLatestCheckpoint() > o.getLatestCheckpoint()) { return -1; }
        else if (getLatestCheckpoint() < o.getLatestCheckpoint()) { return 1;}

        if (laps == 0 && getLatestCheckpoint() == 0){
            return 0;
        }
        else if (getLatestCheckpoint() == 0){
            return getCurrentLap().getLapStart().compareTo(o.getCurrentLap().getLapStart());
        }

        Instant last = getCurrentLap().getPassedCheckpointTime(getLatestCheckpoint());
        Instant oLast = o.getCurrentLap().getPassedCheckpointTime(getLatestCheckpoint());
        return last.compareTo(oLast);
    }

}
