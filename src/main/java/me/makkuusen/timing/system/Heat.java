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

    Track track;
    private Instant startTime;
    private Instant endTime;
    HashMap<UUID, Spectator> spectators = new HashMap<>();
    HashMap<UUID, Driver> drivers = new HashMap<>();
    BlockManager blockManager;

    public Heat(Track track){

    }

    public abstract boolean startHeat();

    public abstract boolean passLap(Driver driver);

    public abstract boolean finishHeat();

    public List<Participant> getParticipants(){
        List<Participant> rp = new ArrayList<>();
        rp.addAll(drivers.values());
        rp.addAll(spectators.values());
        return rp;
    }
}
