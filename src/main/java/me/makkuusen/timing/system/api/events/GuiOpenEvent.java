package me.makkuusen.timing.system.api.events;

import me.makkuusen.timing.system.gui.BaseGui;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuiOpenEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private BaseGui gui;
    private boolean cancelled = false;

    public GuiOpenEvent(Player player, BaseGui gui){
        this.player = player;
        this.gui = gui;
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

    public BaseGui getGui() {
        return gui;
    }

    public void setGui(BaseGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }
}
