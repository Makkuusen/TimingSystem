package me.makkuusen.timing.system.track.holograms;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.HologramLines;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Location;

import java.util.List;

public class HologramHD implements HologramManager {
    Hologram hologram = null;

    @Override
    public void createOrUpdateHologram(Location location, List<String> hologramLineStringList) {
        if (hologram == null) {
            hologram = HolographicDisplaysAPI.get(TimingSystem.getPlugin()).createHologram(location);
        } else if (!hologram.getPosition().isInSameWorld(location)) {
            hologram.delete();
            hologram = HolographicDisplaysAPI.get(TimingSystem.getPlugin()).createHologram(location);
        } else if (hologram.getPosition().distance(location) > 1) {
            hologram.delete();
            hologram = HolographicDisplaysAPI.get(TimingSystem.getPlugin()).createHologram(location);
        }

        HologramLines hologramLines = hologram.getLines();
        hologramLines.clear();

        for (String line : hologramLineStringList) {
            hologramLines.appendText(line);
        }
    }

    @Override
    public void removeHologram() {
        if (hologram != null) {
            hologram.delete();
        }
    }
}