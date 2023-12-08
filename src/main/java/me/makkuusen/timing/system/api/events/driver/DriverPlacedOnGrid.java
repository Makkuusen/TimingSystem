package me.makkuusen.timing.system.api.events.driver;

import lombok.Getter;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class DriverPlacedOnGrid extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Driver driver;
    private final Heat heat;

    public DriverPlacedOnGrid(Driver driver, Heat heat) {
        this.driver = driver;
        this.heat = heat;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
