package me.makkuusen.timing.system.api.events.driver;

import lombok.Getter;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Getter
public class DriverPassCheckpointEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Driver driver;
    private final Lap lap;
    private final TrackRegion checkpointRegion;
    private final Instant time;

    public DriverPassCheckpointEvent(Driver driver, Lap lap, TrackRegion checkpointRegion, Instant time) {
        this.driver = driver;
        this.lap = lap;
        this.checkpointRegion = checkpointRegion;
        this.time = time;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
