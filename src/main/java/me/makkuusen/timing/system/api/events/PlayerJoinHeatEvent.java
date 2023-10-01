package me.makkuusen.timing.system.api.events;

import lombok.Getter;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.heat.Heat;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerJoinHeatEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final TPlayer tPlayer;
    private final Heat heat;

    public PlayerJoinHeatEvent(TPlayer tPlayer, Heat heat) {
        this.player = tPlayer.getPlayer();
        this.tPlayer = tPlayer;
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
