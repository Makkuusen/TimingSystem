package me.makkuusen.timing.system.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MenuOpenTTEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private boolean isPermitted = false;



    private boolean medalsOverride = false;

    public MenuOpenTTEvent(Player player){
        this.player = player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    public Player getPlayer()
    {
        return player;
    }

    public void isPermitted(boolean isPermitted)
    {
        this.isPermitted = isPermitted;
    }

    public boolean isPermitted()
    {
        return isPermitted;
    }

    public boolean hasMedalsOverride() {
        return medalsOverride;
    }

    public void setMedalsOverride(boolean medalsOverride) {
        this.medalsOverride = medalsOverride;
    }
}
