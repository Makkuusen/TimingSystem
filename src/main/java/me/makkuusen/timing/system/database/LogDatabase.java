package me.makkuusen.timing.system.database;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.logging.LogEntry;
import me.makkuusen.timing.system.logging.track.LogTrackCreated;
import me.makkuusen.timing.system.logging.track.LogTrackMoved;
import me.makkuusen.timing.system.logging.track.LogTrackValueUpdated;
import me.makkuusen.timing.system.logging.track.TrackLogEntry;
import me.makkuusen.timing.system.logging.track.masstoggle.LogTrackMassToggle;
import me.makkuusen.timing.system.logging.track.masstoggle.LogTrackMassToggleBuilder;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.options.TrackOptions;
import me.makkuusen.timing.system.track.tags.TrackTag;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.SQLException;
import java.util.*;

public interface LogDatabase {

    Set<me.makkuusen.timing.system.logging.track.TrackLogEntry> trackLogEntries = new HashSet<>();

//    Set<EventLogEntry> eventLogEntries = new HashSet<>();


    List<DbRow> selectTrackEntries() throws SQLException;

    List<DbRow> selectEventEntries() throws SQLException;

    void insertTrackEntry(LogEntry logEntry) throws SQLException;

    void insertEventEntry(LogEntry logEntry) throws SQLException;


    static void synchronize() {
        try {
            List<DbRow> trackEntries = TimingSystem.getLogDatabase().selectTrackEntries();
            trackEntries.forEach(row -> {
                try {
                    final String action = row.getString("action");
                    final long date = row.getInt("date");
                    final TPlayer tPlayer = TSDatabase.getPlayer(UUID.fromString(row.getString("player")));
                    JSONObject body = (JSONObject) new JSONParser().parse(row.getString("body"));

                    Optional<Track> _track = TrackDatabase.getTrackById(row.getInt("id"));
                    if(_track.isEmpty())
                        return;
                    Track track = _track.get();

                    trackLogEntries.add(switch (action) {
                        case "create" -> new LogTrackCreated(tPlayer, date, track);
                        case "update_value" -> new LogTrackValueUpdated(tPlayer, date, track, body);
                        case "mass_toggle" -> new LogTrackMassToggle<>(tPlayer, date, track, body);
                        case "move" -> new LogTrackMoved(tPlayer, date, track, body);
                        default -> throw new RuntimeException("Did not recognise action '" + action + "' whilst synchronizing logs.");
                    });
                } catch (ParseException e) {
                    TimingSystem.getPlugin().getLogger().warning("Failed to parse body of track log entry " + row.getInt("id") + ": " + e.getMessage());
                }
            });

//            List<DbRow> eventEntries = TimingSystem.getLogDatabase().selectEventEntries();
//            eventEntries.forEach(row -> {
//                try {
//                    eventLogEntries.add(new EventLogEntry(row));
//                } catch (ParseException e) {
//                    TimingSystem.getPlugin().getLogger().warning("Failed to parse body of event log entry " + row.getInt("id") + ": " + e.getMessage());
//                }
//            });
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().warning("Failed to sync track events: " + e.getMessage());
        }
    }

    static void registerTrackLogEntry(TrackLogEntry logEntry) throws SQLException {
        TimingSystem.getLogDatabase().insertTrackEntry(logEntry);
        trackLogEntries.add(logEntry);
    }

//    static void registerEventLogEntry(EventLogEntry logEntry) throws SQLException {
//        TimingSystem.getLogDatabase().insertEventEntry(logEntry);
//        eventLogEntries.add(logEntry);
//    }
}
