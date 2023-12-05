package me.makkuusen.timing.system.database;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DbRow;
import co.aikar.taskchain.TaskChain;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import me.makkuusen.timing.system.*;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.permissions.PermissionTrack;
import me.makkuusen.timing.system.timetrial.TimeTrialAttempt;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.track.*;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public interface TrackDatabase {
    List<Track> tracks = new ArrayList<>();
    List<TrackRegion> startRegions = new ArrayList<>();

    List<DbRow> selectTracks() throws SQLException;

    DbRow selectTrack(long trackId) throws SQLException;

    List<DbRow> selectFinishes() throws SQLException;

    List<DbRow> selectAttempts() throws SQLException;

    List<DbRow> selectTrackTags() throws SQLException;

    List<DbRow> selectTags() throws SQLException;

    List<DbRow> selectCheckpointTimes() throws SQLException;

    List<DbRow> selectTrackRegions() throws SQLException;

    DbRow selectTrackRegion(long regionId) throws SQLException;

    List<DbRow> selectRegionPoints(int regionId) throws SQLException;

    List<DbRow> selectLocations() throws SQLException;

    long createTrack(String uuid, String name, long date, int weight, ItemStack gui, Location location, Location leaderboard, Track.TrackMode mode, Track.TrackType type, BoatUtilsMode boatUtilsMode) throws SQLException;

    long createRegion(long trackId, int index, String minP, String maxP, TrackRegion.RegionType type, TrackRegion.RegionShape shape, Location location) throws SQLException;

    long createPoint(long regionId, BlockVector2 v) throws SQLException;

    long createLocation(long trackId, int index, TrackLocation.Type type, Location location) throws SQLException;

    void removeTrack(long trackId);

    void createTagAsync(TrackTag tag, TextColor color, ItemStack item);

    void deleteTagAsync(TrackTag tag);

    void deletePoint(long regionId);

    void deleteLocation(int trackId, int index, TrackLocation.Type type);

    void updateLocation(int index, Location location, TrackLocation.Type type, long trackId);

    void addTagToTrack(int trackId, TrackTag tag);

    void tagSet(String tag, String column, String value);

    void removeFinish(int finishId);

    void removeAllFinishes(int trackId, UUID uuid);

    void removeAllFinishes(int trackId);

    void removeTagFromTrack(int trackId, TrackTag tag);

    void trackSet(int trackId, String column, String value);

    void trackRegionSet(int regionId, String column, String value);

    void createCheckpointFinish(long finishId, int checkpointIndex, long time);

    void createAttempt(int id, UUID uuid, long date, long time);


    static void initDatabaseSynchronize() throws SQLException {
        loadTags();
        loadTracksAndTimeTrials();
        loadTrackRegions();
        loadTrackLocations();
    }

    private static void loadTracksAndTimeTrials() throws SQLException {
        var dbRows = TimingSystem.getTrackDatabase().selectTracks();

        for (DbRow dbRow : dbRows) {
            Track rTrack = new Track(dbRow);
            tracks.add(rTrack);
            TimingSystem.getPlugin().getLogger().info("LOADING IN " + rTrack.getDisplayName());
            loadTrackTags();
        }
    }

    static void loadTrackFinishesAsync() {
        TaskChain<?> chain = TimingSystem.newChain();
        TimingSystem.getPlugin().getLogger().warning("Async loading started'");

        chain.async(TrackDatabase::loadFinishes)
                .async(TrackDatabase::loadAttempts)
                .delay(20)
                .async(TrackDatabase::loadCheckpointTimes)
                .execute((finished) -> TimingSystem.getPlugin().getLogger().warning("Loading of finishes completed"));
    }

    private static void loadFinishes() {
        // This comment seems to just be chilling:
        //var resultFinishes = DB.getResults("SELECT * FROM `ts_finishes` WHERE ( `uuid`,`time`) IN (SELECT `uuid`, min(`time`) FROM `ts_finishes` WHERE `trackId` = " + rTrack.getId() + " AND `isRemoved` = 0 GROUP BY `uuid`) AND `trackId` = " + rTrack.getId() + " GROUP BY `uuid` ORDER BY `time` ASC, `date` ASC;");
        TimingSystem.getPlugin().getLogger().warning("Start loading finishes");
        try {
            var resultFinishes = TimingSystem.getTrackDatabase().selectFinishes();
            for (DbRow finish : resultFinishes) {
                var uuid = finish.getString("uuid") == null ? null : UUID.fromString(finish.getString("uuid"));
                if (TSDatabase.getPlayer(uuid) != null) {
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
            var attempts = TimingSystem.getTrackDatabase().selectAttempts();
            for (DbRow attempt : attempts) {
                var uuid = attempt.getString("uuid") == null ? null : UUID.fromString(attempt.getString("uuid"));
                if (TSDatabase.getPlayer(uuid) != null) {
                    var maybeTrack = getTrackById(attempt.getInt("trackId"));
                    maybeTrack.ifPresent(track -> track.addTimeTrialAttempt(new TimeTrialAttempt(attempt)));
                }
            }
        } catch (SQLException ignore) {
        }
        TimingSystem.getPlugin().getLogger().warning("Finish loading attempts");
    }

    private static void loadTrackTags() throws SQLException {
        var tags = TimingSystem.getTrackDatabase().selectTrackTags();
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

            var checkpointResults = TimingSystem.getTrackDatabase().selectCheckpointTimes();
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
            for (Track rTrack : tracks) {
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
        var trackRegions = TimingSystem.getTrackDatabase().selectTrackRegions();
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
                    var pointRows = TimingSystem.getTrackDatabase().selectRegionPoints(region.getInt("id"));
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
        var locations = TimingSystem.getTrackDatabase().selectLocations();
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

    private static void loadTags() throws SQLException {
        var trackTags = TimingSystem.getTrackDatabase().selectTags();
        for (DbRow dbRow : trackTags) {
            TrackTagManager.addTag(new TrackTag(dbRow));
        }
    }

    static Track trackNew(String name, UUID uuid, Location location, Track.TrackType type, ItemStack gui) {
        try {
            long date = ApiUtilities.getTimestamp();

            Location leaderboard = location.clone();
            leaderboard.setY(leaderboard.getY() + 3);
            // Save the track
            var trackId = TimingSystem.getTrackDatabase().createTrack(uuid.toString(), name, date, 100, gui, location, leaderboard, Track.TrackMode.TIMETRIAL, type, BoatUtilsMode.VANILLA);

            var dbRow = TimingSystem.getTrackDatabase().selectTrack(trackId);

            Track rTrack = new Track(dbRow);
            tracks.add(rTrack);

            return rTrack;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }


    public static Track trackNewFromTrackExchange(TrackExchangeTrack trackExchangeTrack, Location newSpawnLocation, Vector offset, Player player) {
        try {
            World world = newSpawnLocation.getWorld();
            String name = trackExchangeTrack.getName();
            UUID uuid = trackExchangeTrack.getOwnerUUID();
            if (TSDatabase.getPlayer(uuid) == null) {
                uuid = player.getUniqueId();
            }
            long dateCreated = trackExchangeTrack.getDateCreated();
            Track.TrackType trackType = trackExchangeTrack.getTrackType();
            Track.TrackMode trackMode = trackExchangeTrack.getTrackMode();
            ItemStack gui = trackExchangeTrack.getGuiItem();
            int weight = trackExchangeTrack.getWeight();
            BoatUtilsMode boatUtilsMode = trackExchangeTrack.getBoatUtilsMode();

            // Create track
            var trackId = TimingSystem.getTrackDatabase().createTrack(uuid.toString(), name, dateCreated, weight, gui, newSpawnLocation, newSpawnLocation, trackMode, trackType, boatUtilsMode);
            var dbRow = TimingSystem.getTrackDatabase().selectTrack(trackId);

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
                if (location instanceof TrackLeaderboard leaderboard)
                    leaderboard.createOrUpdateHologram();
            });

            return rTrack;
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    static TrackRegion trackRegionNew(Region selection, long trackId, int index, TrackRegion.RegionType type, Location location) throws SQLException {
        Long regionId;
        Location minPLoc = BukkitAdapter.adapt(location.getWorld(), selection.getMinimumPoint());
        Location maxPLoc = BukkitAdapter.adapt(location.getWorld(), selection.getMaximumPoint());
        String minP = ApiUtilities.locationToString(minPLoc);
        String maxP = ApiUtilities.locationToString(maxPLoc);

        if (selection instanceof Polygonal2DRegion polySelection) {
            regionId = TimingSystem.getTrackDatabase().createRegion(trackId, index, minP, maxP, type, TrackRegion.RegionShape.POLY, location);
            for (BlockVector2 v : polySelection.getPoints()) {
                TimingSystem.getTrackDatabase().createPoint(regionId, v);
            }
            return new TrackPolyRegion(regionId, trackId, index, type, location, minPLoc, maxPLoc, polySelection.getPoints());

        } else {
            regionId = TimingSystem.getTrackDatabase().createRegion(trackId, index, minP, maxP, type, TrackRegion.RegionShape.CUBOID, location);
            return new TrackCuboidRegion(regionId, trackId, index, type, location, minPLoc, maxPLoc);
        }
    }

    static TrackLocation trackLocationNew(int trackId, int index, TrackLocation.Type type, Location location) throws SQLException {
        TimingSystem.getTrackDatabase().createLocation(trackId, index, type, location);
        if (type == TrackLocation.Type.LEADERBOARD) {
            return new TrackLeaderboard(trackId, index, location, type);
        } else {
            return new TrackLocation(trackId, index, location, type);
        }

    }

    static void removeTrack(Track track) {
        TimingSystem.getTrackDatabase().removeTrack(track.getId());
        LeaderboardManager.removeLeaderboards(track);
        startRegions.removeIf(trackRegion -> trackRegion.getTrackId() == track.getId());
        tracks.remove(track);
        var events = EventDatabase.events.stream().filter(event -> event.getTrack() != null).filter(event -> event.getTrack().getId() == track.getId()).toList();
        for (Event event : events) {
            EventDatabase.removeEvent(event);
        }

    }

    static Optional<Track> getTrack(String name) {
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

    static List<Track> getAvailableTracks(Player player) {
        if (!player.hasPermission("timingsystem.packs.trackadmin") && !player.isOp()) {
            return TrackDatabase.tracks.stream().filter(Track::isOpen).toList();
        }

        return tracks;
    }

    static List<Track> getOpenTracks() {
        return TrackDatabase.tracks.stream().filter(Track::isOpen).toList();
    }

    static List<TrackRegion> getTrackStartRegions() {
        return startRegions.stream().filter(r -> r.getRegionType().equals(TrackRegion.RegionType.START)).collect(Collectors.toList());
    }

    static boolean trackNameNotAvailable(String name) {

        for (Track rTrack : tracks) {
            if (rTrack.getCommandName().equalsIgnoreCase(name.replaceAll(" ", ""))) {
                return true;
            }
        }
        return false;
    }

    static void addTrackRegion(TrackRegion region) {
        startRegions.add(region);
    }

    static void removeTrackRegion(TrackRegion region) {
        startRegions.remove(region);
    }

    static List<String> getTracksAsStrings(Player player) {
        List<String> tracks = new ArrayList<>();
        if (player.hasPermission(PermissionTrack.MENU.getNode()) || player.hasPermission("timingsystem.packs.trackadmin") || player.isOp()) {
            TrackDatabase.tracks.forEach(track -> tracks.add(track.getCommandName()));
        } else {
            getOpenTracks().forEach(track -> tracks.add(track.getCommandName()));
        }

        return tracks;
    }

    static List<String> getRegionsAsStrings(BukkitCommandCompletionContext c) {
        List<String> regions = new ArrayList<>();
        var maybeTrack = TimingSystem.playerEditingSession.get(c.getPlayer().getUniqueId());
        if (maybeTrack == null) {
            return regions;
        }
        maybeTrack.getRegions().forEach(region -> regions.add(region.getRegionType().name().toLowerCase() + "-" + region.getRegionIndex()));

        return regions;
    }

    static ContextResolver<Track, BukkitCommandExecutionContext> getTrackContextResolver() {
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

    static ContextResolver<TrackRegion, BukkitCommandExecutionContext> getRegionContextResolver() {
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

    static void unload() {
        LeaderboardManager.removeAllLeaderboards();
        tracks.clear();
        startRegions.clear();
    }
}
