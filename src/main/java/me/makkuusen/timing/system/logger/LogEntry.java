package me.makkuusen.timing.system.logger;

import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.TimingSystem;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.SQLException;

@Getter
public class LogEntry {

    private final long date;
    private final JSONObject body;

    public LogEntry(DbRow data) throws ParseException {
        date = data.getLong("date");
        body = (JSONObject) new JSONParser().parse(data.getString("body"));
    }

    public LogEntry(long date, JSONObject body) {
        this.date = date;
        this.body = body;

        try {
            TimingSystem.getLogDatabase().insertEvent(this);
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().warning("Failed to insert a LogEntry into the Database.");
        }
    }

    public LogType getLogType() {
        return LogType.of(getInt("type"));
    }

    public String getString(String key) {
        return String.valueOf(body.get(key));
    }

    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }
}
