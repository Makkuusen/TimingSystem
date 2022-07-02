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

public class RaceDatabase
{
    public static Race plugin;
    private static final List<RaceTrack> tracks = new ArrayList<>();
    private static final List<RaceRegion> regions = new ArrayList<>();

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
                RaceTrack rTrack = new RaceTrack(result);
                tracks.add(rTrack);

                Statement statementFinishes = connection.createStatement();
                ResultSet resultFinishes = statementFinishes.executeQuery("SELECT * FROM `tracksFinishes` WHERE (`uuid`,`time`) IN (SELECT `uuid`, min(`time`) FROM `tracksFinishes` WHERE `trackId` = " + rTrack.getId() + " AND `isRemoved` = 0 GROUP BY `uuid`) AND `isRemoved` = 0 ORDER BY `time`;");
                while (resultFinishes.next())
                {
                    rTrack.addRaceFinish(new RaceFinish(resultFinishes));
                }
                resultFinishes.close();
            }

            result = statement.executeQuery("SELECT * FROM `tracksRegions` WHERE `isRemoved` = 0;");

            while (result.next())
            {
                Optional<RaceTrack> maybeTrack = getTrackById(result.getInt("trackId"));
                if (maybeTrack.isPresent())
                {
                    var rTrack = maybeTrack.get();
                    RaceRegion raceRegion = new RaceRegion(result);
                    regions.add(raceRegion);
                    if (raceRegion.getRegionType().equals(RaceRegion.RegionType.START))
                    {
                        rTrack.newStartRegion(raceRegion);
                    }
                    else if (raceRegion.getRegionType().equals(RaceRegion.RegionType.END))
                    {
                        rTrack.newEndRegion(raceRegion);
                    }
                    else if (raceRegion.getRegionType().equals(RaceRegion.RegionType.CHECKPOINT))
                    {
                        rTrack.addCheckpoint(raceRegion);
                    }
                    else if (raceRegion.getRegionType().equals(RaceRegion.RegionType.RESET))
                    {
                        rTrack.addResetRegion(raceRegion);
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

    static RaceTrack trackNew(String name, UUID uuid, Location location, RaceTrack.TrackType type, ItemStack gui)
    {
        try
        {
            Connection connection = ApiDatabase.getConnection();
            Statement statement = connection.createStatement();

            long date = ApiUtilities.getTimestamp();

            Location leaderboard = location.clone();
            leaderboard.setY(leaderboard.getY() + 3);
            // Save the track
            statement.executeUpdate("INSERT INTO `tracks` (`uuid`, `name`, `dateCreated`, `guiItem`, `spawn`, `leaderboard`, `type`, `toggleOpen`, `toggleGovernment`, `options`, `isRemoved`) VALUES('" + uuid + "', " + ApiDatabase.sqlString(name) + ", " + date + ", " + ApiDatabase.sqlString(RaceUtilities.itemToString(gui)) + ", '" + ApiUtilities.locationToString(location) + "', '" + ApiUtilities.locationToString(leaderboard) + "', " + ApiDatabase.sqlString(type == null ? null : type.toString()) + ", 0, 0, NULL , 0);", Statement.RETURN_GENERATED_KEYS);
            ResultSet keys = statement.getGeneratedKeys();

            keys.next();
            int trackId = keys.getInt(1);

            ResultSet result = statement.executeQuery("SELECT * FROM `tracks` WHERE `id` = " + trackId + ";");
            result.next();

            RaceTrack rTrack = new RaceTrack(result);
            tracks.add(rTrack);

            statement.executeUpdate("INSERT INTO `tracksRegions` (`trackId`, `regionIndex`, `regionType`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + ", 0, " + ApiDatabase.sqlString(RaceRegion.RegionType.START.toString()) + ", NULL, NULL, '" + ApiUtilities.locationToString(location) + "', 0);", Statement.RETURN_GENERATED_KEYS);
            keys = statement.getGeneratedKeys();

            keys.next();
            int regionId = keys.getInt(1);
            result = statement.executeQuery("SELECT * FROM `tracksRegions` WHERE `id` = " + regionId + ";");
            result.next();

            RaceRegion startRegion = new RaceRegion(result);
            rTrack.newStartRegion(startRegion);
            regions.add(startRegion);

            statement.executeUpdate("INSERT INTO `tracksRegions` (`trackId`, `regionIndex`, `regionType`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + ", 0, " + ApiDatabase.sqlString(RaceRegion.RegionType.END.toString()) + ", NULL, NULL, '" + ApiUtilities.locationToString(location) + "', 0);", Statement.RETURN_GENERATED_KEYS);
            keys = statement.getGeneratedKeys();

            keys.next();
            regionId = keys.getInt(1);
            result = statement.executeQuery("SELECT * FROM `tracksRegions` WHERE `id` = " + regionId + ";");
            result.next();

            RaceRegion endRegion = new RaceRegion(result);
            rTrack.newEndRegion(endRegion);
            regions.add(endRegion);

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

    static public Optional<RaceTrack> getRaceTrack(String name)
    {
        for (RaceTrack t : tracks)
        {
            if (t.getName().equalsIgnoreCase(name))
            {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    static public Optional<RaceTrack> getTrackById(int id)
    {
        for (RaceTrack t : tracks)
        {
            if (t.getId() == id)
            {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    static public List<RaceTrack> getRaceTracks()
    {
        return tracks;
    }

    static public List<RaceTrack> getAvailableRaceTracks(Player player)
    {
        if (!player.hasPermission("track.admin") && !player.isOp())
        {
            return RaceDatabase.getRaceTracks().stream().filter(RaceTrack::isOpen).toList();
        }

        return getRaceTracks();
    }

    static public List<RaceRegion> getRaceRegions()
    {
        return regions;
    }
    static public List<RaceRegion> getRaceStartRegions()
    {
        return regions.stream().filter(r -> r.getRegionType().equals(RaceRegion.RegionType.START)).collect(Collectors.toList());
    }

    static boolean trackNameAvailable(String name)
    {

        for (RaceTrack rTrack : tracks)
        {
            if (rTrack.getName().equalsIgnoreCase(name))
            {
                return false;
            }
        }
        return true;
    }

    static public void addRaceRegion(RaceRegion region)
    {
        regions.add(region);
    }

    static public void removeRaceRegion(RaceRegion region)
    {
        regions.remove(region);
    }

    static public void removeRaceTrack(RaceTrack raceTrack)
    {
        ApiDatabase.asynchronousQuery(new String[]{
                "UPDATE `tracksRegions` SET `isRemoved` = 1 WHERE `trackId` = " + raceTrack.getId() + ";",
                "UPDATE `tracksFinishes` SET `isRemoved` = 1 WHERE `trackId` = " + raceTrack.getId() + ";",
                "UPDATE `tracks` SET `isRemoved` = 1 WHERE `id` = " + raceTrack.getId() + ";"
        });

        regions.removeIf(raceRegion -> raceRegion.getTrackId() == raceTrack.getId());
        tracks.remove(raceTrack);
        LeaderboardManager.removeLeaderboard(raceTrack.getId());
    }
}
