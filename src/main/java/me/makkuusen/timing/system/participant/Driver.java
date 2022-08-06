package me.makkuusen.timing.system.participant;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.DriverScoreboard;
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
public abstract class Driver extends Participant implements Comparable<Driver> {

    private int id;
    private Heat heat;
    private Integer position;
    private int startPosition;
    private Instant startTime;
    private Instant endTime;
    private DriverState state;
    private DriverScoreboard scoreboard;
    private List<Lap> laps = new ArrayList<>();

    public Driver(DbRow data) {
        super(data);
        id = data.get("id");
        heat = EventDatabase.getHeat(data.getInt("heatId")).get();
        position = data.getInt("position");
        startPosition = data.getInt("startPosition");
        startTime = data.getLong("startTime") == null ? null : Instant.ofEpochMilli(data.getLong("startTime"));
        endTime = data.getLong("endTime") == null ? null : Instant.ofEpochMilli(data.getLong("endTime"));
        state = isFinished() ? DriverState.FINISHED : DriverState.SETUP;
    }

    public void updateScoreboard(){
        if (getTPlayer().getPlayer() == null) {
            if (scoreboard != null) {
                scoreboard.removeScoreboard();
                scoreboard = null;
            }
            return;
        }
        if (scoreboard == null){
            scoreboard = new DriverScoreboard(getTPlayer().getPlayer(), this);
        }
        scoreboard.setDriverLines(getTPlayer().getPlayer());
    }

    public void finish() {
        finishLap();
        setEndTime(TimingSystem.currentTime);
        state = DriverState.FINISHED;
    }

    public void disqualify(){
        removeUnfinishedLap();
        setEndTime(TimingSystem.currentTime);
        state = DriverState.FINISHED;
    }

    public void start() {
        state = DriverState.RUNNING;
        newLap();
    }

    public void passLap() {
        finishLap();
        newLap();
    }

    private void finishLap() {
        getCurrentLap().setLapEnd(TimingSystem.currentTime);
        if (heat.getFastestLapUUID() == null || getCurrentLap().getLapTime() < heat.getDrivers().get(heat.getFastestLapUUID()).getBestLap().get().getLapTime() || getCurrentLap().equals(heat.getDrivers().get(heat.getFastestLapUUID()).getBestLap().get())) {
            EventAnnouncements.broadcastFastestLap(heat, this, getCurrentLap().getLapTime());
            heat.setFastestLapUUID(getTPlayer().getUniqueId());
        } else {
            EventAnnouncements.sendLapTime(this, getCurrentLap().getLapTime());
        }
        ApiUtilities.msgConsole(getTPlayer().getName() + " finished lap in: " + ApiUtilities.formatAsTime(getCurrentLap().getLapTime()));
    }

    public void reset() {
        state = DriverState.SETUP;
        setEndTime(null);
        setStartTime(null);
        laps = new ArrayList<>();
        setPosition(startPosition);
        removeScoreboard();
        scoreboard = null;
    }

    public void removeScoreboard() {
        if (scoreboard != null) {
            scoreboard.removeScoreboard();
        }
    }

    public boolean isFinished(){
        return endTime != null;
    }

    private void newLap() {
        laps.add(new Lap(this, heat.getEvent().getTrack()));
    }

    public long getFinishTime() {
        return Duration.between(startTime, endTime).toMillis();
    }

    public void setPosition(int position) {
        this.position = position;
        DB.executeUpdateAsync("UPDATE `ts_drivers` SET `position` = " + position + " WHERE `id` = " + id + ";");
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
        DB.executeUpdateAsync("UPDATE `ts_drivers` SET `startPosition` = " + startPosition + " WHERE `id` = " + id + ";");
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
        if (startTime == null) {
            DB.executeUpdateAsync("UPDATE `ts_drivers` SET `startTime` = NULL WHERE `id` = " + id + ";");
        } else {
            DB.executeUpdateAsync("UPDATE `ts_drivers` SET `startTime` = " + startTime.toEpochMilli() + " WHERE `id` = " + id + ";");
        }
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        if (endTime == null) {
            DB.executeUpdateAsync("UPDATE `ts_drivers` SET `endTime` = NULL WHERE `id` = " + id + ";");
        } else {
            DB.executeUpdateAsync("UPDATE `ts_drivers` SET `endTime` = " + endTime.toEpochMilli() + " WHERE `id` = " + id + ";");
        }
    }

    public void setState(DriverState state) {
        this.state = state;
    }

    public @Nullable Lap getCurrentLap() {
        return laps.get(laps.size() - 1);
    }

    public void removeUnfinishedLap(){
        if(getCurrentLap() != null && getCurrentLap().getLapEnd() == null) {
            laps.remove(getCurrentLap());
        }
    }

    public Optional<Lap> getBestLap() {
        if (getLaps().size() == 0) {
            return Optional.empty();
        }
        if (getLaps().get(0).getLapTime() == -1) {
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

    public void onShutdown(){
        if (scoreboard != null) {
            scoreboard.removeScoreboard();
        }
    }
}
