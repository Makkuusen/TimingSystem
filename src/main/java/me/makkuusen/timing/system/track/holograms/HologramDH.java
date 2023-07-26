package me.makkuusen.timing.system.track.holograms;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public class HologramDH implements HologramManager {
    Hologram hologram = null;

    @Override
    public void createOrUpdateHologram(Location location, List<String> hologramLineStringList) {
        removeHologram();

        // These are slightly taller than HolographicDisplays and needs to spawn on a slight offset to not be stuck in the ground.
        Location offsetLocation = location.clone();
        offsetLocation.setY(location.getY() + 0.5);
        String hologramName = String.valueOf(UUID.randomUUID());
        hologram = DHAPI.createHologram(hologramName, offsetLocation, false, hologramLineStringList);
    }

    @Override
    public void removeHologram() {
        if (hologram != null) {
            hologram.delete();
        }
    }
}