package me.makkuusen.timing.system.logging.track.masstoggle;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.logging.track.TrackLogEntry;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.function.Function;

@Getter
public class LogTrackMassToggle<T> extends TrackLogEntry {

    private final String type;

    private final List<T> added;
    private final List<T> removed;

    private Function<? super T, ?> mapper = t -> t;

    @SuppressWarnings("unchecked,badcode")
    public LogTrackMassToggle(TPlayer tPlayer, long date, Track track, JSONObject body) {
        super(tPlayer, date, track, "mass_toggle");
        this.type = String.valueOf(body.get("type"));

        this.added = new ArrayList<>();
        JSONArray added = (JSONArray) body.get("added");
        added.forEach(o -> this.added.add((T) o));

        this.removed = new ArrayList<>();
        JSONArray removed = (JSONArray) body.get("removed");
        removed.forEach(o -> this.removed.add((T) o));
    }

    public LogTrackMassToggle(LogTrackMassToggleBuilder<T> builder) {
        super(builder.tPlayer, ApiUtilities.getTimestamp(), builder.track, "mass_toggle");
        this.type = builder.type;
        this.mapper = builder.mapper;
        this.added = builder.added;
        this.removed = builder.removed;
    }

    @Override
    public String generateBody() {
        JSONObject body = new JSONObject();
        body.put("type", type);

        JSONArray added = new JSONArray();
        this.added.stream().map(mapper).forEach(added::add);
        body.put("added", added);

        JSONArray removed = new JSONArray();
        this.removed.stream().map(mapper).forEach(removed::add);
        body.put("removed", removed);

        return body.toJSONString();
    }
}
