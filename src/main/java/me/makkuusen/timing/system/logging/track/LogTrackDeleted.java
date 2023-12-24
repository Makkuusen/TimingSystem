package me.makkuusen.timing.system.logging.track;

import me.makkuusen.timing.system.track.Track;
import org.json.simple.JSONObject;

import java.util.UUID;

public class LogTrackDeleted {

    public static void create(UUID playerUUID, Track track) {
        JSONObject body = new JSONObject();
        body.put("player", playerUUID.toString());
        body.put("action", "delete_track");
        body.put("track", track.getId());

        new TrackLogEntry(body);
    }
}
