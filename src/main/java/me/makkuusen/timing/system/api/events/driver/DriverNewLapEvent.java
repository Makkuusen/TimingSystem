package me.makkuusen.timing.system.api.events.driver;

import lombok.Getter;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class DriverNewLapEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Driver driver;
    private final Lap lap;

    public DriverNewLapEvent(Driver driver, Lap lap) {
        this.driver = driver;
        this.lap = lap;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
