package me.makkuusen.timing.system.heat;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.BlockManager;
import me.makkuusen.timing.system.TaskChainCountdown;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.track.TrackRegion;
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
    private BlockManager BlockManager;
    private List<Driver> startPositions = new ArrayList<>();
    private List<Driver> livePositions = new ArrayList<>();
    private GenericScoreboard scoreboard;
    private Long fastestLap;

    public Heat(DbRow data) {
        id = data.getInt("id");
        event = EventDatabase.getEvent(data.getInt("eventId")).get();
        name = data.getString("name");
        heatState = HeatState.valueOf(data.getString("state"));
        startTime = data.getLong("startTime") == null ? null : Instant.ofEpochMilli(data.getLong("startTime"));
        endTime = data.getLong("endTime") == null ? null : Instant.ofEpochMilli(data.getLong("endTime"));
        if (data.get("fastestLap") == null) {
            fastestLap = 0L;
        } else {
            fastestLap = data.getInt("fastestLap").longValue();
        }
        BlockManager = new BlockManager(event.getTrack());
    }

    public boolean loadHeat() {
        if (this instanceof QualifyHeat && event.getState() != Event.EventState.QUALIFICATION){
            return false;
        }
        if (this instanceof FinalHeat && event.getState() != Event.EventState.FINAL){
            return false;
        }
        if (getHeatState() != HeatState.SETUP) {
            return false;
        }
        getBlockManager().setStartingGrid();
        List<Driver> pos = new ArrayList<>();
        pos.addAll(getStartPositions());
        if(getEvent().getTrack().getGridRegions().values().size() != 0)
        for (Driver d : getStartPositions()){
            Player player = d.getTPlayer().getPlayer();
            if (player != null) {
                TrackRegion gridRegion = getEvent().getTrack().getGridRegions().get(d.getStartPosition());
                if (gridRegion != null) {
                    player.teleport(gridRegion.getSpawnLocation());
                    getEvent().getTrack().spawnBoat(player, gridRegion.getSpawnLocation());
                }
            }
        }
        setLivePositions(pos);
        setHeatState(HeatState.LOADED);
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
        getBlockManager().clearStartingGrid();
        getDrivers().values().stream().forEach(driver -> {
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
        for (Driver rd : getLivePositions())
        {
            rd.setPosition(pos++);
        }
        updateScoreboard();
    }

    public void resetHeat() {
        setHeatState(HeatState.SETUP);
        setStartTime(null);
        setEndTime(null);
        setFastestLap(0);
        setLivePositions(new ArrayList<>());
        getDrivers().values().stream().forEach(driver -> driver.reset());
        ApiUtilities.clearScoreboards();
        ApiUtilities.msgConsole("CLEARED SCOREBOARDS");
    }

    public List<Driver> getResults(){
        if (heatState != HeatState.FINISHED) {
            return List.of();
        }
        return livePositions;
    }

    public void addDriver(Driver driver) {
        drivers.put(driver.getTPlayer().getUniqueId(), driver);
        if (driver.getStartPosition() > 0) {
            startPositions.add(driver.getStartPosition()-1, driver);
        }
    }

    public List<Participant> getParticipants(){
        return event.getParticipants();
    }

    public void updateScoreboard(){
        Scoreboard board = scoreboard.getScoreboard();
        getEvent().getParticipants().stream()
                .filter(participant -> participant.getTPlayer().getPlayer() != null)
                .forEach(participant -> participant.getTPlayer().getPlayer().setScoreboard(board));
    }

    public boolean allDriversFinished(){
        for (Driver d : getDrivers().values()){
            if (!d.isFinished()){
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
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `startTime` = "+ startTime.toEpochMilli() + " WHERE `id` = " + id + ";");
        }
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        if (endTime == null) {
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `endTime` = NULL WHERE `id` = " + id + ";");
        } else {
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `endTime` = "+ endTime.toEpochMilli() + " WHERE `id` = " + id + ";");
        }
    }

    public void setFastestLap(long fastestLap) {
        this.fastestLap = fastestLap;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `fastestLap` = " + fastestLap + " WHERE `id` = " + id + ";");
    }
}
