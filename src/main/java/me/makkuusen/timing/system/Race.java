package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Race {

    public static TimingSystem plugin;
    private int totalLaps;
    private int totalPitstops;
    private Instant startTime;
    private boolean isRunning = false;
    Track track;
    HashMap<UUID, RaceDriver> raceDrivers = new HashMap<>();;
    List<RaceSplits> positions = new ArrayList<>();

    public Race(int totalLaps, int totalPitstops, Track track){
        this.totalLaps = totalLaps;
        this.totalPitstops = totalPitstops;
        this.track = track;
    }

    public void addRaceDriver(TSPlayer TSPlayer)
    {
        RaceDriver raceDriver = new RaceDriver(TSPlayer, this);
        raceDrivers.put(TSPlayer.getUniqueId(), raceDriver);
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
        updatePositions();
    }

    public void updatePositions() {

        Collections.sort(positions);
        Scoreboard board = getScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(board);
        }

    }

    public long getCurrentTime()
    {
        return Duration.between(startTime, plugin.currentTime).toMillis();
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public int getTotalLaps() {
        return totalLaps;
    }

    public List<RaceDriver> getDrivers() {
        return raceDrivers.values().stream().toList();
    }

    public Track getTrack() {
        return track;
    }

    public Long passLap(UUID uuid) {
        var raceDriver = raceDrivers.get(uuid);

        if (!raceDriver.hasPassedAllCheckpoints())
        {
            plugin.sendMessage(raceDriver.getTSPlayer().getPlayer(), "messages.error.timer.missedCheckpoints");
            return null;
        }

        if(totalLaps == raceDriver.getLaps())
        {
            raceDriver.setFinished();
            return raceDriver.getLaptime(raceDriver.getLaps() + 1);
        }
        else
        {
            raceDriver.passLap();
            return raceDriver.getLaptime(raceDriver.getLaps());
        }
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
           names.add(rd.getTSPlayer().getName());
        }

        return String.join(", ", names);
    }

    public Scoreboard getScoreboard()
    {
        SimpleScoreboard scoreboard = new SimpleScoreboard("§e§l" + getScoreboardName());

        int count = 0;
        int score = -1;
        for(RaceSplits rs : positions){
            if(score == -9){
                break;
            }
            scoreboard.add("§f" + positions.get(count++).getRaceDriver().getTSPlayer().getName(), score--);
        }
        scoreboard.build();

        return scoreboard.getScoreboard();
    }

    String getScoreboardName()
    {
        int spacesCount = ((20 - track.getName().length()) / 2) - 1;

        StringBuilder spaces = new StringBuilder();

        for (int i = 0; i < spacesCount; i++)
        {
            spaces.append(" ");
        }

        return spaces + track.getName() + spaces;
    }

    public boolean isRunning(){
        return isRunning;
    }

    public HashMap<UUID, RaceDriver> getRaceDrivers() {
        return raceDrivers;
    }

    public RaceDriver getRaceDriver(UUID uuid) {
        return raceDrivers.get(uuid);
    }

    public void removeRaceDriver(UUID uuid) {
        raceDrivers.remove(uuid);
    }

    public boolean hasRaceDriver(UUID uuid){
        return raceDrivers.containsKey(uuid);
    }
}
