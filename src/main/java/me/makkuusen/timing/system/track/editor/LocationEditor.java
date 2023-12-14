package me.makkuusen.timing.system.track.editor;

import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LocationEditor {

    public static Map<UUID, List<TrackLocation>> restoreLocations = new HashMap<>();
    public static Component createOrUpdateLocation(Player player, Track track, TrackLocation.Type trackLocationType, String index) {
        int locationIndex;
        boolean remove = false;
        if (index != null) {

            if (index.equalsIgnoreCase("-all")) {
                return removeAllLocations(player, track, trackLocationType);
            } else if (index.equalsIgnoreCase("+all")) {
                return restoreAllLocations(player, track, trackLocationType);
            }
            remove = TrackEditor.getParsedRemoveFlag(index);
            try {
                locationIndex = TrackEditor.getParsedIndex(index);
            } catch (NumberFormatException e) {
                return Text.get(player, Error.NUMBER_FORMAT);
            }
            if (locationIndex == 0) {
                return Text.get(player, Error.NO_ZERO_INDEX);
            }

        } else {
            locationIndex = getIndex(track, trackLocationType);
        }

        if (remove) {
            return removeTrackLocation(player, track, trackLocationType, locationIndex);
        } else {
            var location = getLocation(player, trackLocationType);
            createOrUpdateTrackLocation(track, trackLocationType, locationIndex, location);
            return Text.get(player, Success.SAVED);
        }
    }

    private static Location getLocation(Player player, TrackLocation.Type trackLocationType) {
        Location location = player.getLocation();
        if (trackLocationType.equals(TrackLocation.Type.LEADERBOARD)) {
            location.setY(location.getY() + 3);
        }
        return location;
    }

    private static void createOrUpdateTrackLocation(Track track, TrackLocation.Type type, int index, Location location) {
        if (track.getTrackLocations().hasLocation(type, index)) {
            track.getTrackLocations().update(track.getTrackLocations().getLocation(type, index).get(), location);
        } else {
            track.getTrackLocations().create(type, index, location);
        }
    }

    private static Component removeTrackLocation(Player player, Track track, TrackLocation.Type type, int locationIndex) {
        var maybeLocation = track.getTrackLocations().getLocation(type, locationIndex);
        if (maybeLocation.isPresent()) {
            if (track.getTrackLocations().remove(maybeLocation.get())) {
                return Text.get(player, Success.REMOVED);
            } else {
                return Text.get(player, Error.FAILED_TO_REMOVE);
            }
        } else {
            return Text.get(player, Error.NOTHING_TO_REMOVE);
        }
    }

    private static Component restoreAllLocations(Player player, Track track, TrackLocation.Type trackLocationType) {
        var locations = restoreLocations.get(player.getUniqueId());
        if (locations.isEmpty()) {
            return Text.get(player, Error.NOTHING_TO_RESTORE);
        }
        if (locations.get(0).getTrackId() != track.getId() || locations.get(0).getLocationType() != trackLocationType) {
            return Text.get(player, Error.NOTHING_TO_RESTORE);
        }

        for (TrackLocation restoreLocation : restoreLocations.get(player.getUniqueId())) {
            var maybeLocation = track.getTrackLocations().getLocation(restoreLocation.getLocationType(), restoreLocation.getIndex());
            if (maybeLocation.isPresent()) {
                continue;
            }
            track.getTrackLocations().create(restoreLocation.getLocationType(), restoreLocation.getIndex(), restoreLocation.getLocation());
        }
        restoreLocations.remove(player.getUniqueId());
        return Text.get(player, Success.RESTORED_LOCATIONS);

    }

    private static Component removeAllLocations(Player player, Track track, TrackLocation.Type trackLocationType) {
        var trackLocations = track.getTrackLocations().getLocations(trackLocationType);
        restoreLocations.put(player.getUniqueId(), trackLocations);
        trackLocations.forEach(track.getTrackLocations()::remove);
        return Text.get(player, Success.REMOVED_LOCATIONS, "%type%", trackLocationType.name());
    }

    private static int getIndex(Track track, TrackLocation.Type type) {

        if (type == TrackLocation.Type.GRID || type == TrackLocation.Type.QUALYGRID) {
            return track.getTrackLocations().getLocations(type).size() + 1;
        } else {
            return 1;
        }
    }
}
