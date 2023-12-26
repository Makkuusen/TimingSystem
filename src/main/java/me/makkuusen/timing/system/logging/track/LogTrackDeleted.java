package me.makkuusen.timing.system.logging.track;

import lombok.Getter;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;

@Getter
public class LogTrackDeleted extends TrackLogEntry {

    // TODO: Track will always be null, because it was deleted.
    //       We somehow need to a way to store a track here.
    public LogTrackDeleted(TPlayer tPlayer, long date, Track track) {
        super(tPlayer, date, track, "delete");
    }

    @Override
    public String generateBody() {
        return "{}"; // TODO: Make this
    }
}
