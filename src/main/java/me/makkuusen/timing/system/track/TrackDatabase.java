package me.makkuusen.timing.system.track;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.LeaderboardManager;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class TrackDatabase {

    public static TimingSystem plugin;
    private static List<Track> tracks = new ArrayList<>();
    private static List<TrackRegion> regions = new ArrayList<>();

    public static void initDatabaseSynchronize() throws SQLException {
        var dbRows = DB.getResults("SELECT * FROM `ts_tracks` WHERE `isRemoved` = 0;");

        for (DbRow dbRow : dbRows) {
            Track rTrack = new Track(dbRow);
            tracks.add(rTrack);
            plugin.getLogger().info("LOADING IN " + rTrack.getDisplayName());

            var resultFinishes = DB.getResults("SELECT * FROM `ts_finishes` WHERE ( `uuid`,`time`) IN (SELECT `uuid`, min(`time`) FROM `ts_finishes` WHERE `trackId` = " + rTrack.getId() + " AND `isRemoved` = 0 GROUP BY `uuid`) AND `trackId` = " + rTrack.getId() + " GROUP BY `uuid` ORDER BY `time` ASC, `date` ASC;");
            for (DbRow finish : resultFinishes) {
                rTrack.addTimeTrialFinish(new TimeTrialFinish(finish));
            }

        }

        var trackRegions = DB.getResults("SELECT * FROM `ts_regions` WHERE `isRemoved` = 0;");
        for (DbRow region : trackRegions) {
            Optional<Track> maybeTrack = getTrackById(region.getInt("trackId"));


            if (maybeTrack.isPresent()) {
                var rTrack = maybeTrack.get();
                TrackRegion trackRegion;
                if (!rTrack.getSpawnLocation().isWorldLoaded()) {
                    continue;
                }


                if (region.getString("regionShape") != null && TrackRegion.RegionShape.POLY.name().equalsIgnoreCase(region.getString("regionShape"))) {
                    var pointRows = DB.getResults("SELECT * FROM `ts_points` WHERE `regionId` = " + region.getInt("id") + ";");
                    List<BlockVector2> points = new ArrayList<>();
                    for (DbRow pointData : pointRows) {
                        points.add(BlockVector2.at(pointData.get("x"), pointData.get("z")));
                    }
                    trackRegion = new TrackPolyRegion(region, points);
                } else {
                    trackRegion = new TrackCuboidRegion(region);
                }
                if (trackRegion.getRegionType().equals(TrackRegion.RegionType.START)) {
                    addTrackRegion(trackRegion);
                }
                rTrack.addRegion(trackRegion);
            }
        }

        var locations = DB.getResults("SELECT * FROM `ts_locations` WHERE `type` = 'GRID'");
        for (DbRow dbRow : locations) {
            Optional<Track> maybeTrack = getTrackById(dbRow.getInt("trackId"));
            if (maybeTrack.isPresent()) {
                Location loc = ApiUtilities.stringToLocation(dbRow.getString("location"));
                Integer index = dbRow.getInt("index");
                maybeTrack.get().addGridLocation(loc, index);
            }
        }
    }

    public static Track trackNew(String name, UUID uuid, Location location, Track.TrackType type, ItemStack gui) {
        try {
            long date = ApiUtilities.getTimestamp();

            Location leaderboard = location.clone();
            leaderboard.setY(leaderboard.getY() + 3);
            // Save the track
            var trackId = DB.executeInsert("INSERT INTO `ts_tracks` " +
                    "(`uuid`, `name`, `dateCreated`, `guiItem`, `spawn`, `leaderboard`, `type`, `mode`, `toggleOpen`, `options`, `isRemoved`) " +
                    "VALUES('" + uuid + "', " +
                    Database.sqlString(name) + ", " + date + ", " +
                    Database.sqlString(ApiUtilities.itemToString(gui)) + ", '" + ApiUtilities.locationToString(location) + "', '" + ApiUtilities.locationToString(leaderboard) + "', " +
                    Database.sqlString(type == null ? null : type.toString()) + "," +
                    Database.sqlString(Track.TrackMode.TIMETRIAL.toString()) + ", 0, NULL , 0);");

            var dbRow = DB.getFirstRow("SELECT * FROM `ts_tracks` WHERE `id` = " + trackId + ";");

            Track rTrack = new Track(dbRow);
            tracks.add(rTrack);

            return rTrack;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static TrackRegion trackRegionNew(Region selection, long trackId, int index, TrackRegion.RegionType type, Location location) throws SQLException {
        Long regionId;
        String minP = ApiUtilities.locationToString(BukkitAdapter.adapt(location.getWorld(), selection.getMinimumPoint()));
        String maxP = ApiUtilities.locationToString(BukkitAdapter.adapt(location.getWorld(), selection.getMaximumPoint()));

        if (selection instanceof Polygonal2DRegion polySelection) {
            regionId = DB.executeInsert("INSERT INTO `ts_regions` (`trackId`, `regionIndex`, `regionType`, `regionShape`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + ", " + index + ", " +
                    Database.sqlString(type.toString()) + ", " + Database.sqlString(TrackRegion.RegionShape.POLY.toString()) + ", '" + minP + "', '" + maxP + "','" + ApiUtilities.locationToString(location) + "', 0);");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_regions` WHERE `id` = " + regionId + ";");
            for (BlockVector2 v : polySelection.getPoints()) {
                DB.executeInsert("INSERT INTO `ts_points` (`regionId`, `x`, `z`) VALUES(" + regionId + ", " + v.getBlockX() + ", " + v.getBlockZ() + ");" );
            }
            return new TrackPolyRegion(dbRow, polySelection.getPoints());

        } else {
            regionId = DB.executeInsert("INSERT INTO `ts_regions` (`trackId`, `regionIndex`, `regionType`, `regionShape`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + "," + index + ", " +
                    Database.sqlString(type.toString()) + ", " + Database.sqlString(TrackRegion.RegionShape.CUBOID.toString()) + ", '" + minP + "', '" + maxP + "','" + ApiUtilities.locationToString(location) + "', 0);");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_regions` WHERE `id` = " + regionId + ";");
            return new TrackCuboidRegion(dbRow);
        }
    }

    static public void removeTrack(Track track) {
        DB.executeUpdateAsync("UPDATE `ts_regions` SET `isRemoved` = 1 WHERE `trackId` = " + track.getId() + ";");
        DB.executeUpdateAsync("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `trackId` = " + track.getId() + ";");
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `isRemoved` = 1 WHERE `id` = " + track.getId() + ";");
        regions.removeIf(trackRegion -> trackRegion.getTrackId() == track.getId());
        tracks.remove(track);
        var events = EventDatabase.getEvents().stream().filter(event -> event.getTrack() != null).filter(event -> event.getTrack().equals(track.getId())).collect(Collectors.toList());
        for (Event event : events) {
            EventDatabase.removeEvent(event);
        }
        LeaderboardManager.removeLeaderboard(track.getId());
    }

    static public Optional<Track> getTrack(String name) {
        for (Track t : tracks) {
            if (t.getCommandName().equalsIgnoreCase(name)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    static public Optional<Track> getTrackById(int id) {
        for (Track t : tracks) {
            if (t.getId() == id) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    static public List<Track> getTracks() {
        return tracks;
    }

    static public List<Track> getAvailableTracks(Player player) {
        if (!player.hasPermission("track.admin") && !player.isOp()) {
            return TrackDatabase.getTracks().stream().filter(Track::isOpen).toList();
        }

        return getTracks();
    }

    static public List<Track> getOpenTracks() {
        return TrackDatabase.getTracks().stream().filter(Track::isOpen).toList();
    }

    static public List<TrackRegion> getTrackStartRegions() {
        return regions.stream().filter(r -> r.getRegionType().equals(TrackRegion.RegionType.START)).collect(Collectors.toList());
    }

    public static boolean trackNameAvailable(String name) {

        for (Track rTrack : tracks) {
            if (rTrack.getCommandName().equalsIgnoreCase(name.replaceAll(" ", ""))) {
                return false;
            }
        }
        return true;
    }

    static public void addTrackRegion(TrackRegion region) {
        regions.add(region);
    }

    static public void removeTrackRegion(TrackRegion region) {
        regions.remove(region);
    }

    public static List<String> getTracksAsStrings() {
        List<String> tracks = new ArrayList<>();
        getTracks().stream().forEach(track -> tracks.add(track.getCommandName()));
        return tracks;
    }

    public static ContextResolver<Track, BukkitCommandExecutionContext> getTrackContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            var maybeTrack = getTrack(name);
            if (maybeTrack.isPresent()) {
                return maybeTrack.get();
            } else {
                // User didn't type an Event, show error!
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static void unload(){
        tracks = new ArrayList<>();
        regions = new ArrayList<>();
    }
}
