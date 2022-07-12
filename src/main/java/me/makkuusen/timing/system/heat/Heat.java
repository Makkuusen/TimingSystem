package me.makkuusen.timing.system.heat;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.BlockManager;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.participant.Spectator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

    private Event event;
    private String name;
    private Instant startTime;
    private Instant endTime;
    private HeatState heatState;
    private HashMap<UUID, Spectator> spectators = new HashMap<>();
    private HashMap<UUID, Driver> drivers = new HashMap<>();
    private BlockManager BlockManager;
    private List<Driver> startPositions = new ArrayList<>();
    private List<Driver> livePositions = new ArrayList<>();
    private GenericScoreboard scoreboard;

    public Heat(Event event, String name){
        this.event = event;
        this.name = name;
        this.heatState = HeatState.SETUP;
        this.BlockManager = new BlockManager(event.getTrack());
    }

    public boolean loadHeat() {
        if (getHeatState() != HeatState.SETUP) {
            return false;
        }
        getBlockManager().setStartingGridBarriers();
        setHeatState(HeatState.LOADED);
        List<Driver> pos = new ArrayList<>();
        pos.addAll(getStartPositions());
        for(Driver d : getStartPositions()){
            Player player = d.getTPlayer().getPlayer();
            if (player != null){
                Location loc = getEvent().getTrack().getGridRegions().get(d.getStartPosition()).getSpawnLocation();
                player.teleport(loc);
                getEvent().getTrack().spawnBoat(player, loc);
            }
        }
        setLivePositions(pos);
        updateScoreboard();
        ApiUtilities.msgConsole("Drivers: " + getDrivers().values().size());
        ApiUtilities.msgConsole("StartPositions: " + getStartPositions().size());
        ApiUtilities.msgConsole("LivePositions: " + getLivePositions().size());
        return true;
    }

    public boolean startHeat() {
        if (getHeatState() != HeatState.LOADED) {
            return false;
        }
        setHeatState(HeatState.RACING);
        updateScoreboard();
        setStartTime(TimingSystem.currentTime);
        getBlockManager().clearStartingGridBarriers();
        EventAnnouncements.sendStartSound(this);
        getDrivers().values().stream().forEach(driver -> driver.setStartTime(TimingSystem.currentTime));
        return true;
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
        getDrivers().values().stream().forEach(driver -> driver.reset());
        ApiUtilities.clearScoreboards();
        ApiUtilities.msgConsole("CLEARED SCOREBOARDS");
    }

    public void reportResults(){

    }

    public void addDrivers(List<Driver> listOfDrivers){
        listOfDrivers.stream().forEach(driver -> {
            drivers.put(driver.getTPlayer().getUniqueId(), driver);
        });
    }

    public void addDriver(Driver driver) {
        drivers.put(driver.getTPlayer().getUniqueId(), driver);
        if (driver.getStartPosition() > 0) {
            startPositions.add(driver.getStartPosition()-1, driver);
        }
    }

    public List<Participant> getParticipants(){
        List<Participant> rp = new ArrayList<>();
        rp.addAll(drivers.values());
        rp.addAll(spectators.values());
        return rp;
    }

    public void updateScoreboard(){
        Scoreboard board = scoreboard.getScoreboard();
        getEvent().getSpectators().stream()
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
}
