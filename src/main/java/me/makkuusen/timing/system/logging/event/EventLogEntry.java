package me.makkuusen.timing.system.logging.event;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.LogDatabase;
import me.makkuusen.timing.system.logging.LogEntry;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.sql.SQLException;

public class EventLogEntry extends LogEntry {

    public EventLogEntry(DbRow data) throws ParseException {
        super(data);
    }

    public EventLogEntry(JSONObject body) {
        super(body);
    }

    @Override
    protected void register() {
        try {
            LogDatabase.registerEventLogEntry(this);
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().warning("Unable to register event log entry: " + e.getMessage());
        }
    }

}
