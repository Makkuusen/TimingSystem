package me.makkuusen.timing.system.database;

import co.aikar.idb.*;
import com.sk89q.worldedit.math.BlockVector2;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Subscriber;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackLocation;
import me.makkuusen.timing.system.track.TrackRegion;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static me.makkuusen.timing.system.TimingSystem.getPlugin;

public class SQLiteDatabase implements TSDatabase, EventDatabase, TrackDatabase {
    @Override
    public boolean initialize() {
        DatabaseOptions options = DatabaseOptions.builder().poolName(getPlugin().getDescription().getName() + " DB").logger(getPlugin().getLogger()).sqlite(new File(getPlugin().getDataFolder(), "ts.db").getPath()).build();
        PooledDatabaseOptions poolOptions = PooledDatabaseOptions.builder().options(options).build();
        BukkitDB.createHikariDatabase(TimingSystem.getPlugin(), poolOptions);
        return createTables();
    }

    @Override
    public boolean update() {
        return false;
    }

    @Override
    public void synchronize() {
        try {
            // Load players;
            var result = DB.getResults("SELECT * FROM `ts_players`;");

            for (DbRow row : result) {
                TPlayer player = new TPlayer(getPlugin(), row);
                TimingSystem.players.put(player.getUniqueId(), player);
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                TPlayer TPlayer = TSDatabase.getPlayer(player.getUniqueId());

                TPlayer.setPlayer(player);

                if (!TPlayer.getName().equals(player.getName())) {
                    // Update name
                    TPlayer.setName(player.getName());
                }
            }

            TrackDatabase.initDatabaseSynchronize();
        } catch (Exception exception) {
            exception.printStackTrace();
            getPlugin().getLogger().warning("Failed to synchronize database, disabling plugin.");
            getPlugin().getServer().getPluginManager().disablePlugin(getPlugin());
        }
    }

