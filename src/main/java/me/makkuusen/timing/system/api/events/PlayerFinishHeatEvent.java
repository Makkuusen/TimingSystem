package me.makkuusen.timing.system.api.events;

import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Participant;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerFinishHeatEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Driver driver;
    private final Heat heat;

    public PlayerFinishHeatEvent(Heat heat, Driver driver) {
        this.heat = heat;
        this.driver = driver;
    }

    public Heat getHeat() {
        return heat;
    }

    public Driver getDriver() {
        return driver;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
