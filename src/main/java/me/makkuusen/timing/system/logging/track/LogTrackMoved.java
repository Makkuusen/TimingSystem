package me.makkuusen.timing.system.logging.track;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Location;
import org.json.simple.JSONObject;

import java.util.UUID;

public class LogTrackMoved {

    public static void create(UUID playerUUID, Track track, Location from, Location to) {
        JSONObject body = new JSONObject();
        body.put("player", playerUUID.toString());
        body.put("action", "move_track");
        body.put("track", track.getDisplayName());
        body.put("from", ApiUtilities.locationToString(from));
        body.put("to", ApiUtilities.locationToString(to));

        new TrackLogEntry(body);
    }
}
