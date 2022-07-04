package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Race {

    static TimingSystem plugin;
    private int totalLaps = 5;
    private int totalPitstops = 2;
    private Instant startTime;
    boolean isRunning = false;
    RaceTrack track;
    HashMap<UUID, RaceDriver> raceDrivers = new HashMap<>();;
    List<RaceSplits> positions = new ArrayList<>();

    public Race(int totalLaps, int totalPitstops, RaceTrack track){
        this.totalLaps = totalLaps;
        this.totalPitstops = totalPitstops;
        this.track = track;
    }

    public void addRaceDriver(RPlayer rPlayer)
    {
        RaceDriver raceDriver = new RaceDriver(rPlayer, this);
        raceDrivers.put(rPlayer.getUniqueId(), raceDriver);
    }

    public void startRace() {
        startTime = plugin.currentTime;
        List<RaceSplits> pos = new ArrayList<>();
        for (RaceDriver rd : raceDrivers.values())
        {
            rd.resetRaceSplits();
            pos.add(rd.getRaceSplits());
        }
        positions = pos;
        isRunning = true;
    }

    public void updatePositions() {

        Collections.sort(positions);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("§2Positions right now: ");
            int count = 1;
            for (RaceSplits rs : positions) {
                player.sendMessage("§2" + count + ": §a" + rs.getRaceDriver().getRPlayer().getName());
                count++;
            }
        }

    }

    public long getCurrentTime()
    {
        return Duration.between(startTime, Instant.now()).toMillis();
    }

    public long getEndTime(RaceDriver raceDriver)
    {
        return Duration.between(startTime, raceDriver.getEndTime()).toMillis();
    }

    public void finishRaceDriver(UUID uuid) {
        RaceDriver raceDriver = raceDrivers.get(uuid);
        raceDriver.setFinished();
    }

    public void resetRace()
    {
        for (RaceDriver rd : raceDrivers.values())
        {
            rd.reset();
        }
        isRunning = false;
    }

    public int getTotalLaps() {
        return totalLaps;
    }

    public List<RaceDriver> getDrivers() {
        return raceDrivers.values().stream().toList();
    }

    public RaceTrack getTrack() {
        return track;
    }

    public long passLap(UUID uuid) {
        var raceDriver = raceDrivers.get(uuid);
        if(totalLaps == raceDriver.getLaps())
        {
            raceDriver.setFinished();
        }
        else
        {
            raceDriver.passLap();
            if(raceDriver.getLaps() > 0) {
                return raceDriver.getLaptime(raceDriver.getLaps());
            }
        }
        return 0;
    }

    public void setTotalLaps(int totalLaps) {
        this.totalLaps = totalLaps;
    }

    public void setTotalPitstops(int totalPitstops) {
        this.totalPitstops = totalPitstops;
    }

    public int getTotalPitstops() {
        return totalPitstops;
    }

    public String getDriversAsString() {
        List<String> names = new ArrayList<>();
        for (RaceDriver rd : raceDrivers.values())
        {
           names.add(rd.getRPlayer().getName());
        }

        return String.join(", ", names);
    }
}
