package me.makkuusen.timing.system.track;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class GridManager {

    private final HashMap<UUID, ArmorStand> armorStands = new HashMap<>();

    public GridManager() {
    }

    public void teleportPlayerToGrid(Player player, Location location, Track track) {
        location.setPitch(player.getLocation().getPitch());
        if (!location.isWorldLoaded()) {
            return;
        }
        if (player.getVehicle() != null && player.getVehicle() instanceof Boat boat) {
            boat.remove();
        }
        player.teleport(location);
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        ArmorStand ar = (ArmorStand) location.getWorld().spawnEntity(location.clone().add(0, -1.45, 0), EntityType.ARMOR_STAND);
        ar.setCanMove(false);
        ar.setGravity(false);
        ar.setVisible(false);
        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
            TimeTrialController.lastTimeTrialTrack.put(player.getUniqueId(), track);
            Boat boat = ApiUtilities.spawnBoatAndAddPlayerWithBoatUtils(player, location, track, false);
            ar.addPassenger(boat);
            armorStands.put(player.getUniqueId(), ar);
        }, 2);
    }

    public void startPlayerFromGrid(UUID uuid) {
        ArmorStand ar = armorStands.get(uuid);
        if (ar != null) {
            ar.remove();
        }
    }

    public void clearArmorstands() {
        armorStands.values().stream().forEach(armorStand -> armorStand.remove());
    }
}
