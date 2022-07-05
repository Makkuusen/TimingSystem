package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
    HashMap<UUID, RaceSpectator> raceSpectators = new HashMap<>();
    HashMap<UUID, RaceDriver> raceDrivers = new HashMap<>();
    List<RaceDriver> livePositioning = new ArrayList<>();

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
        isRunning = true;
        startTime = plugin.currentTime;
        for (RaceDriver rd : raceDrivers.values())
        {
            rd.resetLaps();
            Player player = rd.getTSPlayer().getPlayer();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER,1,1);
        }
        livePositioning = new ArrayList<>();
        livePositioning.addAll(raceDrivers.values());
        updatePositions();
    }

    private void updatePositions() {

        Collections.sort(livePositioning);
        Scoreboard board = getScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(board);
        }

    }

    public long getCurrentTime()
    {
        return Duration.between(startTime, plugin.currentTime).toMillis();
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

    public void passLap(UUID uuid) {
        var raceDriver = raceDrivers.get(uuid);

        if (totalLaps == raceDriver.getLaps())
        {
            raceDriver.setFinished();
            updatePositions();
            Player player = raceDriver.getTSPlayer().getPlayer();
            int pos = getPosition(raceDriver);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER,1,1);
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "title " + player.getName() + " title {\"text\":\"§6-- §eP" + pos + " §6--\"}");

        }
        else
        {
            raceDriver.passLap();
            updatePositions();
        }
    }

    public void passNextCheckpoint(RaceDriver raceDriver){
        raceDriver.getCurrentLap().passNextCheckpoint(plugin.currentTime);
        updatePositions();
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
        for(RaceDriver rd : livePositioning){
            if(score == -9){
                break;
            }
            scoreboard.add("§f" + livePositioning.get(count++).getTSPlayer().getName(), score--);
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

    public HashMap<UUID, RaceSpectator> getRaceSpectators() {
        return raceSpectators;
    }

    public RaceSpectator getRaceSpectator(UUID uuid) {
        return raceSpectators.get(uuid);
    }

    public void removeRaceSpectator(UUID uuid) {
        raceSpectators.remove(uuid);
    }

    public boolean hasRaceSpectator(UUID uuid){
        return raceSpectators.containsKey(uuid);
    }

    private int getPosition(RaceDriver raceDriver) {
        for (int i = 0; i < livePositioning.size(); i++) {

            if (livePositioning.get(i).equals(raceDriver)) {
                return i + 1;
            }
        }
        return -1;
    }
}
