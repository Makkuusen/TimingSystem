package me.makkuusen.timing.system;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class TrackDatabase
{
    public static TimingSystem plugin;
    private static final List<Track> tracks = new ArrayList<>();
    private static final List<TrackRegion> regions = new ArrayList<>();

    static void connect()
    {
        if (!ApiDatabase.initialize(plugin))
        {
            plugin.getLogger().warning("Failed to initialize database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }

        if (!ApiDatabase.synchronize())
        {
            plugin.getLogger().warning("Failed to synchronize database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
        initDatabaseSynchronize();
    }

    private static void initDatabaseSynchronize()
    {
        try
        {
            Connection connection = ApiDatabase.getConnection();
            Statement statement = connection.createStatement();

            ResultSet result = statement.executeQuery("SELECT * FROM `tracks` WHERE `isRemoved` = 0;");

            while (result.next())
            {
                Track rTrack = new Track(result);
                tracks.add(rTrack);

                Statement statementFinishes = connection.createStatement();
                ResultSet resultFinishes = statementFinishes.executeQuery("SELECT * FROM `tracksFinishes` WHERE (`uuid`,`time`) IN (SELECT `uuid`, min(`time`) FROM `tracksFinishes` WHERE `trackId` = " + rTrack.getId() + " AND `isRemoved` = 0 GROUP BY `uuid`) AND `isRemoved` = 0 ORDER BY `time`;");
                while (resultFinishes.next())
                {
                    rTrack.addTimeTrialFinish(new TimeTrialFinish(resultFinishes));
                }
                resultFinishes.close();
            }

            result = statement.executeQuery("SELECT * FROM `tracksRegions` WHERE `isRemoved` = 0;");

            while (result.next())
            {
                Optional<Track> maybeTrack = getTrackById(result.getInt("trackId"));
                if (maybeTrack.isPresent())
                {
                    var rTrack = maybeTrack.get();
                    TrackRegion trackRegion = new TrackRegion(result);
                    regions.add(trackRegion);
                    if (trackRegion.getRegionType().equals(TrackRegion.RegionType.START))
                    {
                        rTrack.newStartRegion(trackRegion);
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

            statement.close();
            result.close();
            connection.close();

        } catch (SQLException exception)
        {
            plugin.getLogger().warning("Failed to synchronize database: " + exception.getMessage());
        }
    }

    static Track trackNew(String name, UUID uuid, Location location, Track.TrackType type, ItemStack gui)
    {
        try
        {
            Connection connection = ApiDatabase.getConnection();
            Statement statement = connection.createStatement();

            long date = ApiUtilities.getTimestamp();

            Location leaderboard = location.clone();
            leaderboard.setY(leaderboard.getY() + 3);
            // Save the track
            statement.executeUpdate("INSERT INTO `tracks` (`uuid`, `name`, `dateCreated`, `guiItem`, `spawn`, `leaderboard`, `type`, `toggleOpen`, `toggleGovernment`, `options`, `isRemoved`) VALUES('" + uuid + "', " + ApiDatabase.sqlString(name) + ", " + date + ", " + ApiDatabase.sqlString(ApiUtilities.itemToString(gui)) + ", '" + ApiUtilities.locationToString(location) + "', '" + ApiUtilities.locationToString(leaderboard) + "', " + ApiDatabase.sqlString(type == null ? null : type.toString()) + ", 0, 0, NULL , 0);", Statement.RETURN_GENERATED_KEYS);
            ResultSet keys = statement.getGeneratedKeys();

            keys.next();
            int trackId = keys.getInt(1);

            ResultSet result = statement.executeQuery("SELECT * FROM `tracks` WHERE `id` = " + trackId + ";");
            result.next();

            Track rTrack = new Track(result);
            tracks.add(rTrack);

            statement.executeUpdate("INSERT INTO `tracksRegions` (`trackId`, `regionIndex`, `regionType`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + ", 0, " + ApiDatabase.sqlString(TrackRegion.RegionType.START.toString()) + ", NULL, NULL, '" + ApiUtilities.locationToString(location) + "', 0);", Statement.RETURN_GENERATED_KEYS);
            keys = statement.getGeneratedKeys();

            keys.next();
            int regionId = keys.getInt(1);
            result = statement.executeQuery("SELECT * FROM `tracksRegions` WHERE `id` = " + regionId + ";");
            result.next();

            TrackRegion startRegion = new TrackRegion(result);
            rTrack.newStartRegion(startRegion);
            regions.add(startRegion);

            statement.executeUpdate("INSERT INTO `tracksRegions` (`trackId`, `regionIndex`, `regionType`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + ", 0, " + ApiDatabase.sqlString(TrackRegion.RegionType.END.toString()) + ", NULL, NULL, '" + ApiUtilities.locationToString(location) + "', 0);", Statement.RETURN_GENERATED_KEYS);
            keys = statement.getGeneratedKeys();

            keys.next();
            regionId = keys.getInt(1);
            result = statement.executeQuery("SELECT * FROM `tracksRegions` WHERE `id` = " + regionId + ";");
            result.next();

            TrackRegion endRegion = new TrackRegion(result);
            rTrack.newEndRegion(endRegion);
            regions.add(endRegion);

            statement.executeUpdate("INSERT INTO `tracksRegions` (`trackId`, `regionIndex`, `regionType`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + ", 0, " + ApiDatabase.sqlString(TrackRegion.RegionType.PIT.toString()) + ", NULL, NULL, '" + ApiUtilities.locationToString(location) + "', 0);", Statement.RETURN_GENERATED_KEYS);
            keys = statement.getGeneratedKeys();

            keys.next();
            regionId = keys.getInt(1);
            result = statement.executeQuery("SELECT * FROM `tracksRegions` WHERE `id` = " + regionId + ";");
            result.next();

            TrackRegion pitRegion = new TrackRegion(result);
            rTrack.newPitRegion(pitRegion);
            regions.add(pitRegion);

            statement.close();
            result.close();
            connection.close();

            return rTrack;
        } catch (SQLException exception)
        {
            exception.printStackTrace();
            return null;
        }
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
            return TrackDatabase.getTracks().stream().filter(Track::isOpen).toList();
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

    static boolean trackNameAvailable(String name)
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

    static public void removeTrack(Track Track)
    {
        ApiDatabase.asynchronousQuery(new String[]{
                "UPDATE `tracksRegions` SET `isRemoved` = 1 WHERE `trackId` = " + Track.getId() + ";",
                "UPDATE `tracksFinishes` SET `isRemoved` = 1 WHERE `trackId` = " + Track.getId() + ";",
                "UPDATE `tracks` SET `isRemoved` = 1 WHERE `id` = " + Track.getId() + ";"
        });

        regions.removeIf(trackRegion -> trackRegion.getTrackId() == Track.getId());
        tracks.remove(Track);
        LeaderboardManager.removeLeaderboard(Track.getId());
    }
}
