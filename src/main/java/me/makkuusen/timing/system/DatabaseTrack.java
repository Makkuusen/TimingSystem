package me.makkuusen.timing.system;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class DatabaseTrack {

    public static TimingSystem plugin;
    private static final List<Track> tracks = new ArrayList<>();
    private static final List<TrackRegion> regions = new ArrayList<>();

    public static void initDatabaseSynchronize() throws SQLException {
        var dbRows = DB.getResults("SELECT * FROM `tracks` WHERE `isRemoved` = 0;");

        for (DbRow dbRow : dbRows) {
            Track rTrack = new Track(dbRow);
            tracks.add(rTrack);

            var resultFinishes = DB.getResults("SELECT * FROM `tracksFinishes` WHERE (`uuid`,`time`) IN (SELECT `uuid`, min(`time`) FROM `tracksFinishes` WHERE `trackId` = " + rTrack.getId() + " AND `isRemoved` = 0 GROUP BY `uuid`) AND `isRemoved` = 0 ORDER BY `time`;");
            for (DbRow finish : resultFinishes) {
                rTrack.addTimeTrialFinish(new TimeTrialFinish(finish));
            }

        }

        var trackRegions = DB.getResults("SELECT * FROM `tracksRegions` WHERE `isRemoved` = 0;");
        for (DbRow region : trackRegions) {
            Optional<Track> maybeTrack = getTrackById(region.getInt("trackId"));
            if (maybeTrack.isPresent())
            {
                var rTrack = maybeTrack.get();
                TrackRegion trackRegion = new TrackRegion(region);
                if (trackRegion.getRegionType().equals(TrackRegion.RegionType.START))
                {
                    rTrack.newStartRegion(trackRegion);
                    addTrackRegion(trackRegion);
                }
                else if (trackRegion.getRegionType().equals(TrackRegion.RegionType.END))
                {
                    rTrack.newEndRegion(trackRegion);
                }
                else if (trackRegion.getRegionType().equals(TrackRegion.RegionType.CHECKPOINT))
                {
                    rTrack.addCheckpoint(trackRegion);
                }
                else if (trackRegion.getRegionType().equals(TrackRegion.RegionType.RESET))
                {
                    rTrack.addResetRegion(trackRegion);
                }
                else if (trackRegion.getRegionType().equals(TrackRegion.RegionType.GRID))
                {
                    rTrack.addGridRegion(trackRegion);
                }
                else if (trackRegion.getRegionType().equals(TrackRegion.RegionType.PIT))
                {
                    rTrack.newPitRegion(trackRegion);
                }
            }
        }
    }

    public static Track trackNew(String name, UUID uuid, Location location, Track.TrackType type, ItemStack gui)
    {
        try
        {
            long date = ApiUtilities.getTimestamp();

            Location leaderboard = location.clone();
            leaderboard.setY(leaderboard.getY() + 3);
            // Save the track
            var trackId = DB.executeInsert("INSERT INTO `tracks` " +
                    "(`uuid`, `name`, `dateCreated`, `guiItem`, `spawn`, `leaderboard`, `type`, `mode`, `toggleOpen`, `toggleGovernment`, `options`, `isRemoved`) " +
                    "VALUES('" + uuid + "', " +
                    Database.sqlString(name) + ", " + date + ", " +
                    Database.sqlString(ApiUtilities.itemToString(gui)) + ", '" + ApiUtilities.locationToString(location) + "', '" + ApiUtilities.locationToString(leaderboard) + "', " +
                    Database.sqlString(type == null ? null : type.toString()) + "," +
                    Database.sqlString(Track.TrackMode.TIMETRIAL.toString()) + ", 0, 0, NULL , 0);");

            var dbRow = DB.getFirstRow("SELECT * FROM `tracks` WHERE `id` = " + trackId + ";");

            Track rTrack = new Track(dbRow);
            tracks.add(rTrack);

            TrackRegion startRegion = trackRegionNew(trackId, TrackRegion.RegionType.START, location);
            rTrack.newStartRegion(startRegion);
            regions.add(startRegion);

            TrackRegion endRegion = trackRegionNew(trackId, TrackRegion.RegionType.END, location);
            rTrack.newEndRegion(endRegion);

            TrackRegion pitRegion = trackRegionNew(trackId, TrackRegion.RegionType.PIT, location);
            rTrack.newPitRegion(pitRegion);

            return rTrack;
        } catch (SQLException exception)
        {
            exception.printStackTrace();
            return null;
        }
    }

    public static TrackRegion trackRegionNew(long trackId, TrackRegion.RegionType type, Location location) throws SQLException {
        var regionId = DB.executeInsert("INSERT INTO `tracksRegions` (`trackId`, `regionIndex`, `regionType`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + ", 0, " +
                Database.sqlString(type.toString()) + ", NULL, NULL, '" + ApiUtilities.locationToString(location) + "', 0);");
        var dbRow = DB.getFirstRow("SELECT * FROM `tracksRegions` WHERE `id` = " + regionId + ";");
        return new TrackRegion(dbRow);

    }

    static public void removeTrack(Track Track)
    {
        DB.executeUpdateAsync("UPDATE `tracksRegions` SET `isRemoved` = 1 WHERE `trackId` = " + Track.getId() + ";");
        DB.executeUpdateAsync("UPDATE `tracksFinishes` SET `isRemoved` = 1 WHERE `trackId` = " + Track.getId() + ";");
        DB.executeUpdateAsync("UPDATE `tracks` SET `isRemoved` = 1 WHERE `id` = " + Track.getId() + ";");
        regions.removeIf(trackRegion -> trackRegion.getTrackId() == Track.getId());
        tracks.remove(Track);
        LeaderboardManager.removeLeaderboard(Track.getId());
    }

    static public Optional<Track> getTrack(String name)
    {
        for (Track t : tracks)
        {
            if (t.getName().equalsIgnoreCase(name))
            {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    static public Optional<Track> getTrackById(int id)
    {
        for (Track t : tracks)
        {
            if (t.getId() == id)
            {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    static public List<Track> getTracks()
    {
        return tracks;
    }

    static public List<Track> getAvailableTracks(Player player)
    {
        if (!player.hasPermission("track.admin") && !player.isOp())
        {
            return DatabaseTrack.getTracks().stream().filter(Track::isOpen).toList();
        }

        return getTracks();
    }

    static public List<TrackRegion> getTrackRegions()
    {
        return regions;
    }
    static public List<TrackRegion> getTrackStartRegions()
    {
        return regions.stream().filter(r -> r.getRegionType().equals(TrackRegion.RegionType.START)).collect(Collectors.toList());
    }

    public static boolean trackNameAvailable(String name)
    {

        for (Track rTrack : tracks)
        {
            if (rTrack.getName().equalsIgnoreCase(name))
            {
                return false;
            }
        }
        return true;
    }

    static public void addTrackRegion(TrackRegion region)
    {
        regions.add(region);
    }

    static public void removeTrackRegion(TrackRegion region)
    {
        regions.remove(region);
    }

    public static List<String> getTracksAsStrings(){
        List<String> tracks = new ArrayList<>();
        getTracks().stream().forEach(track -> tracks.add(track.getName()));
        return tracks;
    }

    public static ContextResolver<Track, BukkitCommandExecutionContext> getTrackContextResolver() {
        return (c) -> {
            String[] ts = new String[c.getArgs().size()];
            c.getArgs().toArray(ts);
            String trackName = ApiUtilities.concat(ts, 0);
            var maybeTrack = getTrack(trackName);
            if (maybeTrack.isPresent()) {
                return maybeTrack.get();
            } else {
                // User didn't type an Event, show error!
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }
}
