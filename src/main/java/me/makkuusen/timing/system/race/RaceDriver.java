package me.makkuusen.timing.system.race;

import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RaceDriver extends RaceParticipant implements Comparable<RaceDriver>{

    public static TimingSystem plugin;
    private int laps;
    private int pits;
    private int position;
    private List<RaceLap> raceLaps = new ArrayList<>();
    private boolean finished;
    private boolean isRunning;
    private Instant endTime;

    public RaceDriver(TPlayer tPlayer, Race race) {
        super(tPlayer, race);
        this.laps = 0;
        this.pits = 0;
        this.position = 0;
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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
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

    public long getBestLapTime(){
        long bestTime = -1;
        for(RaceLap lap : raceLaps)
        {
            if (bestTime == -1){
                bestTime = lap.getLaptime();
            }
            if(lap.getLaptime() < bestTime && lap.getLaptime() > 0)
            {
                bestTime = lap.getLaptime();
            }
        }

        return bestTime;
    }

    public long getAverageLapTime(){
        if(raceLaps.size() > 1) {
            long totalTime = 0;
            int laps = 0;
            for (RaceLap lap : raceLaps) {
                if (lap.getLapEnd() != null) {
                    totalTime += lap.getLaptime();
                    laps++;
                }
            }
            return totalTime / laps;
        } else {
            return -1;
        }
    }

    public long getPreviousLapTime() {
        if (raceLaps.size() > 1) {
            return raceLaps.get(laps - 2).getLaptime();
        }
        return -1;
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

    public void passPit() {
        if (!getCurrentLap().hasPitted()) {
            pits++;
            RaceAnnouncements.sendPit(this, pits);
            getCurrentLap().setHasPitted();
        }
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

    public long getFinishTime(Instant startTime){
        return Duration.between(startTime, endTime).toMillis();
    }

    public Instant getTimeStamp(int lap, int checkpoint){
        if (lap > race.getTotalLaps()){
            return raceLaps.get(race.getTotalLaps() - 1).getLapEnd();
        }

        return raceLaps.get(lap - 1).getPassedCheckpointTime(checkpoint);
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
