package me.makkuusen.timing.system.track.holograms;

import org.bukkit.Location;

import java.util.List;

public interface HologramManager {

    void createOrUpdateHologram(Location location, List<String> hologramLines);

    void removeHologram();
}
