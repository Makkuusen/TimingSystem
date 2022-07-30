package me.makkuusen.timing.system.heat;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TaskChainCountdown;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.event.EventResults;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.track.GridManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public abstract class Heat {

    private int id;
    private Event event;
    private Integer heatNumber;
    private Instant startTime;
    private Instant endTime;
    private HeatState heatState;
    private HashMap<UUID, Driver> drivers = new HashMap<>();
    private GridManager gridManager;
    private List<Driver> startPositions = new ArrayList<>();
    private List<Driver> livePositions = new ArrayList<>();
    private UUID fastestLapUUID;
    private Integer timeLimit;
    private Integer totalLaps;
    private Integer totalPits;
    private Integer startDelay;
    private Integer maxDrivers;
    private SpectatorScoreboard scoreboard;

    public Heat(DbRow data) {
        id = data.getInt("id");
        event = EventDatabase.getEvent(data.getInt("eventId")).get();
        heatState = HeatState.valueOf(data.getString("state"));
        heatNumber = data.getInt("heatNumber");
        startTime = data.getLong("startTime") == null ? null : Instant.ofEpochMilli(data.getLong("startTime"));
        endTime = data.getLong("endTime") == null ? null : Instant.ofEpochMilli(data.getLong("endTime"));
        timeLimit = data.get("timeLimit") == null ? null : data.getInt("timeLimit");
        totalLaps = data.get("totalLaps") == null ? null : data.getInt("totalLaps");
        totalPits = data.get("totalPitstops") == null ? null : data.getInt("totalPitstops");
        maxDrivers = data.get("maxDrivers") == null ? null : data.getInt("maxDrivers");
        startDelay = data.get("startDelay") == null ? TimingSystem.configuration.getStartDelay() : data.getInt("startDelay");
        fastestLapUUID = data.getString("fastestLapUUID") == null ? null : UUID.fromString(data.getString("fastestLapUUID"));
        gridManager = new GridManager();
    }

    public abstract String getName();

    public boolean loadHeat() {
        if (this instanceof QualifyHeat && event.getState() != Event.EventState.QUALIFICATION) {
            return false;
        }
        if (this instanceof FinalHeat && event.getState() != Event.EventState.FINAL) {
            return false;
        }
        if (getHeatState() != HeatState.SETUP) {
            return false;
        }
        List<Driver> pos = new ArrayList<>();
        pos.addAll(getStartPositions());
        if (getEvent().getTrack().getGridLocations().values().size() != 0)
            for (Driver d : getStartPositions()) {
                Player player = d.getTPlayer().getPlayer();
                if (player != null) {
                    Location grid = getEvent().getTrack().getGridLocation(d.getStartPosition());
                    if (grid != null) {
                        gridManager.teleportPlayerToGrid(player, grid);
                    }
                }
                EventDatabase.addPlayerToRunningHeat(d);
            }
        setLivePositions(pos);
        setHeatState(HeatState.LOADED);
        scoreboard = new SpectatorScoreboard(this);
        updateScoreboard();
        ApiUtilities.msgConsole("Drivers: " + getDrivers().values().size());
        ApiUtilities.msgConsole("StartPositions: " + getStartPositions().size());
        ApiUtilities.msgConsole("LivePositions: " + getLivePositions().size());
        return true;
    }

    public boolean startCountdown() {
        if (getHeatState() != HeatState.LOADED) {
            return false;
        }
        TaskChainCountdown.countdown(this);
        return true;
    }

    public void startHeat() {
        setHeatState(HeatState.RACING);
        updateScoreboard();
        setStartTime(TimingSystem.currentTime);
        getDrivers().values().stream().forEach(driver -> {
            gridManager.startPlayerFromGrid(driver.getTPlayer().getUniqueId());
            driver.setStartTime(TimingSystem.currentTime);
            EventDatabase.addPlayerToRunningHeat(driver);
            if (driver.getTPlayer().getPlayer() != null) {
                driver.getTPlayer().getPlayer().playSound(driver.getTPlayer().getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1, 1);
            }
        });
    }

    public abstract boolean passLap(Driver driver);

    public boolean finishHeat() {
        if (getHeatState() != HeatState.RACING) {
            return false;
        }
        setHeatState(HeatState.FINISHED);
        setEndTime(TimingSystem.currentTime);
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(TimingSystem.getPlugin(), new Runnable() {
            public void run() {
                getDrivers().values().forEach(driver -> driver.removeScoreboard());
                scoreboard.removeScoreboard();
                ApiUtilities.msgConsole("CLEARED SCOREBOARDS");
            }
        }, 200);

        getDrivers().values().stream().forEach(driver -> {
            EventDatabase.removePlayerFromRunningHeat(driver.getTPlayer().getUniqueId());
            if (driver.getEndTime() == null) {
                driver.setEndTime(TimingSystem.currentTime);
                driver.setRunning(false);
            }
        });

        //Dump all laps to database
        getDrivers().values().stream().forEach(driver -> driver.getLaps().forEach(EventDatabase::lapNew));

        if (this instanceof QualifyHeat qualifyHeat) {
            EventAnnouncements.broadcastHeatResult(EventResults.generateQualyHeatResults(qualifyHeat), qualifyHeat);
        } else {
            EventAnnouncements.broadcastHeatResult(EventResults.generateFinalHeatResults((FinalHeat) this), this);
        }

        return true;
    }

    public void updatePositions() {
        Collections.sort(getLivePositions());
        int pos = 1;
        for (Driver rd : getLivePositions()) {
            rd.setPosition(pos++);
        }
        updateScoreboard();
    }

    public boolean resetHeat() {
        if (getHeatState() == HeatState.FINISHED) {
            return false;
        }
        setHeatState(HeatState.SETUP);
        setStartTime(null);
        setEndTime(null);
        setFastestLapUUID(null);
        setLivePositions(new ArrayList<>());
        gridManager.clearArmorstands();
        getDrivers().values().stream().forEach(driver -> {
            driver.reset();
            EventDatabase.removePlayerFromRunningHeat(driver.getTPlayer().getUniqueId());
        });
        scoreboard.removeScoreboard();
        ApiUtilities.msgConsole("CLEARED SCOREBOARDS");
        return true;
    }

    public List<Driver> getResults() {
        if (heatState != HeatState.FINISHED) {
            return List.of();
        }
        return livePositions;
    }

    public int getMaxDrivers(){
        if (maxDrivers != null) {
            return maxDrivers;
        }
        return getEvent().getTrack().getGrids().size();
    }

    public void addDriver(Driver driver) {
        drivers.put(driver.getTPlayer().getUniqueId(), driver);
        if (driver.getStartPosition() > 0) {
            startPositions.add(driver.getStartPosition() - 1, driver);
        }
        if (!event.getSpectators().containsKey(driver.getTPlayer().getUniqueId())) {
            event.addSpectator(driver.getTPlayer().getUniqueId());
        }
    }

    public boolean removeDriver(Driver driver) {
        if (driver.getHeat().getHeatState() != HeatState.SETUP) {
            return false;
        }
        try {
            DB.executeUpdate("UPDATE `ts_drivers` SET `isRemoved` = 1 WHERE `id` = " + driver.getId() + ";");
            drivers.remove(driver.getTPlayer().getUniqueId());
            startPositions.remove(driver);
            int startPos = driver.getStartPosition();
            for (Driver d : startPositions) {
                if (d.getStartPosition() > startPos){
                    d.setStartPosition(d.getStartPosition() - 1);
                    d.setPosition(d.getPosition() - 1);
                }
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Participant> getParticipants(){
        List<Participant> participants = new ArrayList<>();
        participants.addAll(getEvent().getSpectators().values());
        return participants;
    }

    public void updateScoreboard() {
        livePositions.stream().forEach(driver -> driver.updateScoreboard());
        scoreboard.updateScoreboard();
    }

    public boolean allDriversFinished() {
        for (Driver d : getDrivers().values()) {
            if (!d.isFinished()) {
                return false;
            }
        }
        return true;
    }

    public void setHeatState(HeatState state) {
        this.heatState = state;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `state` = '" + state.name() + "' WHERE `id` = " + id + ";");
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
        if (startTime == null) {
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `startTime` = NULL WHERE `id` = " + id + ";");
        } else {
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `startTime` = " + startTime.toEpochMilli() + " WHERE `id` = " + id + ";");
        }
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        if (endTime == null) {
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `endTime` = NULL WHERE `id` = " + id + ";");
        } else {
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `endTime` = " + endTime.toEpochMilli() + " WHERE `id` = " + id + ";");
        }
    }

    public void setFastestLapUUID(UUID fastestLapUUID) {
        this.fastestLapUUID = fastestLapUUID;
        if (fastestLapUUID == null) {
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `fastestLapUUID` = NULL WHERE `id` = " + id + ";");
        } else {
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `fastestLapUUID` = '" + fastestLapUUID + "' WHERE `id` = " + id + ";");
        }
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `timeLimit` = " + timeLimit + " WHERE `id` = " + getId() + ";");
    }

    public void setStartDelay(int startDelay) {
        this.startDelay = startDelay;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `startDelay` = " + startDelay + " WHERE `id` = " + getId() + ";");
    }

    public void setTotalLaps(int totalLaps){
        this.totalLaps = totalLaps;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `totalLaps` = " + totalLaps + " WHERE `id` = " + getId() + ";");
    }

    public void setMaxDrivers(int maxDrivers){
        this.maxDrivers = maxDrivers;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `maxDrivers` = " + maxDrivers + " WHERE `id` = " + getId() + ";");
    }

    public void setTotalPits(int totalPits) {
        this.totalPits = totalPits;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `totalPitstops` = " + totalPits + " WHERE `id` = " + getId() + ";");
    }

    public void setHeatNumber(int heatNumber) {
        this.heatNumber = heatNumber;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `heatNumber` = " + heatNumber + " WHERE `id` = " + getId() + ";");
    }
}
