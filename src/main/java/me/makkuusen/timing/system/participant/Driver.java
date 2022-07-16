package me.makkuusen.timing.system.participant;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.Lap;
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

    private Heat heat;
    private boolean finished = false;
    private int position = 0;
    private int startPosition = -1;
    private Instant startTime;
    private Instant endTime;
    private boolean isRunning = false;
    private List<Lap> laps = new ArrayList<>();

    public Driver(TPlayer tPlayer, Heat heat){
        super(tPlayer);
        this.heat = heat;
    }

    public void finish(){
        getCurrentLap().setLapEnd(TimingSystem.currentTime);
        EventAnnouncements.sendLapTime(this, getCurrentLap().getLapTime());
        endTime = TimingSystem.currentTime;
        isRunning = false;
        finished = true;
    }

    public void start(){
        isRunning = true;
        newLap();
    }

    public void startLap(){
        newLap();
    }

    public void passLap(){
        getCurrentLap().setLapEnd(TimingSystem.currentTime);
        EventAnnouncements.sendLapTime(this, getCurrentLap().getLapTime());
        ApiUtilities.msgConsole(getTPlayer().getName() + " finished lap in: " + ApiUtilities.formatAsTime(getCurrentLap().getLapTime()));
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
        laps.add(new Lap(this, heat.getEvent().getTrack()));
    }

    public long getFinishTime(){
        return Duration.between(startTime, endTime).toMillis();
    }

    public @Nullable Lap getCurrentLap(){
        return laps.get(laps.size()-1);
    }

    public Optional<Lap> getBestLap() {
        if (getLaps().size() == 0){
            return Optional.empty();
        }
        if (getLaps().get(0).getLapTime() == -1){
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

    @Override
    public int compareTo(@NotNull Driver o) {
        return 0;
    }
}
