package me.makkuusen.timing.system.api.events;

import me.makkuusen.timing.system.timetrial.TimeTrialAttempt;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TimeTrialAttemptEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final TimeTrialAttempt timeTrialAttempt;


    public TimeTrialAttemptEvent(Player player, TimeTrialAttempt timeTrialAttempt) {
        this.player = player;
        this.timeTrialAttempt = timeTrialAttempt;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public TimeTrialAttempt getTimeTrialAttempt() {
        return timeTrialAttempt;
    }

}
