package me.makkuusen.timing.system.logging.track;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Location;
import org.json.simple.JSONObject;

import java.util.UUID;

public class LogTrackMoved extends TrackLogEntry {

    private final Location from;
    private final Location to;

    public LogTrackMoved(TPlayer tPlayer, long date, Track track, JSONObject body) {
        super(tPlayer, date, track, "move");
        this.from = ApiUtilities.stringToLocation(String.valueOf(body.get("from")));
        this.to = ApiUtilities.stringToLocation(String.valueOf(body.get("to")));
    }

    @Override
    public String generateBody() {
        JSONObject body = new JSONObject();
        body.put("from", ApiUtilities.locationToString(from));
        body.put("to", ApiUtilities.locationToString(to));
        return body.toJSONString();
    }

    public static LogTrackMoved create(TPlayer tPlayer, Track track, Location from, Location to) {
        long date = ApiUtilities.getTimestamp();
        JSONObject body = new JSONObject();
        body.put("from", ApiUtilities.locationToString(from));
        body.put("to", ApiUtilities.locationToString(to));

        return new LogTrackMoved(tPlayer, date, track, body);
    }
}
