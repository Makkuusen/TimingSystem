package me.makkuusen.timing.system.track.options;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TrackDatabase;

import java.util.ArrayList;
import java.util.List;

public class TrackOptions {

    private final List<TrackOption> trackOptions = new ArrayList<>();
    private final int trackId;
    public TrackOptions (int trackId) {
        this.trackId = trackId;
    }

    public void add(TrackOption trackOption) {
        trackOptions.add(trackOption);
    }

    public boolean create(TrackOption trackOption) {
        if (!trackOptions.contains(trackOption)) {
            TrackDatabase.trackOptionNew(trackId, trackOption);
            trackOptions.add(trackOption);
            return true;
        }
        return false;
    }

    public boolean remove(TrackOption trackOption) {
        if (hasOption(trackOption)) {
            TimingSystem.getTrackDatabase().deleteTrackOptionAsync(trackId, trackOption);
            trackOptions.remove(trackOption);
            return true;
        }
        return false;
    }

    public boolean hasOption(TrackOption trackOption) {
        return trackOptions.contains(trackOption);
    }

    public String listOfOptions() {
        if (trackOptions.isEmpty()) {
            return "None";
        }
        return String.join(", ", trackOptions.stream().map(TrackOption::toString).toList());
    }
}
