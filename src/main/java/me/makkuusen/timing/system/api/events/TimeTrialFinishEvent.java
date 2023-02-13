package me.makkuusen.timing.system.api.events;

import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TimeTrialFinishEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private TimeTrialFinish timeTrialFinish;
    private long oldBestTime;
    private boolean newBestTime;


    public TimeTrialFinishEvent(Player player, TimeTrialFinish timeTrialFinish, long oldBestTime, boolean newBestTime) {
        this.player = player;
        this.timeTrialFinish = timeTrialFinish;
        this.oldBestTime = oldBestTime;
        this.newBestTime = newBestTime;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public TimeTrialFinish getTimeTrialFinish() {
        return timeTrialFinish;
    }

    public long getOldBestTime() {
        return oldBestTime;
    }

    public boolean isNewBestTime() {
        return newBestTime;
    }
}
