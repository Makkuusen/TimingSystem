package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Driver extends Participant implements Comparable<Driver> {

    private Heat heat;
    private boolean finished = false;
    private int position = 0;
    private Instant startTime;
    private Instant endTime;
    private boolean isRunning;
    private List<Lap> laps = new ArrayList<>();

    public Driver(TPlayer tPlayer, Heat heat){
        super(tPlayer);
        this.heat = heat;
    }

    public void finish(){
        endTime = TimingSystem.currentTime;
        isRunning = false;

    }

    public void start(){
        startTime = TimingSystem.currentTime;
        isRunning = true;
    }

    public void startLap(){
        newLap();
    }

    public void passLap(){
        getCurrentLap().setLapEnd(TimingSystem.currentTime);
        EventAnnouncements.sendLapTime(this, getCurrentLap().getLaptime());
        newLap();
    }

    public void reset(){
        isRunning = false;
        endTime = null;
        startTime = null;
        laps = new ArrayList<>();
        finished = false;
    }

    private void newLap(){
        laps.add(new Lap(this, heat.getTrack()));
    }

    public long getFinishTime(){
        return Duration.between(startTime, endTime).toMillis();
    }

    public Lap getCurrentLap(){
        return laps.get(laps.size()-1);
    }

    @Override
    public int compareTo(@NotNull Driver driver) {
        return 0;
    }
}
