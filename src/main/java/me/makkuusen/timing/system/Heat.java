package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public abstract class Heat {

    TimingSystem plugin;
    Track track;
    private String name;
    private Instant startTime;
    private Instant endTime;
    private HeatState heatState;
    private HashMap<UUID, Spectator> spectators = new HashMap<>();
    private HashMap<UUID, Driver> drivers = new HashMap<>();
    BlockManager blockManager;
    List<Driver> positions = new ArrayList<>();

    public Heat(TimingSystem plugin, Track track, String name){
        this.name = name;
        this.track = track;
        this.heatState = HeatState.SETUP;
        this.blockManager = new BlockManager(track);
    }

    public abstract boolean startHeat();

    public abstract boolean passLap(Driver driver);

    public abstract boolean finishHeat();

    public abstract void updatePositions();

    public void addDrivers(List<Driver> listOfDrivers){
        listOfDrivers.stream().forEach(driver -> drivers.put(driver.getTPlayer().getUniqueId(), driver));
    }

    public void addDriver(Driver driver) {
        drivers.put(driver.getTPlayer().getUniqueId(), driver);
    }

    public List<Participant> getParticipants(){
        List<Participant> rp = new ArrayList<>();
        rp.addAll(drivers.values());
        rp.addAll(spectators.values());
        return rp;
    }
}
