package me.makkuusen.timing.system.track;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.List;

@Getter
@Setter
public class TrackPolyRegion extends TrackRegion {
    Polygonal2DRegion polygonal2DRegion;

    public TrackPolyRegion(DbRow data, List<BlockVector2> points){
        super(data);
        setShape(RegionShape.POLY);
        polygonal2DRegion = new Polygonal2DRegion(BukkitAdapter.adapt(getSpawnLocation().getWorld()), points, getMinP().getBlockY(), getMaxP().getBlockY());
    }

    public boolean updateRegion(List<BlockVector2> points) {
        try {
            DB.executeUpdateAsync("DELETE FROM `ts_points` WHERE `regionId` = " + getId() + ";");
            for (BlockVector2 v : points) {
                DB.executeInsert("INSERT INTO `ts_points` (`regionId`, `x`, `z`) VALUES(" + getId() + ", " + v.getBlockX() + ", " + v.getBlockZ() + ");");
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
        polygonal2DRegion = new Polygonal2DRegion(BukkitAdapter.adapt(getSpawnLocation().getWorld()), points, getMinP().getBlockY(), getMaxP().getBlockY());
        return true;
    }

    @Override
    public boolean contains(Location loc) {
        return polygonal2DRegion.contains(BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    @Override
    public boolean isDefined() {
        return true;
    }
}
