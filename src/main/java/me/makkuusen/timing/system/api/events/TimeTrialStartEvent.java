package me.makkuusen.timing.system.api.events;

import lombok.Getter;
import me.makkuusen.timing.system.timetrial.TimeTrial;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TimeTrialStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Player player;
    @Getter
    private final TimeTrial timeTrial;

    public TimeTrialStartEvent(Player player, TimeTrial timeTrial) {
        this.player = player;
        this.timeTrial = timeTrial;

    }
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

}
