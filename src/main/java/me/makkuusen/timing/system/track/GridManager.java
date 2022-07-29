package me.makkuusen.timing.system.track;

import me.makkuusen.timing.system.ApiUtilities;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class GridManager {

    private HashMap<UUID, ArmorStand> armorStands = new HashMap<>();

    public GridManager() {
    }

    public void teleportPlayerToGrid(Player player, Location location){
        if (!location.isWorldLoaded()) {
            return;
        }
        player.teleport(location);
        ArmorStand ar = (ArmorStand) location.getWorld().spawnEntity(location.clone().add(0, -0.7, 0), EntityType.ARMOR_STAND);
        ar.setCanMove(false);
        ar.setGravity(false);
        ar.setVisible(false);
        ar.setSmall(true);
        Boat boat = ApiUtilities.spawnBoat(player, location);
        ar.addPassenger(boat);
        boat.addPassenger(player);
        armorStands.put(player.getUniqueId(), ar);
    }

    public void startPlayerFromGrid(UUID uuid) {
        ArmorStand ar = armorStands.get(uuid);
        if (ar != null) {
            ar.remove();
        }
    }

    public void clearArmorstands(){
        armorStands.values().stream().forEach(armorStand -> armorStand.remove());
    }
}
