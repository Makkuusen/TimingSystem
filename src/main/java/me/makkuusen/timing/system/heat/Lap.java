package me.makkuusen.timing.system.heat;

import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.Track;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

@Getter
@Setter
public class Lap implements Comparable<Lap> {

    private TPlayer player;
    private int heatId;
    private Track track;
    private Instant lapEnd;
    private Instant lapStart;
    private boolean pitted;
    private ArrayList<Instant> checkpoints = new ArrayList<>();

    public Lap(Driver driver, Track track) {
        this.heatId = driver.getHeat().getId();
        this.player = driver.getTPlayer();
        this.track = track;
        this.lapStart = TimingSystem.currentTime;
        this.pitted = false;
    }

    public Lap(DbRow data) {
        player = TSDatabase.getPlayer(data.getString("uuid"));
        heatId = data.getInt("heatId");
        track = TrackDatabase.getTrackById(data.getInt("trackId")).orElse(null);
        lapStart = Instant.ofEpochMilli(data.getLong("lapStart"));
        lapEnd = data.getLong("lapEnd") == null ? null : Instant.ofEpochMilli(data.getLong("lapEnd"));
        pitted = data.get("pitted");
    }

    public long getLapTime() {
        if (lapEnd == null || lapStart == null) {
            return -1;
        }
        long lapTime = Duration.between(lapStart, lapEnd).toMillis();
        return ApiUtilities.getRoundedToTick(lapTime);
    }

    public int getNextCheckpoint() {
        if (track.getNumberOfCheckpoints() >= checkpoints.size()) {
            return checkpoints.size() + 1;
        }
        return checkpoints.size();
    }

    public boolean hasPassedAllCheckpoints() {
        return checkpoints.size() == track.getNumberOfCheckpoints();
    }

    public void passNextCheckpoint(Instant timeStamp) {
        checkpoints.add(timeStamp);
    }

    public int getLatestCheckpoint() {
        return checkpoints.size();
    }

    public Instant getCheckpointTime(int checkpoint) {
        if (checkpoints.isEmpty() || checkpoint == 0) {
            return lapStart;
        }
        return checkpoints.get(checkpoint - 1);
    }

    @Override
    public int compareTo(@NotNull Lap lap) {
        return Long.compare(getLapTime(), lap.getLapTime());
    }
}
