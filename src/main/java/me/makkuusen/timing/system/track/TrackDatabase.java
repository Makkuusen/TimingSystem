package me.makkuusen.timing.system.track;

import co.aikar.commands.BukkitCommandCompletionContext;
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
import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.timetrial.TimeTrialAttempt;
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

    private static List<Track> tracks = new ArrayList<>();
    private static List<TrackRegion> startRegions = new ArrayList<>();

    public static void initDatabaseSynchronize() throws SQLException {
        loadTags();
        loadTracksAndTimeTrials();
        loadTrackRegions();
        loadTrackLocations();
        clearUselessEndRegions();
    }

    private static void loadTracksAndTimeTrials() throws SQLException {
        var dbRows = DB.getResults("SELECT * FROM `ts_tracks` WHERE `isRemoved` = 0;");

        for (DbRow dbRow : dbRows) {
            Track rTrack = new Track(dbRow);
            tracks.add(rTrack);
            TimingSystem.getPlugin().getLogger().info("LOADING IN " + rTrack.getDisplayName());

            loadFinishes(rTrack);
            loadAttempts(rTrack);
            loadTrackTags(rTrack);
        }
    }

    private static void loadFinishes(Track rTrack) throws SQLException {
        //var resultFinishes = DB.getResults("SELECT * FROM `ts_finishes` WHERE ( `uuid`,`time`) IN (SELECT `uuid`, min(`time`) FROM `ts_finishes` WHERE `trackId` = " + rTrack.getId() + " AND `isRemoved` = 0 GROUP BY `uuid`) AND `trackId` = " + rTrack.getId() + " GROUP BY `uuid` ORDER BY `time` ASC, `date` ASC;");
        var resultFinishes = DB.getResults("SELECT * FROM `ts_finishes` WHERE `trackId` = " + rTrack.getId() + " AND `isRemoved` = 0;");
        for (DbRow finish : resultFinishes) {
            var uuid = finish.getString("uuid") == null ? null : UUID.fromString(finish.getString("uuid"));
            if (Database.getPlayer(uuid) != null) {
                rTrack.addTimeTrialFinish(new TimeTrialFinish(finish));
            }
        }
    }

    private static void loadAttempts(Track rTrack) throws SQLException {
        var attempts = DB.getResults("SELECT * FROM `ts_attempts` WHERE `trackId` = " + rTrack.getId() + ";");
        for (DbRow attempt : attempts) {
            var uuid = attempt.getString("uuid") == null ? null : UUID.fromString(attempt.getString("uuid"));
            if (Database.getPlayer(uuid) != null) {
                rTrack.addTimeTrialAttempt(new TimeTrialAttempt(attempt));
            }
        }
    }

    private static void loadTrackTags(Track rTrack) throws SQLException {
        var tags = DB.getResults("SELECT * FROM `ts_tracks_tags` WHERE `trackId` = " + rTrack.getId() + ";");
        for (DbRow tag : tags) {
            var trackTag = TrackTagManager.getTrackTag(tag.getString("tag"));
            if (trackTag != null) {
                rTrack.addTag(trackTag);
            }
        }

    }

    private static void loadTrackRegions() throws SQLException {
        var trackRegions = DB.getResults("SELECT * FROM `ts_regions` WHERE `isRemoved` = 0;");
        for (DbRow region : trackRegions) {
            Optional<Track> maybeTrack = getTrackById(region.getInt("trackId"));

            if (maybeTrack.isPresent()) {
                var rTrack = maybeTrack.get();
                TrackRegion trackRegion;
                if (!rTrack.getSpawnLocation().isWorldLoaded()) {
                    continue;
                }

                try {
                    TrackRegion.RegionType.valueOf(region.getString("regionType"));
                } catch (IllegalArgumentException e) {
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
    }

    private static void clearUselessEndRegions() {
        for (Track track : tracks) {
            if (track.hasRegion(TrackRegion.RegionType.START) && track.hasRegion(TrackRegion.RegionType.END)) {
                for (TrackRegion startRegion : track.getRegions(TrackRegion.RegionType.START)) {
                    for (TrackRegion endRegion : track.getRegions(TrackRegion.RegionType.END)) {
                        if (startRegion.hasEqualBounds(endRegion)) {
                            track.removeRegion(endRegion);
                        }
                    }
                }
            }
        }
    }

    private static void loadTrackLocations() throws SQLException {
        var locations = DB.getResults("SELECT * FROM `ts_locations`");
        for (DbRow dbRow : locations) {

            Optional<Track> maybeTrack = getTrackById(dbRow.getInt("trackId"));
            if (maybeTrack.isEmpty()) {
                continue;
            }
            // Check that type is an actual valid TrackLocation. For e.g. Nidos camera system store other values here.
            try {
                TrackLocation.Type.valueOf(dbRow.getString("type"));
            } catch (IllegalArgumentException exception) {
                continue;
            }
            TrackLocation trackLocation;
            if (TrackLocation.Type.valueOf(dbRow.getString("type")) == TrackLocation.Type.LEADERBOARD) {
                trackLocation = new TrackLeaderboard(dbRow);
            } else {
                trackLocation = new TrackLocation(dbRow);
            }
            maybeTrack.get().addTrackLocation(trackLocation);
        }
    }

    public static void loadTags() throws SQLException {
        var trackTags = DB.getResults("SELECT * FROM `ts_tags`");
        for (DbRow dbRow : trackTags) {
            TrackTagManager.addTag(new TrackTag(dbRow));
        }
    }

    public static Track trackNew(String name, UUID uuid, Location location, Track.TrackType type, ItemStack gui) {
        try {
            long date = ApiUtilities.getTimestamp();

            Location leaderboard = location.clone();
            leaderboard.setY(leaderboard.getY() + 3);
            // Save the track
            var trackId = DB.executeInsert("INSERT INTO `ts_tracks` " + "(`uuid`, `name`, `dateCreated`, `weight`, `guiItem`, `spawn`, `leaderboard`, `type`, `mode`, `toggleOpen`, `options`, `isRemoved`) " + "VALUES('" + uuid + "', " + Database.sqlString(name) + ", " + date + ", 100, " + Database.sqlString(ApiUtilities.itemToString(gui)) + ", '" + ApiUtilities.locationToString(location) + "', '" + ApiUtilities.locationToString(leaderboard) + "', " + Database.sqlString(type == null ? null : type.toString()) + "," + Database.sqlString(Track.TrackMode.TIMETRIAL.toString()) + ", 0, NULL , 0);");

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
            regionId = DB.executeInsert("INSERT INTO `ts_regions` (`trackId`, `regionIndex`, `regionType`, `regionShape`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + ", " + index + ", " + Database.sqlString(type.toString()) + ", " + Database.sqlString(TrackRegion.RegionShape.POLY.toString()) + ", '" + minP + "', '" + maxP + "','" + ApiUtilities.locationToString(location) + "', 0);");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_regions` WHERE `id` = " + regionId + ";");
            for (BlockVector2 v : polySelection.getPoints()) {
                DB.executeInsert("INSERT INTO `ts_points` (`regionId`, `x`, `z`) VALUES(" + regionId + ", " + v.getBlockX() + ", " + v.getBlockZ() + ");");
            }
            return new TrackPolyRegion(dbRow, polySelection.getPoints());

        } else {
            regionId = DB.executeInsert("INSERT INTO `ts_regions` (`trackId`, `regionIndex`, `regionType`, `regionShape`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + "," + index + ", " + Database.sqlString(type.toString()) + ", " + Database.sqlString(TrackRegion.RegionShape.CUBOID.toString()) + ", '" + minP + "', '" + maxP + "','" + ApiUtilities.locationToString(location) + "', 0);");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_regions` WHERE `id` = " + regionId + ";");
            return new TrackCuboidRegion(dbRow);
        }
    }

    public static TrackLocation trackLocationNew(int trackId, int index, TrackLocation.Type type, Location location) throws SQLException {
        DB.executeInsert("INSERT INTO `ts_locations` (`trackId`, `index`, `type`, `location`) VALUES(" + trackId + ", " + index + ", '" + type.name() + "', '" + ApiUtilities.locationToString(location) + "');");
        if (type == TrackLocation.Type.LEADERBOARD) {
            return new TrackLeaderboard(trackId, index, location, type);
        } else {
            return new TrackLocation(trackId, index, location, type);
        }

    }

    static public void removeTrack(Track track) {
        DB.executeUpdateAsync("UPDATE `ts_regions` SET `isRemoved` = 1 WHERE `trackId` = " + track.getId() + ";");
        DB.executeUpdateAsync("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `trackId` = " + track.getId() + ";");
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `isRemoved` = 1 WHERE `id` = " + track.getId() + ";");
        LeaderboardManager.removeLeaderboards(track);
        startRegions.removeIf(trackRegion -> trackRegion.getTrackId() == track.getId());
        tracks.remove(track);
        var events = EventDatabase.getEvents().stream().filter(event -> event.getTrack() != null).filter(event -> event.getTrack().equals(track.getId())).toList();
        for (Event event : events) {
            EventDatabase.removeEvent(event);
        }

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
        return startRegions.stream().filter(r -> r.getRegionType().equals(TrackRegion.RegionType.START)).collect(Collectors.toList());
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
        startRegions.add(region);
    }

    static public void removeTrackRegion(TrackRegion region) {
        startRegions.remove(region);
    }

    public static List<String> getTracksAsStrings() {
        List<String> tracks = new ArrayList<>();
        getTracks().forEach(track -> tracks.add(track.getCommandName()));
        return tracks;
    }

    public static List<String> getRegionsAsStrings(BukkitCommandCompletionContext c) {
        List<String> regions = new ArrayList<>();
        var maybeTrack = TimingSystem.playerEditingSession.get(c.getPlayer().getUniqueId());
        if (maybeTrack == null) {
            return regions;
        }
        maybeTrack.getRegions().forEach(region -> regions.add(region.getRegionType().name().toLowerCase() + "-" + region.getRegionIndex()));

        return regions;
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

    public static ContextResolver<TrackRegion, BukkitCommandExecutionContext> getRegionContextResolver() {
        return (c) -> {
            String region = c.popFirstArg();
            var maybeTrack = TimingSystem.playerEditingSession.get(c.getPlayer().getUniqueId());
            if (maybeTrack != null) {
                try {
                    String[] regionName = region.split("-");
                    int index = Integer.parseInt(regionName[1]);
                    String regionType = regionName[0];
                    var maybeRegion = maybeTrack.getRegion(TrackRegion.RegionType.valueOf(regionType.toUpperCase()), index);
                    if (maybeRegion.isPresent()) {
                        return maybeRegion.get();
                    }
                } catch (Exception ignored) {

                }
            }
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        };
    }

    public static void unload() {
        LeaderboardManager.removeAllLeaderboards();
        tracks = new ArrayList<>();
        startRegions = new ArrayList<>();
    }
}
