package me.makkuusen.timing.system;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerRegionData {
    private Player player;
    private static final Map<UUID, PlayerRegionData> players = new HashMap<>();
    private final List<Integer> entered = new ArrayList<>();

    public void remove() {
        players.remove(this.getPlayer().getUniqueId());
    }

    public PlayerRegionData(Player player) {
        this.player = player;
    }

    public void updatePlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public static PlayerRegionData instanceOf(Player player) {
        players.putIfAbsent(player.getUniqueId(), new PlayerRegionData(player));
        if (players.containsKey(player.getUniqueId())) {
            players.get(player.getUniqueId()).updatePlayer(player);
        }

        return players.get(player.getUniqueId());
    }

    public List<Integer> getEntered() {
        return this.entered;
    }

}
