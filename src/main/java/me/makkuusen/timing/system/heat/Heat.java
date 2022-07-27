package me.makkuusen.timing.system.heat;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TaskChainCountdown;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.track.GridManager;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

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
    private String name;
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
    private Integer startDelay = 20*2;
    private SpectatorScoreboard scoreboard;

    public Heat (DbRow data) {
        id = data.getInt("id");
        event = EventDatabase.getEvent(data.getInt("eventId")).get();
        name = data.getString("name");
        heatState = HeatState.valueOf(data.getString("state"));
        startTime = data.getLong("startTime") == null ? null : Instant.ofEpochMilli(data.getLong("startTime"));
        endTime = data.getLong("endTime") == null ? null : Instant.ofEpochMilli(data.getLong("endTime"));
        timeLimit = data.get("timeLimit") == null ? null : data.getInt("timeLimit");
        totalLaps = data.get("totalLaps") == null ? null : data.getInt("totalLaps");
        totalPits = data.get("totalPitstops") == null ? null : data.getInt("totalPitstops");
        fastestLapUUID = data.getString("fastestLapUUID") == null ? null : UUID.fromString(data.getString("fastestLapUUID"));
        gridManager = new GridManager();
    }

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
        if (getEvent().getTrack().getGridRegions().values().size() != 0)
            for (Driver d : getStartPositions()) {
                Player player = d.getTPlayer().getPlayer();
                if (player != null) {
                    TrackRegion gridRegion = getEvent().getTrack().getGridRegions().get(d.getStartPosition());
                    if (gridRegion != null) {
                        gridManager.teleportPlayerToGrid(player, gridRegion);
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
                ApiUtilities.clearScoreboards();
                ApiUtilities.msgConsole("CLEARED SCOREBOARDS");
            }
        }, 200);

        if (this instanceof QualifyHeat) {
            getEvent().getEventResults().reportQualyResults(getLivePositions());
        } else if (this instanceof FinalHeat) {
            getEvent().getEventResults().reportFinalResults(getLivePositions());
        }
        getDrivers().values().stream().forEach(driver -> EventDatabase.removePlayerFromRunningHeat(driver.getTPlayer().getUniqueId()));

        //Dump all laps to database
        getDrivers().values().stream().forEach(driver -> driver.getLaps().forEach(EventDatabase::lapNew));

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

    public void resetHeat() {
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
        ApiUtilities.clearScoreboards();
        ApiUtilities.msgConsole("CLEARED SCOREBOARDS");
    }

    public List<Driver> getResults() {
        if (heatState != HeatState.FINISHED) {
            return List.of();
        }
        return livePositions;
    }

    public void addDriver(Driver driver) {
        drivers.put(driver.getTPlayer().getUniqueId(), driver);
        if (driver.getStartPosition() > 0) {
            startPositions.add(driver.getStartPosition() - 1, driver);
        }
    }

    public List<Participant> getParticipants(){
        List<Participant> participants = new ArrayList<>();
        participants.addAll(drivers.values());
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

    public void setTotalPits(int totalPits) {
        this.totalPits = totalPits;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `totalPitstops` = " + totalPits + " WHERE `id` = " + getId() + ";");
    }
}
