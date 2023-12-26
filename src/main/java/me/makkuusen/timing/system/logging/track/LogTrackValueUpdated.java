package me.makkuusen.timing.system.logging.track;

import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import org.json.simple.JSONObject;

@Getter
public final class LogTrackValueUpdated extends TrackLogEntry {

    private final String settingChanged;
    private final Object oldValue;
    private final Object newValue;

    public LogTrackValueUpdated(TPlayer tPlayer, long date, Track track, JSONObject jsonObject) {
        super(tPlayer, date, track, "update_value");
        this.settingChanged = String.valueOf(jsonObject.get("setting_changed"));
        this.oldValue = jsonObject.get("old_value");
        this.newValue = jsonObject.get("new_value");
    }

    @Override
    public String generateBody() {
        JSONObject body = new JSONObject();
        body.put("setting_changed", settingChanged);
        body.put("old_value", oldValue);
        body.put("new_value", newValue);
        return body.toJSONString();
    }

    public static <T> LogTrackValueUpdated create(TPlayer tPlayer, Track track, String settingChanged, T oldValue, T newValue) {
        long date = ApiUtilities.getTimestamp();
        JSONObject body = new JSONObject();
        body.put("setting_changed", settingChanged);
        body.put("old_value", oldValue);
        body.put("new_value", newValue);

        return new LogTrackValueUpdated(tPlayer, date, track, body);
    }
}
