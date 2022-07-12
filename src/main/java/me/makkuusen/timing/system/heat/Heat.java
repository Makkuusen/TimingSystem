package me.makkuusen.timing.system.heat;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.BlockManager;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.participant.Spectator;
import me.makkuusen.timing.system.track.Track;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public abstract class Heat {

    Event event;
    Track track;
    private String name;
    private Instant startTime;
    private Instant endTime;
    private HeatState heatState;
    private HashMap<UUID, Spectator> spectators = new HashMap<>();
    private HashMap<UUID, Driver> drivers = new HashMap<>();
    me.makkuusen.timing.system.BlockManager BlockManager;
    List<Driver> positions = new ArrayList<>();

    public Heat(Event event, Track track, String name){
        this.event = event;
        this.name = name;
        this.track = track;
        this.heatState = HeatState.SETUP;
        this.BlockManager = new BlockManager(track);
    }

    public abstract boolean startHeat();

    public abstract boolean passLap(Driver driver);

    public abstract boolean finishHeat();

    public abstract void updatePositions();

    public abstract void resetHeat();

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
