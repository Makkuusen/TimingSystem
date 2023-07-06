package me.makkuusen.timing.system.timetrial;

import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TPlayer;

import java.util.UUID;

@Getter
public class TimeTrialAttempt {

    private final int trackId;
    private final UUID uuid;
    private final long date;
    private final long time;

    public TimeTrialAttempt(int trackId, UUID uuid, long date, long time) {
        this.trackId = trackId;
        this.uuid = uuid;
        this.date = date;
        this.time = time;
    }

    public TimeTrialAttempt(DbRow data) {
        this.trackId = data.getInt("trackId");
        this.uuid = data.getString("uuid") == null ? null : UUID.fromString(data.getString("uuid"));
        this.date = data.getInt("date");
        this.time = data.getInt("time");
    }

    public TPlayer getPlayer() {
        return Database.getPlayer(uuid);
    }
}
