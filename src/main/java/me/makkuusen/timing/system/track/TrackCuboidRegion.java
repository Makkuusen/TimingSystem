package me.makkuusen.timing.system.track;

import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class TrackCuboidRegion extends TrackRegion {

    public TrackCuboidRegion(DbRow data) {
        super(data);
        setShape(RegionShape.CUBOID);
    }

    public boolean contains(Location loc) {
        if (loc == null || getMinP() == null || getMinP() == null) {
            return false;
        } else {
            return loc.getBlockX() >= getMinP().getBlockX() && loc.getBlockX() <= getMaxP().getBlockX() && loc.getBlockY() >= getMinP().getBlockY() && loc.getBlockY() <= getMaxP().getBlockY() && loc.getBlockZ() >= getMinP().getBlockZ() && loc.getBlockZ() <= getMaxP().getBlockZ();
        }
    }

    public boolean isDefined() {
        return getMinP() != null && getMaxP() != null;
    }

    public boolean hasEqualBounds(TrackRegion other) {
        if (other instanceof TrackCuboidRegion trackCuboidRegion) {
            if (!other.getWorldName().equalsIgnoreCase(getWorldName())) {
                return false;
            }
            if (!isDefined() || !other.isDefined()) {
                return false;
            }
            return getMinP().equals(trackCuboidRegion.getMinP()) && getMaxP().equals(trackCuboidRegion.getMaxP());
        }
        return false;
    }
}
