package me.makkuusen.timing.system.logging.track;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.LogDatabase;
import me.makkuusen.timing.system.logging.LogEntry;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;

import java.sql.SQLException;

public abstract class TrackLogEntry extends LogEntry {

    protected final Track track;

    public TrackLogEntry(TPlayer tPlayer, long date, Track track, String action) {
        super(tPlayer, date, action);
        this.track = track;
    }

    @Override
    public void save() {
        try {
            LogDatabase.registerTrackLogEntry(this);
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().warning("Failed to register a track log: " + e.getMessage());
        }
    }
}
