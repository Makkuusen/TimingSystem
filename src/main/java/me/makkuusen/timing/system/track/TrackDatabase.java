package me.makkuusen.timing.system.track;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import co.aikar.taskchain.TaskChain;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.LeaderboardManager;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.permissions.PermissionTrack;
import me.makkuusen.timing.system.timetrial.TimeTrialAttempt;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class TrackDatabase {

    @Getter
    private static List<Track> tracks = new ArrayList<>();
    private static List<TrackRegion> startRegions = new ArrayList<>();

    public static void initDatabaseSynchronize() throws SQLException {
        loadTags();
        loadTracksAndTimeTrials();
        loadTrackRegions();
        loadTrackLocations();
    }

    private static void loadTracksAndTimeTrials() throws SQLException {
        var dbRows = DB.getResults("SELECT * FROM `ts_tracks` WHERE `isRemoved` = 0;");

        for (DbRow dbRow : dbRows) {
            Track rTrack = new Track(dbRow);
            tracks.add(rTrack);
            TimingSystem.getPlugin().getLogger().info("LOADING IN " + rTrack.getDisplayName());
            loadTrackTags();
        }
    }

    public static void loadTrackFinishesAsync() {


        TaskChain<?> chain = TimingSystem.newChain();
        TimingSystem.getPlugin().getLogger().warning("Async loading started'");

        chain.async(TrackDatabase::loadFinishes)
                .async(TrackDatabase::loadAttempts)
                .delay(20)
                .async(TrackDatabase::loadCheckpointTimes)
                .execute((finished) -> TimingSystem.getPlugin().getLogger().warning("Loading of finishes completed"));
    }

    private static void loadFinishes() {
        //var resultFinishes = DB.getResults("SELECT * FROM `ts_finishes` WHERE ( `uuid`,`time`) IN (SELECT `uuid`, min(`time`) FROM `ts_finishes` WHERE `trackId` = " + rTrack.getId() + " AND `isRemoved` = 0 GROUP BY `uuid`) AND `trackId` = " + rTrack.getId() + " GROUP BY `uuid` ORDER BY `time` ASC, `date` ASC;");
        TimingSystem.getPlugin().getLogger().warning("Start loading finishes");
        try {
            var resultFinishes = DB.getResults("SELECT * FROM `ts_finishes` WHERE `isRemoved` = 0;");
            for (DbRow finish : resultFinishes) {
                var uuid = finish.getString("uuid") == null ? null : UUID.fromString(finish.getString("uuid"));
                if (Database.getPlayer(uuid) != null) {
                    var maybeTrack = getTrackById(finish.getInt("trackId"));
                    maybeTrack.ifPresent(track -> track.addTimeTrialFinish(new TimeTrialFinish(finish)));
                }
            }
        } catch (SQLException e) {
            TaskChain.abort();
        }

        TimingSystem.getPlugin().getLogger().warning("Finish loading finishes");
    }

    private static void loadAttempts() {
        TimingSystem.getPlugin().getLogger().warning("Start loading attempts");
        try {
            var attempts = DB.getResults("SELECT * FROM `ts_attempts`;");
            for (DbRow attempt : attempts) {
                var uuid = attempt.getString("uuid") == null ? null : UUID.fromString(attempt.getString("uuid"));
                if (Database.getPlayer(uuid) != null) {
                    var maybeTrack = getTrackById(attempt.getInt("trackId"));
                    maybeTrack.ifPresent(track -> track.addTimeTrialAttempt(new TimeTrialAttempt(attempt)));
                }
            }
        } catch (SQLException ignore) {
        }
        TimingSystem.getPlugin().getLogger().warning("Finish loading attempts");
    }

    private static void loadTrackTags() throws SQLException {
        var tags = DB.getResults("SELECT * FROM `ts_tracks_tags`;");
        for (DbRow tag : tags) {
            var trackTag = TrackTagManager.getTrackTag(tag.getString("tag"));
            if (trackTag != null) {
                var maybeTrack = getTrackById(tag.getInt("trackId"));
                maybeTrack.ifPresent(track -> track.addTag(trackTag));
            }
        }
    }

    private static void loadCheckpointTimes() {
        TimingSystem.getPlugin().getLogger().warning("Start loading checkpoints");
        try {
            Map<Integer, Map<Integer, Long>> checkpoints = new HashMap<>();

            var checkpointResults = DB.getResults("SELECT * FROM `ts_finishes_checkpoints` WHERE `isRemoved` = 0;");
            if (!checkpointResults.isEmpty()) {
                for (DbRow checkpoint : checkpointResults) {
                    var finishId = checkpoint.getInt("finishId");
                    if (!checkpoints.containsKey(finishId)) {
                        checkpoints.put(finishId, new HashMap<>());
                    }
                    checkpoints.get(finishId).put(checkpoint.getInt("checkpointIndex"), Long.valueOf(checkpoint.getInt("time")));
                }
            }

            // Sort checkpoints into track
            for (Track rTrack : getTracks()) {
                var players = rTrack.getTimeTrialFinishes().keySet();
                for (TPlayer tPlayer : players) {
                    for (TimeTrialFinish finish : rTrack.getTimeTrialFinishes().get(tPlayer)) {
                        if (checkpoints.containsKey(finish.getId())) {
                            finish.updateCheckpointTimes(checkpoints.get(finish.getId()));
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
        }
        TimingSystem.getPlugin().getLogger().warning("finish loading checkpoints");
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

    public static Track trackNewFromTrackExchange(TrackExchangeTrack trackExchangeTrack, Location newSpawnLocation, Vector offset) {
        try {
            World world = newSpawnLocation.getWorld();
            String name = trackExchangeTrack.getName();
            UUID uuid = trackExchangeTrack.getOwnerUUID();
            long dateCreated = trackExchangeTrack.getDateCreated();
            Track.TrackType trackType = trackExchangeTrack.getTrackType();
            Track.TrackMode trackMode = trackExchangeTrack.getTrackMode();
            ItemStack gui = trackExchangeTrack.getGuiItem();
            int weight = trackExchangeTrack.getWeight();
            BoatUtilsMode boatUtilsMode = trackExchangeTrack.getBoatUtilsMode();

            // Create track
            var trackId = DB.executeInsert("INSERT INTO `ts_tracks` (`uuid`, `name`, `dateCreated`, `weight`, `guiItem`, `spawn`, `leaderboard`, `type`, `mode`, `toggleOpen`, `options`, `boatUtilsMode`, `isRemoved`) " + "VALUES('" + uuid + "', " + Database.sqlString(name) + ", " + dateCreated + ", " + weight + ", " + Database.sqlString(ApiUtilities.itemToString(gui)) + ", '" + ApiUtilities.locationToString(newSpawnLocation) + "', '" + ApiUtilities.locationToString(newSpawnLocation) + "', " + Database.sqlString(trackType.toString()) + "," + Database.sqlString(trackMode.toString()) + ", 0, NULL, " + Database.sqlString(boatUtilsMode.toString()) + " , 0);");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_tracks` WHERE `id` = " + trackId + ";");

            Track rTrack = new Track(dbRow);
            tracks.add(rTrack);

            // Create regions
            trackExchangeTrack.getTrackRegions().forEach(serializableRegion -> {
                try {
                    TrackRegion region;
                    Location newLocation = TrackExchangeTrack.getNewLocation(newSpawnLocation.getWorld(), serializableRegion.getSpawnLocation(world), offset);
                    Location minP = TrackExchangeTrack.getNewLocation(newSpawnLocation.getWorld(), serializableRegion.getMinP(world), offset);
                    Location maxP = TrackExchangeTrack.getNewLocation(newSpawnLocation.getWorld(), serializableRegion.getMaxP(world), offset);
                    if (serializableRegion.getRegionShape() == TrackRegion.RegionShape.POLY) {
                        List<BlockVector2> finalPoints = new ArrayList<>();
                        for (BlockVector2 point : serializableRegion.getPoints()) {
                            point = point.subtract(offset.getBlockX(), offset.getBlockZ());
                            finalPoints.add(point);
                        }
                        region = TrackDatabase.trackRegionNew(new Polygonal2DRegion(BukkitAdapter.adapt(newSpawnLocation.getWorld()), finalPoints, minP.getBlockY(), maxP.getBlockY()), trackId, serializableRegion.getRegionIndex(), serializableRegion.getRegionType(), newLocation);
                    } else
                        region = TrackDatabase.trackRegionNew(new CuboidRegion(BukkitAdapter.adapt(newSpawnLocation.getWorld()), BlockVector3.at(minP.getBlockX(), minP.getBlockY(), minP.getBlockZ()), BlockVector3.at(maxP.getBlockX(), maxP.getBlockY(), maxP.getBlockZ())), trackId, serializableRegion.getRegionIndex(), serializableRegion.getRegionType(), newLocation);

                    if(region.getRegionType() == TrackRegion.RegionType.START)
                        addTrackRegion(region);

                    rTrack.addRegion(region);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            // Create track locations
            trackExchangeTrack.getTrackLocations().stream().map(serializableLocation -> serializableLocation.toTrackLocation(world)).forEach(location -> {
                rTrack.addTrackLocation(location);
                if(location instanceof TrackLeaderboard leaderboard)
                    leaderboard.createOrUpdateHologram();
            });

            return rTrack;
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
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
        var events = EventDatabase.getEvents().stream().filter(event -> event.getTrack() != null).filter(event -> event.getTrack().getId() == track.getId()).toList();
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

    static public List<Track> getAvailableTracks(Player player) {
        if (!player.hasPermission("timingsystem.packs.trackadmin") && !player.isOp()) {
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

    public static boolean trackNameNotAvailable(String name) {

        for (Track rTrack : tracks) {
            if (rTrack.getCommandName().equalsIgnoreCase(name.replaceAll(" ", ""))) {
                return true;
            }
        }
        return false;
    }

    static public void addTrackRegion(TrackRegion region) {
        startRegions.add(region);
    }

    static public void removeTrackRegion(TrackRegion region) {
        startRegions.remove(region);
    }

    public static List<String> getTracksAsStrings(Player player) {
        List<String> tracks = new ArrayList<>();
        if (player.hasPermission(PermissionTrack.MENU.getNode()) || player.hasPermission("timingsystem.packs.trackadmin") || player.isOp()) {
            getTracks().forEach(track -> tracks.add(track.getCommandName()));
        } else {
            getOpenTracks().forEach(track -> tracks.add(track.getCommandName()));
        }

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
