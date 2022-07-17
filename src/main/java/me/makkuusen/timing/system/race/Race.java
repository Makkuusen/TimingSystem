package me.makkuusen.timing.system.race;

import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.BlockManager;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Race {

    public static TimingSystem plugin;
    private int totalLaps;
    private int totalPits;
    private Instant startTime;
    private HeatState heatState;
    Track track;
    BlockManager BlockManager;
    RaceScoreboard raceScoreboard;
    HashMap<UUID, RaceSpectator> raceSpectators = new HashMap<>();
    HashMap<UUID, RaceDriver> raceDrivers = new HashMap<>();
    List<RaceDriver> livePositioning = new ArrayList<>();

    public Race(int totalLaps, int totalPits, Track track){
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;
        this.track = track;
        this.heatState = HeatState.SETUP;
        this.BlockManager = new BlockManager(track);    }

    public void addRaceDriver(TPlayer TPlayer)
    {
        RaceDriver raceDriver = new RaceDriver(TPlayer, this);
        raceDrivers.put(TPlayer.getUniqueId(), raceDriver);
    }

    public void loadRace() {
        if (heatState.equals(HeatState.SETUP)) {
            livePositioning = new ArrayList<>();
            livePositioning.addAll(raceDrivers.values());
            BlockManager.setStartingGrid();
            raceScoreboard = new RaceScoreboard(this);
        }
    }

    public void startRace() {
        heatState = HeatState.RACING;
        startTime = plugin.currentTime;
        BlockManager.clearStartingGrid();
        for (RaceDriver rd : raceDrivers.values())
        {
            rd.resetLaps();
            Player player = rd.getTSPlayer().getPlayer();
            if (player != null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1, 1);
            }
        }

        updatePositions();
    }

    private void updatePositions() {

        Collections.sort(livePositioning);
        int pos = 1;
        for (RaceDriver rd : livePositioning)
        {
            rd.setPosition(pos++);
        }
        Scoreboard board = raceScoreboard.getScoreboard();
        for (RaceParticipant raceParticipant : getRaceParticipants()) {
            if (raceParticipant.getPlayer() != null) {
                raceParticipant.getPlayer().setScoreboard(board);
            }
        }
    }

    public void resetRace()
    {
        for (RaceDriver rd : raceDrivers.values())
        {
            rd.reset();
        }
        heatState = HeatState.SETUP;
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

        if (totalLaps <= raceDriver.getLaps() && totalPits <= raceDriver.getPits())
        {
            raceDriver.setFinished();
            updatePositions();
            Player player = raceDriver.getTSPlayer().getPlayer();
            int pos = getPosition(raceDriver);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER,1,1);
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "title " + player.getName() + " title {\"text\":\"§6-- §eP" + pos + " §6--\"}");
            RaceAnnouncements.broadcastFinish(raceDriver, raceDriver.getFinishTime(startTime));
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

    public void setTotalPits(int totalPits) {
        this.totalPits = totalPits;
    }

    public int getTotalPits() {
        return totalPits;
    }

    public String getDriversAsString() {
        List<String> names = new ArrayList<>();
        for (RaceDriver rd : raceDrivers.values())
        {
           names.add(rd.getTSPlayer().getName());
        }

        return String.join(", ", names);
    }

    public HeatState getRaceState() {
        return heatState;
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

    public List<RaceParticipant> getRaceParticipants(){
        List<RaceParticipant> rp = new ArrayList<>();
        rp.addAll(raceDrivers.values());
        rp.addAll(raceSpectators.values());
        return rp;
    }
}
