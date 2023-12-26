package me.makkuusen.timing.system.logging;

import lombok.Getter;
import me.makkuusen.timing.system.tplayer.TPlayer;

@Getter
public abstract class LogEntry {

    protected final TPlayer tPlayer;
    protected final long date;
    protected final String action;

    public LogEntry(TPlayer tPlayer, long date, String action) {
        this.tPlayer = tPlayer;
        this.date = date;
        this.action = action;
    }

    public abstract String generateBody();

    public abstract void save();
}
