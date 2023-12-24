package me.makkuusen.timing.system.logging;

import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@Getter
public abstract class LogEntry {

    private final long date;
    private final JSONObject body;

    public LogEntry(DbRow data) throws ParseException {
        this.date = data.getInt("date");

        String json = data.getString("body");
        body = (JSONObject) new JSONParser().parse(json);
    }

    public LogEntry(JSONObject body) {
        this.date = ApiUtilities.getTimestamp();
        this.body = body;

        register();
    }

    protected abstract void register();
}
