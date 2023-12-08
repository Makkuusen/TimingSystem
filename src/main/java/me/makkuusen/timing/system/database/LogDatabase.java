package me.makkuusen.timing.system.database;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.logger.LogEntry;
import org.json.simple.parser.ParseException;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface LogDatabase {

    Set<LogEntry> logEntries = new HashSet<>();


    List<DbRow> selectTrackEntries() throws SQLException;

    void insertEvent(LogEntry logEntry) throws SQLException;


    static void synchronize() {
        try {
            List<DbRow> trackEntries = TimingSystem.getLogDatabase().selectTrackEntries();
            trackEntries.forEach(row -> {
                try {
                    logEntries.add(new LogEntry(row));
                } catch (ParseException e) {
                    TimingSystem.getPlugin().getLogger().warning("Failed to parse body of track event " + row.getInt("id") + ": " + e.getMessage());
                }
            });
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().warning("Failed to sync track events: " + e.getMessage());
        }
    }
}
