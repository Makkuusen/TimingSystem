package me.makkuusen.timing.system;

import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TrackRegion
{

    private final int id;
    private final int trackId;
    private final int regionIndex;
    private final RegionType regionType;
    private Location minP;
    private Location maxP;
    private Location spawnLocation;

    enum RegionType
    {
        START, END, CHECKPOINT, RESET, PIT, GRID
    }

    public TrackRegion(ResultSet data) throws SQLException
    {
        id = data.getInt("id");
        trackId = data.getInt("trackId");
        regionIndex = data.getInt("regionIndex");
        regionType = data.getString("regionType") == null ? null : TrackRegion.RegionType.valueOf(data.getString("regionType"));
        minP = ApiUtilities.stringToLocation(data.getString("minP"));
        maxP = ApiUtilities.stringToLocation(data.getString("maxP"));
        spawnLocation = ApiUtilities.stringToLocation(data.getString("spawn"));
    }

    public void setMinP(Location minP)
    {
        this.minP = minP;
    }

    public void setMaxP(Location maxP)
    {
        this.maxP = maxP;
    }

    public Location getMinP() {
        return minP;
    }

    public Location getMaxP() {
        return maxP;
    }

    public Location getSpawnLocation()
    {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation)
    {
        this.spawnLocation = spawnLocation;
    }

    public int getId()
    {
        return id;
    }

    public int getTrackId()
    {
        return trackId;
    }

    public int getRegionIndex()
    {
        return regionIndex;
    }

    public RegionType getRegionType()
    {
        return regionType;
    }

    public boolean contains(Location loc)
    {
        if (loc == null || minP == null || maxP == null)
        {
            return false;
        }
        else
        {
            return loc.getBlockX() >= this.minP.getBlockX() && loc.getBlockX() <= this.maxP.getBlockX() && loc.getBlockY() >= this.minP.getBlockY() && loc.getBlockY() <= this.maxP.getBlockY() && loc.getBlockZ() >= this.minP.getBlockZ() && loc.getBlockZ() <= this.maxP.getBlockZ();
        }
    }

    public String getWorldName()
    {
        return spawnLocation.getWorld().getName();
    }
}
