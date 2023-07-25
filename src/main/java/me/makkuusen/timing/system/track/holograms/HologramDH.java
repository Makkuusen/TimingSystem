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

        String hologramName = String.valueOf(UUID.randomUUID());
        hologram = DHAPI.createHologram(hologramName, location, false, hologramLineStringList);
    }

    @Override
    public void removeHologram() {
        if (hologram != null) {
            hologram.delete();
        }
    }
}