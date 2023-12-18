package me.makkuusen.timing.system.track.editor;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import me.makkuusen.timing.system.track.regions.TrackRegions;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;


public class RegionEditor {
    public static Component createOrUpdateRegion(Player player, Track track, TrackRegion.RegionType regionType, String index, boolean overload) {
        int regionIndex;
        boolean remove = false;
        if (index != null) {

            if (index.equalsIgnoreCase("-all")) {
                return removeAllRegions(player, track, regionType);
            }
            if (index.equalsIgnoreCase("-")) {
                return removeTrackRegion(player, track, regionType, 1);
            }

            remove = TrackEditor.getParsedRemoveFlag(index);
            try {
                regionIndex = TrackEditor.getParsedIndex(index);
            } catch (NumberFormatException e) {
                return Text.get(player, Error.NUMBER_FORMAT);
            }
            if (regionIndex == 0) {
                return Text.get(player, Error.NO_ZERO_INDEX);
            }

        } else {
            regionIndex = getIndex(track, regionType);
        }

        if (remove) {
            return removeTrackRegion(player, track, regionType, regionIndex);
        } else {
            return createOrUpdateTrackRegion(player, track, regionType, regionIndex, overload);
        }
    }

    private static Component createOrUpdateTrackRegion(Player player, Track track, TrackRegion.RegionType regionType, int index, boolean overload) {
        var maybeSelection = ApiUtilities.getSelection(player);
        if (maybeSelection.isEmpty()) {
            return Text.get(player, Error.SELECTION);
        }
        var selection = maybeSelection.get();
        TrackRegions trackRegions = track.getTrackRegions();
        if (overload) {
            trackRegions.create(regionType, index, selection, player.getLocation());
        } else if (trackRegions.hasRegion(regionType, index)) {
            trackRegions.update(trackRegions.getRegion(regionType, index).get(), selection, player.getLocation());
        } else {
            trackRegions.create(regionType, index, selection, player.getLocation());
        }
        return Text.get(player, Success.SAVED);
    }

    private static int getIndex(Track track, TrackRegion.RegionType regionType) {
        if (regionType == TrackRegion.RegionType.START || regionType == TrackRegion.RegionType.END || regionType == TrackRegion.RegionType.PIT || regionType == TrackRegion.RegionType.LAGSTART || regionType == TrackRegion.RegionType.LAGEND) {
            return 1;
        } else if (regionType == TrackRegion.RegionType.CHECKPOINT) {
            return track.getNumberOfCheckpoints() + 1;
        } else {
            return track.getTrackRegions().getRegions(regionType).size() + 1;
        }
    }

    private static Component removeTrackRegion(Player player, Track track, TrackRegion.RegionType regionType, int regionIndex) {
        if (regionType == TrackRegion.RegionType.CHECKPOINT) {
            var checkpointRegions = track.getTrackRegions().getCheckpoints(regionIndex);
            if (!checkpointRegions.isEmpty()) {
                for (TrackRegion region : checkpointRegions) {
                    if (track.getTrackRegions().remove(region)) {
                        return Text.get(player, Success.REMOVED);
                    } else {
                        return Text.get(player, Error.FAILED_TO_REMOVE);
                    }
                }
            } else {
                return Text.get(player, Error.NOTHING_TO_REMOVE);
            }
        }

        var maybeRegion = track.getTrackRegions().getRegion(regionType, regionIndex);
        if (maybeRegion.isPresent()) {
            if (track.getTrackRegions().remove(maybeRegion.get())) {
                return Text.get(player, Success.REMOVED);
            } else {
                return Text.get(player, Error.FAILED_TO_REMOVE);
            }
        } else {
            return Text.get(player, Error.NOTHING_TO_REMOVE);
        }

    }

    private static Component removeAllRegions(Player player, Track track, TrackRegion.RegionType regionType) {
        var regions = track.getTrackRegions().getRegions(regionType);
        regions.forEach(track.getTrackRegions()::remove);
        return Text.get(player, Success.REMOVED_REGIONS, "%type%", regionType.name());
    }
}
