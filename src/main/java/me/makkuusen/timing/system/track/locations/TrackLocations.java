package me.makkuusen.timing.system.track.locations;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TrackDatabase;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class TrackLocations {

    private final Set<TrackLocation> trackLocations = new HashSet<>();
    private final int trackId;
    public TrackLocations(int trackId) {
        this.trackId = trackId;
    }

    public Optional<Location> getFinishTp() {
        if(trackLocations.stream().noneMatch(l -> l.getLocationType() == TrackLocation.Type.FINISH_TP_ALL)) return Optional.empty();
        return Optional.of(trackLocations.stream().filter(l -> l.getLocationType() == TrackLocation.Type.FINISH_TP_ALL).toList().get(0).getLocation());
    }

    public Optional<Location> getFinishTp(int pos) {
        for(TrackLocation finishTp : trackLocations.stream().filter(l -> l.getLocationType() == TrackLocation.Type.FINISH_TP).toList()) {
            if(finishTp.getIndex() == pos)
                return Optional.of(finishTp.getLocation());
        }
        return Optional.empty();
    }

    public void add(TrackLocation trackLocation) {
        trackLocations.add(trackLocation);
    }

    public boolean hasLocation(TrackLocation.Type locationType) {
        return trackLocations.stream().anyMatch(trackLocation -> trackLocation.getLocationType().equals(locationType));
    }

    public boolean hasLocation(TrackLocation.Type locationType, int index) {
        return trackLocations.stream().filter(trackLocation -> trackLocation.getLocationType().equals(locationType)).anyMatch(trackLocation -> trackLocation.getIndex() == index);
    }


    public List<TrackLocation> getLocations() {
        return trackLocations.stream().toList();
    }
    public List<TrackLocation> getLocations(TrackLocation.Type locationType) {
        var list = trackLocations.stream().filter(trackLocation -> trackLocation.getLocationType().equals(locationType)).collect(Collectors.toList());
        list.sort(Comparator.comparingInt(TrackLocation::getIndex));
        return list;
    }

    public Optional<TrackLocation> getLocation(TrackLocation.Type locationType) {
        return trackLocations.stream().filter(trackLocation -> trackLocation.getLocationType().equals(locationType)).findFirst();
    }

    public Optional<TrackLocation> getLocation(TrackLocation.Type locationType, int index) {
        return trackLocations.stream().filter(trackLocation -> trackLocation.getLocationType().equals(locationType)).filter(trackLocation -> trackLocation.getIndex() == index).findFirst();
    }

    public void update(TrackLocation trackLocation, Location location) {
        trackLocation.updateLocation(location);
        if (trackLocation instanceof TrackLeaderboard trackLeaderboard) {
            trackLeaderboard.createOrUpdateHologram();
        }
    }

    public boolean create(TrackLocation.Type type, Location location) {
        return create(type, 0, location);
    }

    public boolean create(TrackLocation.Type type, int index, Location location) {
        try {
            var trackLocation = TrackDatabase.trackLocationNew(trackId, index, type, location);
            add(trackLocation);
            if (trackLocation instanceof TrackLeaderboard trackLeaderboard) {
                trackLeaderboard.createOrUpdateHologram();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean remove(TrackLocation trackLocation) {
        if (trackLocations.contains(trackLocation)) {
            if (trackLocation instanceof TrackLeaderboard trackLeaderboard) {
                trackLeaderboard.removeHologram();
            }
            trackLocations.remove(trackLocation);
            TimingSystem.getTrackDatabase().deleteLocation(trackId, trackLocation.getIndex(), trackLocation.getLocationType());
            return true;
        }
        return false;
    }
}
