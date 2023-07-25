package me.makkuusen.timing.system.track.holograms;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Location;

import java.util.List;

public class HologramDH implements HologramManager {
    Hologram hologram = null;

    @Override
    public void createOrUpdateHologram(Location location, List<String> hologramLineStringList) {
        // Try see if a hologram exists first
        hologram = DHAPI.getHologram("TimingSystem");

        if (hologram == null) {
            hologram = DHAPI.createHologram("TimingSystem", location);
        } else if (hologram.getLocation().getWorld() != location.getWorld()) {
            DHAPI.moveHologram(hologram, location);
        } else if (hologram.getLocation().distance(location) > 1) {
            DHAPI.moveHologram(hologram, location);
        }

        DHAPI.setHologramLines(hologram, hologramLineStringList);
    }

    @Override
    public void removeHologram() {
        if (hologram != null) {
            hologram.delete();
        }
    }
}