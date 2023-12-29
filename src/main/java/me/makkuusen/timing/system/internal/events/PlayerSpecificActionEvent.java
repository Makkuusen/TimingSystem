package me.makkuusen.timing.system.internal.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class PlayerSpecificActionEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final UUID playerUUID;
    private final String command;

    public PlayerSpecificActionEvent(Player player, String command) {
        this.playerUUID = player.getUniqueId();
        this.command = command;
    }

    public PlayerSpecificActionEvent(UUID uuid, String command) {
        this.playerUUID = uuid;
        this.command = command;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
