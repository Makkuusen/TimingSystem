package me.makkuusen.timing.system.logging.track.masstoggle;

import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LogTrackMassToggleBuilder<T> {

    protected final TPlayer tPlayer;
    protected final Track track;
    protected final String type;

    protected final List<T> added = new ArrayList<>();
    protected final List<T> removed = new ArrayList<>();

    protected Function<? super T, ?> mapper = t -> t;

    public LogTrackMassToggleBuilder(TPlayer tPlayer, Track track, String type) {
        this.tPlayer = tPlayer;
        this.track = track;
        this.type = type;
    }

    public LogTrackMassToggleBuilder<T> added(T thing) {
        added.add(thing);
        return this;
    }

    public LogTrackMassToggleBuilder<T> removed(T thing) {
        removed.add(thing);
        return this;
    }

    public LogTrackMassToggleBuilder<T> setMapper(Function<? super T, ?> mapper) {
        this.mapper = mapper;
        return this;
    }

    public LogTrackMassToggle<T> build() {
        return new LogTrackMassToggle<>(this);
    }
}
