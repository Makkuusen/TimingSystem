package me.makkuusen.timing.system.track;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import org.bukkit.Location;

@Getter
@Setter
public abstract class TrackRegion {

    private final int id;
    private final int trackId;
    private final int regionIndex;
    private final RegionType regionType;
    private RegionShape shape;
    private Location spawnLocation;
    private Location minP;
    private Location maxP;

    public TrackRegion(DbRow data) {
        id = data.getInt("id");
        trackId = data.getInt("trackId");
        regionIndex = data.getInt("regionIndex");
        regionType = data.getString("regionType") == null ? null : TrackRegion.RegionType.valueOf(data.getString("regionType"));
        spawnLocation = ApiUtilities.stringToLocation(data.getString("spawn"));
        minP = ApiUtilities.stringToLocation(data.getString("minP"));
        maxP = ApiUtilities.stringToLocation(data.getString("maxP"));
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public int getId() {
        return id;
    }

    public int getTrackId() {
        return trackId;
    }

    public int getRegionIndex() {
        return regionIndex;
    }

    public RegionType getRegionType() {
        return regionType;
    }

    public abstract boolean contains(Location loc);

    public abstract boolean isDefined();

    public String getWorldName() {
        if (!spawnLocation.isWorldLoaded()) {
            return "Unknown";
        }
        return spawnLocation.getWorld().getName();
    }

    public void setMinP(Location minP) {
        this.minP = minP;
        DB.executeUpdateAsync("UPDATE `ts_regions` SET `minP` = '" + ApiUtilities.locationToString(minP) + "' WHERE `id` = " + getId() + ";");
    }

    public void setMaxP(Location maxP) {
        this.maxP = maxP;
        DB.executeUpdateAsync("UPDATE `ts_regions` SET `maxP` = '" + ApiUtilities.locationToString(maxP) + "' WHERE `id` = " + getId() + ";");
    }

    public void setSpawn(Location spawn) {
        this.spawnLocation = spawn;
        DB.executeUpdateAsync("UPDATE `ts_regions` SET `spawn` = '" + ApiUtilities.locationToString(spawn) + "' WHERE `id` = " + getId() + ";");
    }

    abstract boolean hasEqualBounds(TrackRegion other);

    public enum RegionType {
        START, END, PIT, CHECKPOINT, RESET, INPIT, LAGSTART, LAGEND
    }

    public enum RegionShape {
        POLY, CUBOID
    }

}
