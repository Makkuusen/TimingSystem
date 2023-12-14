package me.makkuusen.timing.system.track.regions;

import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class TrackRegions {

    @Getter
    private final Set<TrackRegion> regions = new HashSet<>();
    private final Track track;
    public TrackRegions(Track track) {
        this.track = track;
    }

    public void add(TrackRegion trackRegion) {
        regions.add(trackRegion);
    }

    public boolean hasRegion(TrackRegion.RegionType regionType) {
        return regions.stream().anyMatch(trackRegion -> trackRegion.getRegionType().equals(regionType));
    }

    public boolean hasRegion(TrackRegion.RegionType regionType, int index) {
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).anyMatch(trackRegion -> trackRegion.getRegionIndex() == index);
    }

    public List<TrackRegion> getRegions(TrackRegion.RegionType regionType) {
        var list = regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).collect(Collectors.toList());
        list.sort(Comparator.comparingInt(trackRegion -> trackRegion.getRegionIndex()));
        return list;
    }

    public Optional<TrackRegion> getRegion(TrackRegion.RegionType regionType) {
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).findFirst();
    }

    public Optional<TrackRegion> getRegion(TrackRegion.RegionType regionType, int index) {
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).filter(trackRegion -> trackRegion.getRegionIndex() == index).findFirst();
    }

    public List<TrackRegion> getCheckpoints(int index) {
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(TrackRegion.RegionType.CHECKPOINT)).filter(trackRegion -> trackRegion.getRegionIndex() == index).toList();
    }

    public Optional<TrackRegion> getStart() {
        return hasRegion(TrackRegion.RegionType.START, 1) ? getRegion(TrackRegion.RegionType.START, 1) : getRegion(TrackRegion.RegionType.START);

    }

    public boolean update(TrackRegion.RegionType regionType, Region selection, Location location) {
        var region = getRegion(regionType).get();
        return update(region, selection, location);
    }

    public boolean update(TrackRegion region, Region selection, Location location) {
        if (ApiUtilities.isRegionMatching(region, selection)) {
            region.setMaxP(ApiUtilities.getLocationFromBlockVector3(location.getWorld(), selection.getMaximumPoint()));
            region.setMinP(ApiUtilities.getLocationFromBlockVector3(location.getWorld(), selection.getMinimumPoint()));
            region.setSpawn(location);
            if (region instanceof TrackPolyRegion trackPolyRegion) {
                trackPolyRegion.updateRegion(((Polygonal2DRegion) selection).getPoints());
            }
            if (isTrackBoundaryChange(region.getRegionType())) {
                track.setDateChanged();
            }
        } else {
            remove(region);
            return create(region.getRegionType(), region.getRegionIndex(), selection, location);
        }
        return true;
    }

    public boolean create(TrackRegion.RegionType regionType, Region selection, Location location) {
        return create(regionType, 0, selection, location);
    }

    public boolean create(TrackRegion.RegionType regionType, int index, Region selection, Location location) {
        try {
            var region = TrackDatabase.trackRegionNew(selection, track.getId(), index, regionType, location);
            add(region);
            if (regionType.equals(TrackRegion.RegionType.START)) {
                TrackDatabase.addTrackRegion(region);
            }
            if (isTrackBoundaryChange(regionType)) {
                track.setDateChanged();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean remove(TrackRegion region) {
        if (regions.contains(region)) {
            var regionId = region.getId();
            TrackDatabase.removeTrackRegion(region);
            regions.remove(region);
            TimingSystem.getTrackDatabase().trackRegionSet(regionId, "isRemoved", 1);
            if (region instanceof TrackPolyRegion) {
                TimingSystem.getTrackDatabase().deletePoint(regionId);
            }
            if (isTrackBoundaryChange(region.getRegionType())) {
                track.setDateChanged();
            }
            return true;
        }
        return false;
    }

    private boolean isTrackBoundaryChange(TrackRegion.RegionType regionType) {
        if (regionType.equals(TrackRegion.RegionType.START)) {
            return true;
        } else if (regionType.equals(TrackRegion.RegionType.END)) {
            return true;
        } else return regionType.equals(TrackRegion.RegionType.CHECKPOINT);
    }


}
