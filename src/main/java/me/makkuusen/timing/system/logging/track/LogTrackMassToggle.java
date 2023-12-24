package me.makkuusen.timing.system.logging.track;

import me.makkuusen.timing.system.track.Track;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class LogTrackMassToggle<T> {

    private final Set<T> added = new HashSet<>();
    private final Set<T> removed = new HashSet<>();

    private final UUID playerUUID;
    private final Track track;
    private final String action;

    public LogTrackMassToggle(UUID playerUUID, Track track, String action) {
        this.playerUUID = playerUUID;
        this.track = track;
        this.action = action;
    }

    public void added(T thing) {
        added.add(thing);
    }

    public void removed(T thing) {
        removed.add(thing);
    }

    public void create(Function<? super T, ?> mapper) {
        JSONObject body = new JSONObject();
        body.put("player", playerUUID.toString());
        body.put("track", track.getId());
        body.put("action", action);

        // What on earth
        JSONArray added = new JSONArray();
        this.added.stream().map(mapper).forEach(added::add);
        body.put("added", added);

        JSONArray removed = new JSONArray();
        this.removed.stream().map(mapper).forEach(removed::add);
        body.put("removed", removed);

        new TrackLogEntry(body);
    }
}
