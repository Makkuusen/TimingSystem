package me.makkuusen.timing.system.api.events.driver;

import lombok.Getter;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class DriverPassPitEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Driver driver;
    private final Lap lap;
    private final int newPits;

    public DriverPassPitEvent(Driver driver, Lap lap, int newPits) {
        this.driver = driver;
        this.lap = lap;
        this.newPits = newPits;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
