package me.makkuusen.timing.system.logging.track;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.LogDatabase;
import me.makkuusen.timing.system.logging.LogEntry;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.sql.SQLException;

public class TrackLogEntry extends LogEntry {

    public TrackLogEntry(DbRow data) throws ParseException {
        super(data);
    }

    public TrackLogEntry(JSONObject body) {
        super(body);
    }

    @Override
    protected void register() {
        try {
            LogDatabase.registerTrackLogEntry(this);
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().warning("Unable to register track log entry: " + e.getMessage());
        }
    }
}
