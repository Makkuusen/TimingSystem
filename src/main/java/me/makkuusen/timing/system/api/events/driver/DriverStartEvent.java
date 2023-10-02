package me.makkuusen.timing.system.api.events.driver;

import lombok.Getter;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class DriverStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Driver driver;

    public DriverStartEvent(Driver driver) {
        this.driver = driver;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
