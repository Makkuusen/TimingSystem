package me.makkuusen.timing.system.api.events.driver;

import lombok.Getter;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class DriverFinishLapEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Driver driver;
    private final Lap lap;
    private final Boolean isNewFastestLap;

    public DriverFinishLapEvent(Driver driver, Lap lap, boolean isNewFastestLap) {
        this.driver = driver;
        this.lap = lap;
        this.isNewFastestLap = isNewFastestLap;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
