package me.makkuusen.timing.system.api.events;

import lombok.Getter;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class BoatUtilsAppliedEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled;

    private final Player player;
    private final BoatUtilsMode mode;
    private final Track track;

    public BoatUtilsAppliedEvent(Player player, BoatUtilsMode mode, Track track) {
        this.player = player;
        this.mode = mode;
        this.track = track;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        isCancelled = b;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
