package me.makkuusen.timing.system.database;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.logging.LogEntry;
import me.makkuusen.timing.system.logging.event.EventLogEntry;
import me.makkuusen.timing.system.logging.track.TrackLogEntry;
import org.json.simple.parser.ParseException;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface LogDatabase {

    Set<TrackLogEntry> trackLogEntries = new HashSet<>();

    Set<EventLogEntry> eventLogEntries = new HashSet<>();


    List<DbRow> selectTrackEntries() throws SQLException;

    List<DbRow> selectEventEntries() throws SQLException;

    void insertTrackEntry(LogEntry logEntry) throws SQLException;

    void insertEventEntry(LogEntry logEntry) throws SQLException;


    static void synchronize() {
        try {
            List<DbRow> trackEntries = TimingSystem.getLogDatabase().selectTrackEntries();
            trackEntries.forEach(row -> {
                try {
                    trackLogEntries.add(new TrackLogEntry(row));
                } catch (ParseException e) {
                    TimingSystem.getPlugin().getLogger().warning("Failed to parse body of track log entry " + row.getInt("id") + ": " + e.getMessage());
                }
            });

            List<DbRow> eventEntries = TimingSystem.getLogDatabase().selectEventEntries();
            eventEntries.forEach(row -> {
                try {
                    eventLogEntries.add(new EventLogEntry(row));
                } catch (ParseException e) {
                    TimingSystem.getPlugin().getLogger().warning("Failed to parse body of event log entry " + row.getInt("id") + ": " + e.getMessage());
                }
            });
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().warning("Failed to sync track events: " + e.getMessage());
        }
    }

    static void registerTrackLogEntry(TrackLogEntry logEntry) throws SQLException {
        TimingSystem.getLogDatabase().insertTrackEntry(logEntry);
        trackLogEntries.add(logEntry);
    }

    static void registerEventLogEntry(EventLogEntry logEntry) throws SQLException {
        TimingSystem.getLogDatabase().insertEventEntry(logEntry);
        eventLogEntries.add(logEntry);
    }
}
