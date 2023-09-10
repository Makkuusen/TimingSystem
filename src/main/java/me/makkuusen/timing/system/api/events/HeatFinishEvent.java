package me.makkuusen.timing.system.api.events;

import me.makkuusen.timing.system.heat.Heat;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HeatFinishEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Heat heat;

    public HeatFinishEvent(Heat heat) {
        this.heat = heat;
    }

    public Heat getHeat() {
        return heat;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
