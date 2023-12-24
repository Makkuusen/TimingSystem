package me.makkuusen.timing.system.logging.track;

import me.makkuusen.timing.system.track.Track;
import org.json.simple.JSONObject;

import java.util.UUID;

public class LogTrackValueUpdated {

    public static <T> void create(UUID playerUUID, Track track, String settingChanged, T oldValue, T newValue) {
        JSONObject body = new JSONObject();
        body.put("player", playerUUID.toString());
        body.put("track", track.getDisplayName());
        body.put("action", "update_value");
        body.put("setting_changed", settingChanged);
        body.put("old_value", oldValue);
        body.put("new_value", newValue);

        new TrackLogEntry(body);
    }
}
