package me.makkuusen.timing.system.logging.track;

import lombok.Getter;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import org.json.simple.JSONObject;

import java.util.UUID;

@Getter
public class LogTrackCreated extends TrackLogEntry {

    public LogTrackCreated(TPlayer tPlayer, long date, Track track) {
        super(tPlayer, date, track, "create");
    }

    @Override
    public String generateBody() {
        return "{}";
    }
}