    @Override
    public boolean createTables() {
        try {
            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_players` (
                        `uuid` TEXT PRIMARY KEY NOT NULL DEFAULT '',
                        `name` TEXT NOT NULL,
                        `boat` TEXT DEFAULT NULL,
                        `color` TEXT NOT NULL DEFAULT '#9D9D97',
                        `chestBoat` INTEGER NOT NULL DEFAULT 0,
                        `compactScoreboard` INTEGER NOT NULL DEFAULT 0,
                        `override` INTEGER NOT NULL DEFAULT 0,
                        `verbose` INTEGER NOT NULL DEFAULT 0,
                        `toggleSound` INTEGER DEFAULT 1 NOT NULL
                        )
                        """);

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_tracks` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `uuid` TEXT DEFAULT NULL,
                          `name` TEXT NOT NULL,
                          `dateCreated` INTEGER DEFAULT NULL,
                          `dateChanged` INTEGER DEFAULT NULL,
                          `weight` INTEGER NOT NULL DEFAULT 100,
                          `guiItem` TEXT NOT NULL,
                          `spawn` TEXT NOT NULL,
                          `leaderboard` TEXT NOT NULL,
                          `type` TEXT NOT NULL,
                          `mode` TEXT NOT NULL,
                          `toggleOpen` INTEGER NOT NULL,
                          `options` TEXT DEFAULT NULL,
                          `boatUtilsMode` TEXT NOT NULL DEFAULT 'VANILLA',
                          `isRemoved` INTEGER NOT NULL
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_finishes` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `trackId` INT NOT NULL,
                          `uuid` TEXT NOT NULL,
                          `date` INTEGER NOT NULL,
                          `time` INTEGER NOT NULL,
                          `isRemoved` INT NOT NULL
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_finishes_checkpoints` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `finishId` INTEGER NOT NULL,
                          `checkpointIndex` INTEGER DEFAULT NULL,
                          `time` INTEGER NOT NULL,
                          `isRemoved` INTEGER NOT NULL DEFAULT 0
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_attempts` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `trackId` INTEGER NOT NULL,
                          `uuid` TEXT NOT NULL,
                          `date` INTEGER NOT NULL,
                          `time` INTEGER NOT NULL
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_regions` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `trackId` INTEGER NOT NULL,
                          `regionIndex` INTEGER DEFAULT NULL,
                          `regionType` TEXT DEFAULT NULL,
                          `regionShape` TEXT NOT NULL,
                          `minP` TEXT DEFAULT NULL,
                          `maxP` TEXT DEFAULT NULL,
                          `spawn` TEXT NOT NULL,
                          `isRemoved` INTEGER NOT NULL
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_events` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `name` TEXT NOT NULL,
                          `uuid` TEXT NOT NULL,
                          `date` INTEGER DEFAULT NULL,
                          `track` INTEGER DEFAULT NULL,
                          `state` TEXT NOT NULL,
                          `open` INTEGER NOT NULL DEFAULT 1,
                          `isRemoved` INTEGER NOT NULL DEFAULT '0'
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_heats` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `roundId` INTEGER NOT NULL,
                          `heatNumber` INTEGER NOT NULL,
                          `state` TEXT NOT NULL,
                          `startTime` INTEGER DEFAULT NULL,
                          `endTime` INTEGER DEFAULT NULL,
                          `fastestLapUUID` TEXT NULL,
                          `totalLaps` INTEGER DEFAULT NULL,
                          `totalPitstops` INT DEFAULT NULL,
                          `timeLimit` INTEGER DEFAULT NULL,
                          `startDelay` INTEGER DEFAULT NULL,
                          `maxDrivers` INTEGER DEFAULT NULL,
                          `isRemoved` INTEGER NOT NULL DEFAULT '0'
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_drivers` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `uuid` TEXT NOT NULL,
                          `heatId` INTEGER NOT NULL,
                          `position` INTEGER NOT NULL,
                          `startPosition` INTEGER NOT NULL,
                          `startTime` INTEGER DEFAULT NULL,
                          `endTime` INTEGER DEFAULT NULL,
                          `pitstops` INTEGER DEFAULT NULL,
                          `isRemoved` INTEGER NOT NULL DEFAULT '0'
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_events_signs`(
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `eventId` INTEGER NOT NULL,
                          `uuid` TEXT NOT NULL,
                          `type` TEXT NOT NULL
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_laps` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `uuid` TEXT NOT NULL,
                          `heatId` INTEGER NOT NULL,
                          `trackId` INTEGER NOT NULL,
                          `lapStart` TEXT DEFAULT NULL,
                          `lapEnd` INTEGER DEFAULT NULL,
                          `pitted` INTEGER NOT NULL,
                          `isRemoved` INTEGER NOT NULL DEFAULT '0'
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_locations` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `trackId` INTEGER NOT NULL,
                          `type` TEXT NOT NULL,
                          `index` INTEGER DEFAULT NULL,
                          `location` TEXT NOT NULL
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_points` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `regionId` INTEGER NOT NULL,
                          `x` INTEGER DEFAULT NULL,
                          `z` INTEGER DEFAULT NULL
                        );""");


            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_rounds` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `eventId` INTEGER NOT NULL,
                          `roundIndex` INTEGER NOT NULL DEFAULT 1,
                          `type` TEXT DEFAULT NULL,
                          `state` TEXT NOT NULL,
                          `isRemoved` INTEGER NOT NULL DEFAULT 0
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_version` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `version` TEXT NOT NULL,
                          `date` INTEGER NOT NULL
                        );""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_tags` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `tag` TEXT NOT NULL,
                          `color` TEXT NOT NULL DEFAULT '#FFFFFF',
                          `item` TEXT NOT NULL DEFAULT '%ITEM%;',
                          `weight` INT NOT NULL DEFAULT 100
                        );""".replace("%ITEM%", ApiUtilities.itemToString(new ItemBuilder(Material.ANVIL).build()))
            );

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_tracks_tags` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `trackId` INTEGER NOT NULL,
                          `tag` TEXT NOT NULL
                        );""");

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<DbRow> selectPlayers() throws SQLException {
        return null;
    }

    @Override
    public TPlayer createPlayer(UUID uuid, String name) {
        try {
            DB.executeUpdate("INSERT INTO `ts_players` (`uuid`, `name`, `boat`) VALUES('" + uuid + "', " + TSDatabase.sqlStringOf(name) + ", '" + Boat.Type.BIRCH.name() + "');");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_players` WHERE `uuid` = '" + uuid + "';");
            return new TPlayer(getPlugin(), dbRow);
        } catch (SQLException e) {
            getPlugin().getLogger().warning("Failed to create new player: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void playerUpdateValue(UUID uuid, String column, String value) {
        DB.executeUpdateAsync("UPDATE `ts_players` SET `" + column + "` = " + TSDatabase.sqlStringOf(value) + " WHERE `uuid` = '" + uuid + "';");
    }

    // EVENT DATABASE
    private boolean eventDatabaseFinishedLoading = false;

    @Override
    public Event createEvent(UUID uuid, String name) {
        try {
            var eventId = DB.executeInsert("INSERT INTO `ts_events`(`name`,`uuid`,`date`,`track`,`state`,`isRemoved`) " +
                    "VALUES (" +
                    "'" + name + "'," +
                    "'" + uuid + "'," +
                    ApiUtilities.getTimestamp() + "," +
                    "NULL," +
                    "'" + Event.EventState.SETUP.name() + "'," +
                    "0)");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_events` WHERE `id` = " + eventId + ";");
            return new Event(dbRow);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Round createRound(Event event, RoundType roundType, int roundIndex) {
        try {
            var roundId = DB.executeInsert("INSERT INTO `ts_rounds`(`eventId`, `roundIndex`, `type`, `state`, `isRemoved`) " +
                    "VALUES (" +
                    event.getId() + ", " +
                    roundIndex + ", " +
                    "'" + roundType.name() + "'," +
                    "'" + Round.RoundState.SETUP.name() + "'," +
                    "0)");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_rounds` WHERE `id` = " + roundId + ";");
            if (roundType == RoundType.QUALIFICATION)
                return new QualificationRound(dbRow);
            return new FinalRound(dbRow);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Heat createHeat(Round round, int heatIndex) {
        try {
            var heatId = DB.executeInsert("INSERT INTO `ts_heats`(`roundId`, `heatNumber`, `state`, `startTime`, `endTime`, `fastestLapUUID`, `totalLaps`, `totalPitstops`, `timeLimit`, `startDelay`, `maxDrivers`, `isRemoved`) " +
                    "VALUES (" +
                    round.getId() + "," +
                    heatIndex + "," +
                    "'" + HeatState.SETUP.name() + "'," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "0)");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_heats` WHERE `id` = " + heatId + ";");
            return new Heat(dbRow, round);
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @Override
    public void createSign(TPlayer tPlayer, Event event, Subscriber.Type type) {
        DB.executeUpdateAsync("INSERT INTO `ts_events_signs`(" +
                "`eventId`, " +
                "`uuid`, " +
                "`type`) " +
                "VALUES (" +
                event.getId() + ", " +
                "'" + tPlayer.getUniqueId() + "'," +
                "'" + type.name() + "')");
    }

    @Override
    public void removeSign(UUID uuid, int eventId, Subscriber.Type type) {
        DB.executeUpdateAsync("DELETE FROM `ts_events_signs` WHERE `uuid` = '" + uuid.toString() + "' AND `eventId` = " + eventId + " AND `type` = '" + type.name() + "';");
    }

    @Override
    public DbRow createDriver(UUID uuid, Heat heat, int startPosition) {
        try {
            var driverId = DB.executeInsert("INSERT INTO `ts_drivers`(" +
                    "`uuid`, " +
                    "`heatId`, " +
                    "`position`, " +
                    "`startPosition`, " +
                    "`startTime`, " +
                    "`endTime`, " +
                    "`pitstops`) " +
                    "VALUES (" +
                    "'" + uuid + "'," +
                    heat.getId() + "," +
                    startPosition + "," +
                    startPosition + "," +
                    "NULL," +
                    "NULL," +
                    "0)");
            return DB.getFirstRow("SELECT * FROM `ts_drivers` WHERE `id` = " + driverId + ";");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void createLap(Lap lap) {
        String lapEnd = lap.getLapEnd() == null ? "NULL" : String.valueOf(lap.getLapEnd().toEpochMilli());
        DB.executeUpdateAsync("INSERT INTO `ts_laps`(`uuid`, `heatId`, `trackId`, `lapStart`, `lapEnd`, `pitted`) " +
                "VALUES (" +
                "'" + lap.getPlayer().getUniqueId() + "'," +
                lap.getHeatId() + "," +
                lap.getTrack().getId() + "," +
                lap.getLapStart().toEpochMilli() + "," +
                lapEnd + "," +
                lap.isPitted() + ")"
        );
    }

    @Override
    public List<DbRow> selectEvents() throws SQLException {
        return DB.getResults("SELECT * FROM `ts_events` WHERE `isRemoved` = 0;");
    }

    @Override
    public List<DbRow> selectRounds(int eventId) throws SQLException {
        return DB.getResults("SELECT * FROM `ts_rounds` WHERE `eventId` = " + eventId + " AND `isRemoved` = 0;");
    }

    @Override
    public List<DbRow> selectHeats(int roundId) throws SQLException {
        return DB.getResults("SELECT * FROM `ts_heats` WHERE `roundId` = " + roundId + " AND `isRemoved` = 0;");
    }

    @Override
    public List<DbRow> selectSigns(int eventId) throws SQLException {
        return DB.getResults("SELECT * FROM `ts_events_signs` WHERE `eventId` = " + eventId + ";");
    }

    @Override
    public List<DbRow> selectDrivers(int heatId) throws SQLException {
        return DB.getResults("SELECT * FROM `ts_drivers` WHERE `heatId` = " + heatId + " AND `isRemoved` = 0;");
    }

    @Override
    public List<DbRow> selectLaps(int heatId, String uuid) throws SQLException {
        return DB.getResults("SELECT * FROM `ts_laps` WHERE `heatId` = " + heatId + " AND `uuid` = '" + uuid + "' AND `isRemoved` = 0;");
    }

    @Override
    public <T> void remove(T thing) {
        if (thing instanceof Event event)
            DB.executeUpdateAsync("UPDATE `ts_events` SET `isRemoved` = 1 WHERE `id` = " + event.getId() + ";");
        else if (thing instanceof Round round)
            DB.executeUpdateAsync("UPDATE `ts_rounds` SET `isRemoved` = 1 WHERE `id` = " + round.getId() + ";");
        else if (thing instanceof Heat heat)
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `isRemoved` = 1 WHERE `id` = " + heat.getId() + ";");
    }

    @Override
    public void setHasFinishedLoading(boolean b) {
        eventDatabaseFinishedLoading = b;
    }

    @Override
    public boolean hasFinishedLoading() {
        return eventDatabaseFinishedLoading;
    }

    @Override
    public void eventSet(long eventId, String column, String value) {

    }

    @Override
    public void roundSet(long roundId, String column, String value) {

    }

    @Override
    public void heatSet(long heatId, String column, String value) {
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `" + column + "` = '" + value + "' WHERE `id` = " + heatId + ";");
    }

    @Override
    public void driverSet(long driverId, String column, String value) {

    }

    // Track Database

    @Override
    public List<DbRow> selectTracks() throws SQLException {
        return DB.getResults("SELECT * FROM `ts_tracks` WHERE `isRemoved` = 0;");
    }

    @Override
    public DbRow selectTrack(long trackId) throws SQLException {
        return DB.getFirstRow("SELECT * FROM `ts_tracks` WHERE `id` = " + trackId + ";");
    }

    @Override
    public List<DbRow> selectFinishes() throws SQLException {
        return DB.getResults("SELECT * FROM `ts_finishes` WHERE `isRemoved` = 0;");
    }

    @Override
    public List<DbRow> selectAttempts() throws SQLException {
        return DB.getResults("SELECT * FROM `ts_attempts`;");
    }

    @Override
    public List<DbRow> selectTrackTags() throws SQLException {
        return DB.getResults("SELECT * FROM `ts_tracks_tags`;");
    }

    @Override
    public List<DbRow> selectTags() throws SQLException {
        return DB.getResults("SELECT * FROM `ts_tags`");
    }

    @Override
    public List<DbRow> selectCheckpointTimes() throws SQLException {
        return DB.getResults("SELECT * FROM `ts_finishes_checkpoints` WHERE `isRemoved` = 0;");
    }

    @Override
    public List<DbRow> selectTrackRegions() throws SQLException {
        return DB.getResults("SELECT * FROM `ts_regions` WHERE `isRemoved` = 0;");
    }

    @Override
    public DbRow selectTrackRegion(long regionId) throws SQLException {
        return DB.getFirstRow("SELECT * FROM `ts_regions` WHERE `id` = " + regionId + ";");
    }

    @Override
    public List<DbRow> selectRegionPoints(int regionId) throws SQLException {
        return DB.getResults("SELECT * FROM `ts_points` WHERE `regionId` = " + regionId + ";");
    }

    @Override
    public List<DbRow> selectLocations() throws SQLException {
        return DB.getResults("SELECT * FROM `ts_locations`");
    }

    @Override
    public long createTrack(String uuid, String name, long date, int weight, ItemStack gui, Location location, Location leaderboard, Track.TrackMode mode, Track.TrackType type, BoatUtilsMode boatUtilsMode) throws SQLException {
        return DB.executeInsert("INSERT INTO `ts_tracks` (`uuid`, `name`, `dateCreated`, `weight`, `guiItem`, `spawn`, `leaderboard`, `type`, `mode`, `toggleOpen`, `options`, `boatUtilsMode`, `isRemoved`) " +
                "VALUES('" + uuid + "', " + TSDatabase.sqlStringOf(name) + ", " + date + ", " + weight + ", " + TSDatabase.sqlStringOf(ApiUtilities.itemToString(gui)) + ", '" + ApiUtilities.locationToString(location) + "', '" + ApiUtilities.locationToString(leaderboard) + "', " + TSDatabase.sqlStringOf(type == null ? null : type.toString()) + "," + TSDatabase.sqlStringOf(mode.toString()) + ", 0, NULL, " + boatUtilsMode.getId() + ", 0);");
    }

    @Override
    public long createRegion(long trackId, int index, String minP, String maxP, TrackRegion.RegionType type, TrackRegion.RegionShape shape, Location location) throws SQLException {
        return DB.executeInsert("INSERT INTO `ts_regions` (`trackId`, `regionIndex`, `regionType`, `regionShape`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + trackId + ", " + index + ", " + TSDatabase.sqlStringOf(type.toString()) + ", " + TSDatabase.sqlStringOf(TrackRegion.RegionShape.POLY.toString()) + ", '" + minP + "', '" + maxP + "','" + ApiUtilities.locationToString(location) + "', 0);");
    }

    @Override
    public long createPoint(long regionId, BlockVector2 v) throws SQLException {
        return DB.executeInsert("INSERT INTO `ts_points` (`regionId`, `x`, `z`) VALUES(" + regionId + ", " + v.getBlockX() + ", " + v.getBlockZ() + ");");
    }

    @Override
    public long createLocation(long trackId, int index, TrackLocation.Type type, Location location) throws SQLException {
        return DB.executeInsert("INSERT INTO `ts_locations` (`trackId`, `index`, `type`, `location`) VALUES(" + trackId + ", " + index + ", '" + type.name() + "', '" + ApiUtilities.locationToString(location) + "');");
    }

    @Override
    public void removeTrack(long trackId) {
        DB.executeUpdateAsync("UPDATE `ts_regions` SET `isRemoved` = 1 WHERE `trackId` = " + trackId + ";");
        DB.executeUpdateAsync("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `trackId` = " + trackId + ";");
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `isRemoved` = 1 WHERE `id` = " + trackId + ";");
    }

    @Override
    public void createTagAsync(TrackTag tag, TextColor color, ItemStack item) {
        DB.executeUpdateAsync("INSERT INTO `ts_tags` (`tag`, `color`, `item`) VALUES('" + tag.getValue() + "', '" + color.asHexString() + "', " + TSDatabase.sqlStringOf(ApiUtilities.itemToString(item)) + ");");
    }

    @Override
    public void deleteTagAsync(TrackTag tag) {
        DB.executeUpdateAsync("DELETE FROM `ts_tags` WHERE `tag` = '" + tag.getValue() + "';");
        DB.executeUpdateAsync("DELETE FROM `ts_tracks_tags` WHERE `tag` = '" + tag.getValue() + "';");
    }

    @Override
    public void deletePoint(long regionId) {
        DB.executeUpdateAsync("DELETE FROM `ts_points` WHERE `regionId` = " + regionId + ";");
    }

    @Override
    public void deleteLocation(int trackId, int index, TrackLocation.Type type) {
        DB.executeUpdateAsync("DELETE FROM `ts_locations` WHERE `trackId` = " + trackId + " AND `index` = " + index + " AND `type` = '" + type + "';");
    }

    @Override
    public void updateLocation(int index, Location location, TrackLocation.Type type, long trackId) {
        DB.executeUpdateAsync("UPDATE `ts_locations` SET `location` = '" + ApiUtilities.locationToString(location) + "' WHERE `trackId` = " + trackId + " AND `index` = " + index + " AND `type` = '" + type + "';");
    }

    @Override
    public void addTagToTrack(int trackId, TrackTag tag) {
        DB.executeUpdateAsync("INSERT INTO `ts_tracks_tags` (`trackId`, `tag`) VALUES(" + trackId + ", '" + tag.getValue() + "');");
    }

    @Override
    public void tagSet(String tag, String column, String value) {
        DB.executeUpdateAsync("UPDATE `ts_tags` SET `" + column + "` = " + TSDatabase.sqlStringOf(value) + " WHERE `tag` = '" + tag + "';");
    }

    @Override
    public void removeFinish(int finishId) {
        DB.executeUpdateAsync("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `id` = " + finishId + ";");
    }

    @Override
    public void removeAllFinishes(int trackId, UUID uuid) {
        DB.executeUpdateAsync("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `trackId` = " + trackId + " AND `uuid` = '" + uuid + "';");
    }

    @Override
    public void removeAllFinishes(int trackId) {
        DB.executeUpdateAsync("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `trackId` = " + trackId + ";");
    }

    @Override
    public void removeTagFromTrack(int trackId, TrackTag tag) {
        DB.executeUpdateAsync("DELETE FROM `ts_tracks_tags` WHERE `tag` = '" + tag.getValue() + "' AND `trackId` = " + trackId + ";");
    }

    @Override
    public void trackSet(int trackId, String column, String value) {
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `" + column + "` = '" + TSDatabase.sqlStringOf(value) + "' WHERE `id` = " + trackId + ";");
    }

    @Override
    public void trackRegionSet(int trackId, String column, String value) {
        DB.executeUpdateAsync("UPDATE `ts_regions` SET `" + column + "` = '" + TSDatabase.sqlStringOf(value) + "' WHERE `id` = " + trackId + ";");
    }

    @Override
    public void createCheckpointFinish(long finishId, int checkpointIndex, long time) {
        DB.executeUpdateAsync("INSERT INTO `ts_finishes_checkpoints`(" +
                "`finishId`, " +
                "`checkpointIndex`, " +
                "`time`) " +
                "VALUES (" +
                finishId + "," +
                checkpointIndex + "," +
                time + ");"
        );
    }

    @Override
    public void createAttempt(int id, UUID uuid, long date, long time) {
        DB.executeUpdateAsync("INSERT INTO `ts_attempts` (`trackId`, `uuid`, `date`, `time`) VALUES(" + id + ", '" + uuid + "', " + date + ", " + time + ");");
    }
}
