package me.makkuusen.timing.system.track;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import org.bukkit.Location;

@Getter
public class TrackLocation {
    int trackId;
    int index;
    Location location;
    TrackLocation.Type locationType;

    public TrackLocation(int trackId, int index, Location location, TrackLocation.Type locationType){
        this.trackId = trackId;
        this.index = index;
        this.location = location;
        this.locationType = locationType;
    }

    public TrackLocation(DbRow data) {
        trackId = data.getInt("trackId");
        index = data.getInt("index");
        location = ApiUtilities.stringToLocation(data.getString("location"));
        locationType = Type.valueOf(data.getString("type"));
    }

    public void updateLocation(Location location) {
        this.location = location;
        DB.executeUpdateAsync("UPDATE `ts_locations` SET `location` = '" + ApiUtilities.locationToString(location) + "' WHERE `trackId` = " + getTrackId() + " AND `index` = " + index + " AND `type` = '" + locationType + "';");
    }

    public enum Type {
        GRID, LEADERBOARD
    }
}
