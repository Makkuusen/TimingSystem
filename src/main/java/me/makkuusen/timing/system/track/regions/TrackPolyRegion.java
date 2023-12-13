package me.makkuusen.timing.system.track.regions;

import co.aikar.idb.DbRow;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.List;

@Getter
@Setter
public class TrackPolyRegion extends TrackRegion {
    Polygonal2DRegion polygonal2DRegion;

    public TrackPolyRegion(DbRow data, List<BlockVector2> points) {
        super(data);
        setShape(RegionShape.POLY);
        polygonal2DRegion = new Polygonal2DRegion(BukkitAdapter.adapt(getSpawnLocation().getWorld()), points, getMinP().getBlockY(), getMaxP().getBlockY());
    }

    public TrackPolyRegion(long id, long trackId, int regionIndex, RegionType regionType, Location spawnLocation, Location minP, Location maxP, List<BlockVector2> points) {
        super (id,trackId,regionIndex,regionType,spawnLocation,minP,maxP);
        setShape(RegionShape.POLY);
        polygonal2DRegion = new Polygonal2DRegion(BukkitAdapter.adapt(getSpawnLocation().getWorld()), points, getMinP().getBlockY(), getMaxP().getBlockY());
    }

    public void updateRegion(List<BlockVector2> points) {
        try {
            TimingSystem.getTrackDatabase().deletePoint(getId());
            for (BlockVector2 v : points) {
                TimingSystem.getTrackDatabase().createPoint(getId(), v);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return;
        }
        polygonal2DRegion = new Polygonal2DRegion(BukkitAdapter.adapt(getSpawnLocation().getWorld()), points, getMinP().getBlockY(), getMaxP().getBlockY());
    }

    @Override
    public boolean contains(Location loc) {
        return polygonal2DRegion.contains(BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    @Override
    public boolean isDefined() {
        return true;
    }

    public boolean hasEqualBounds(TrackRegion other) {
        if (other instanceof TrackPolyRegion trackPolyRegion) {
            if (!other.getWorldName().equalsIgnoreCase(getWorldName())) {
                return false;
            }
            if (!isDefined() || !other.isDefined()) {
                return false;
            }
            return getMinP().equals(trackPolyRegion.getMinP()) && getMaxP().equals(trackPolyRegion.getMaxP());
        }
        return false;
    }
}
