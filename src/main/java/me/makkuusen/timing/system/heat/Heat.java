package me.makkuusen.timing.system.heat;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import co.aikar.taskchain.TaskChain;
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
import me.makkuusen.timing.system.participant.DriverState;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.track.GridManager;
import me.makkuusen.timing.system.track.TrackLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Heat {

    private int id;
    private Event event;
    private Round round;
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
    private Instant lastScoreboardUpdate = Instant.now();
    private long updateScoreboardDelay = 300;

    public Heat(DbRow data, Round round) {
        id = data.getInt("id");
        this.event = round.getEvent();
        this.round = round;
        heatState = HeatState.valueOf(data.getString("state"));
        heatNumber = data.getInt("heatNumber");
        startTime = data.getLong("startTime") == null ? null : Instant.ofEpochMilli(data.getLong("startTime"));
        endTime = data.getLong("endTime") == null ? null : Instant.ofEpochMilli(data.getLong("endTime"));
        timeLimit = data.get("timeLimit") == null ? null : data.getInt("timeLimit");
        totalLaps = data.get("totalLaps") == null ? null : data.getInt("totalLaps");
        totalPits = data.get("totalPitstops") == null ? null : data.getInt("totalPitstops");
        maxDrivers = data.get("maxDrivers") == null ? null : data.getInt("maxDrivers");
        startDelay = data.get("startDelay") == null ? round instanceof FinalRound ? TimingSystem.configuration.getFinalStartDelayInMS() : TimingSystem.configuration.getQualyStartDelayInMS() : data.getInt("startDelay");
        fastestLapUUID = data.getString("fastestLapUUID") == null ? null : UUID.fromString(data.getString("fastestLapUUID"));
        gridManager = new GridManager();
    }

    public String getName(){
        if (round instanceof QualificationRound) {
            return "R" + round.getRoundIndex() + "Q" + getHeatNumber();
        } else {
            return "R" + round.getRoundIndex() + "F" + getHeatNumber();
        }
    }

    public boolean loadHeat() {
        if (event.getEventSchedule().getCurrentRound() != round.getRoundIndex()) {
            if (round.getRoundIndex() == 1) {
                event.start();
            } else {
                return false;
            }

        }
        if (getHeatState() != HeatState.SETUP) {
            return false;
        }
        List<Driver> pos = new ArrayList<>();
        pos.addAll(getStartPositions());
        if (getEvent().getTrack().getTrackLocations(TrackLocation.Type.GRID).size() != 0)
            for (Driver d : getStartPositions()) {
                Player player = d.getTPlayer().getPlayer();
                if (player != null) {
                    Location grid = getEvent().getTrack().getTrackLocation(TrackLocation.Type.GRID, d.getStartPosition()).get().getLocation();
                    if (grid != null) {
                        gridManager.teleportPlayerToGrid(player, grid, getEvent().getTrack());
                    }
                }
                EventDatabase.addPlayerToRunningHeat(d);
                d.setState(DriverState.LOADED);
            }
        setLivePositions(pos);
        setHeatState(HeatState.LOADED);
        scoreboard = new SpectatorScoreboard(this);
        updateScoreboard();
        return true;
    }

    public boolean reloadHeat(){
        if (!resetHeat()) {
            return false;
        }
        if (!loadHeat()) {
            return false;
        }
        return true;
    }

    public boolean startCountdown() {
        if (getHeatState() != HeatState.LOADED) {
            return false;
        }
        TaskChainCountdown.countdown(this, 5);
        return true;
    }

    public void startHeat() {

        setHeatState(HeatState.RACING);
        updateScoreboard();
        setStartTime(TimingSystem.currentTime);
        if (round instanceof QualificationRound) {
            startWithDelay(getStartDelay(), true);
            return;
        }
        getDrivers().values().stream().forEach(driver -> driver.setStartTime(TimingSystem.currentTime));
        startWithDelay(getStartDelay(), false);
    }

    private void startWithDelay(long startDelayMS, boolean setStartTime){
        TaskChain<?> chain = TimingSystem.newChain();
        for (Driver driver : getStartPositions()) {
            chain.sync(() -> {
                getGridManager().startPlayerFromGrid(driver.getTPlayer().getUniqueId());
                if (setStartTime) {
                    driver.setStartTime(TimingSystem.currentTime);
                }
                driver.setState(DriverState.STARTING);
                if (driver.getTPlayer().getPlayer() != null) {
                    driver.getTPlayer().getPlayer().playSound(driver.getTPlayer().getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1, 1);
                }
            });
            if (startDelayMS > 0) {
                //Start delay in ms divided by 50ms to get ticks
                chain.delay((int)(startDelayMS / 50));
            }
        }
        chain.execute();
    }

    public boolean passLap(Driver driver) {
        if (round instanceof QualificationRound) {
            return QualifyHeat.passQualyLap(driver);
        } else {
            return FinalHeat.passLap(driver);
        }
    }

    public boolean finishHeat() {
        if (getHeatState() != HeatState.RACING) {
            return false;
        }
        updatePositions();
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
                driver.removeUnfinishedLap();
                if (driver.getLaps().size() > 0) {
                    driver.setEndTime(driver.getCurrentLap().getLapEnd());
                } else {
                    driver.setEndTime(TimingSystem.currentTime);
                }
                driver.setState(DriverState.FINISHED);
            }
        });

        //Dump all laps to database
        getDrivers().values().stream().forEach(driver -> driver.getLaps().forEach(EventDatabase::lapNew));

        var heatResults = EventResults.generateHeatResults(this);
        EventAnnouncements.broadcastHeatResult(heatResults,this);

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
        if (scoreboard != null) {
            scoreboard.removeScoreboard();
        }
        ApiUtilities.msgConsole("CLEARED SCOREBOARDS");
        return true;
    }

    public int getMaxDrivers(){
        if (maxDrivers != null) {
            return maxDrivers;
        }
        return getEvent().getTrack().getTrackLocations(TrackLocation.Type.GRID).size();
    }

    public void addDriver(Driver driver) {
        drivers.put(driver.getTPlayer().getUniqueId(), driver);
        if (driver.getStartPosition() > 0) {
            startPositions.add(driver);
            Collections.sort(startPositions, Comparator.comparingInt(Driver::getStartPosition));
        }
        if (!event.getSpectators().containsKey(driver.getTPlayer().getUniqueId())) {
            event.addSpectator(driver.getTPlayer().getUniqueId());
        }
    }

    public boolean removeDriver(Driver driver) {
        if (driver.getHeat().getHeatState() != HeatState.SETUP && driver.getHeat().getHeatState() != HeatState.LOADED) {
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

    public boolean setDriverPosition(Driver driver, int newStartPosition){
        if (driver.getHeat().getHeatState() != HeatState.SETUP && driver.getHeat().getHeatState() != HeatState.LOADED) {
            return false;
        }

        int prevPos = driver.getStartPosition();
        for (Driver d : startPositions) {
            if (newStartPosition < prevPos) {
                if (d.getStartPosition() >= newStartPosition && d.getStartPosition() < prevPos) {
                    d.setStartPosition(d.getStartPosition() + 1);
                    d.setPosition(d.getPosition() + 1);
                }
            } else if (newStartPosition > prevPos){
                if (d.getStartPosition() > prevPos && d.getStartPosition() <= newStartPosition) {
                    d.setStartPosition(d.getStartPosition() - 1);
                    d.setPosition(d.getPosition() - 1);
                }
            } else {
                return false;
            }
        }
        driver.setStartPosition(newStartPosition);
        driver.setPosition(newStartPosition);
        Collections.sort(startPositions, Comparator.comparingInt(Driver::getStartPosition));
        return true;
    }

    public boolean disqualifyDriver(Driver driver){
        if (!driver.getHeat().isActive()) {
            return false;
        }
        driver.disqualify();
        if (noDriversRunning()) {
            finishHeat();
        }
        return true;
    }

    public List<Participant> getParticipants(){
        List<Participant> participants = new ArrayList<>();
        participants.addAll(getEvent().getSpectators().values());
        return participants;
    }

    public void updateScoreboard() {
        if (Duration.between(lastScoreboardUpdate, TimingSystem.currentTime).toMillis() > updateScoreboardDelay) {
            livePositions.stream().forEach(driver -> driver.updateScoreboard());
            scoreboard.updateScoreboard();
            lastScoreboardUpdate = TimingSystem.currentTime;
        }
    }

    public boolean noDriversRunning() {
        for (Driver d : getDrivers().values()) {
            if (d.isRunning()) {
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

    public void setStartDelayInTicks(int startDelay) {
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

    public boolean isActive() {
        return getHeatState() == HeatState.LOADED || getHeatState() == HeatState.RACING || getHeatState() == HeatState.STARTING;
    }

    public boolean isRacing() {
        return getHeatState() == HeatState.RACING ||getHeatState() == HeatState.STARTING;
    }

    public void onShutdown() {
        gridManager.clearArmorstands();
        if (scoreboard != null) {
            scoreboard.removeScoreboard();
        }
        drivers.values().forEach(driver -> driver.onShutdown());
    }

    public void reverseGrid(Integer percentage) {
        if (getHeatState() != HeatState.SETUP && getHeatState() != HeatState.LOADED) {
            return;
        }
        int reverseSize = Math.min((getStartPositions().size() * percentage)/100, getStartPositions().size());

        if (reverseSize == 0) {
            return;
        }

        for (Driver driver : getStartPositions()) {

            int driverPos = driver.getStartPosition();
            if (driverPos > reverseSize) {
                break;
            }
            int newPos = reverseSize - (driverPos - 1);
            driver.setStartPosition(newPos);
            driver.setPosition(newPos);
        }
        Collections.sort(startPositions, Comparator.comparingInt(Driver::getStartPosition));
        Collections.sort(livePositions, Comparator.comparingInt(Driver::getPosition));
    }
}
