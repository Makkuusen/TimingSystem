package me.makkuusen.timing.system.database;

import co.aikar.idb.BukkitDB;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import co.aikar.idb.PooledDatabaseOptions;
import com.sk89q.worldedit.math.BlockVector2;
import me.makkuusen.timing.system.*;
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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static me.makkuusen.timing.system.TimingSystem.getPlugin;

public class MySQLDatabase implements TSDatabase, EventDatabase, TrackDatabase {
    @Override
    public boolean initialize() {
        TimingSystemConfiguration config = TimingSystem.configuration;
        String hostAndPort = config.getSqlHost() + ":" + config.getSqlPort();

        PooledDatabaseOptions options = BukkitDB.getRecommendedOptions(TimingSystem.getPlugin(), config.getSqlUsername(), config.getSqlPassword(), config.getSqlDatabase(), hostAndPort);

        BukkitDB.createHikariDatabase(TimingSystem.getPlugin(), options);
        return createTables();
    }

    @Override
    public boolean update() {
        try {
            var row = DB.getFirstRow("SELECT * FROM `ts_version` ORDER BY `date` DESC;");
            if (row == null) {
                DB.executeInsert("INSERT INTO `ts_version` (`version`, `date`) VALUES('" + getPlugin().getPluginMeta().getVersion() + "', " + ApiUtilities.getTimestamp() + ");");
                rc9Update();
                v1_0Update();
                v1_2Update();
                v1_3Update();
                v1_6Update();
                v1_8Update();
                v1_9Update();
            } else {
                if (TSDatabase.isNewerVersion(row.getString("version"), getPlugin().getPluginMeta().getVersion())) {
                    updateDatabase(row.getString("version"));
                    getPlugin().getLogger().warning("UPDATING DATABASE FROM " + row.getString("version") + " to " + getPlugin().getPluginMeta().getVersion());
                    DB.executeInsert("INSERT INTO `ts_version` (`version`, `date`) VALUES('" + getPlugin().getPluginMeta().getVersion() + "', " + ApiUtilities.getTimestamp() + ");");
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            getPlugin().getLogger().warning("Failed to update database, disabling plugin.");
            getPlugin().getServer().getPluginManager().disablePlugin(getPlugin());
            return false;
        }
    }

    @Override
    public boolean createTables() {
        try {
            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_players` (
                      `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
                      `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `boat` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                      `toggleSound` tinyint(1) DEFAULT 1 NOT NULL,
                      PRIMARY KEY (`uuid`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_tracks` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                      `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `dateCreated` bigint(30) DEFAULT NULL,
                      `guiItem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `spawn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `leaderboard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `mode` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `toggleOpen` tinyint(1) NOT NULL,
                      `options` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                      `isRemoved` tinyint(1) NOT NULL,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_finishes` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `trackId` int(11) NOT NULL,
                      `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `date` bigint(30) NOT NULL,
                      `time` int(11) NOT NULL,
                      `isRemoved` tinyint(1) NOT NULL,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_finishes_checkpoints` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `finishId` int(11) NOT NULL,
                      `checkpointIndex` int(11) DEFAULT NULL,
                      `time` int(11) NOT NULL,
                      `isRemoved` tinyint(1) NOT NULL DEFAULT 0,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_attempts` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `trackId` int(11) NOT NULL,
                      `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `date` bigint(30) NOT NULL,
                      `time` int(11) NOT NULL,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_regions` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `trackId` int(11) NOT NULL,
                      `regionIndex` int(11) DEFAULT NULL,
                      `regionType` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                      `regionShape` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `minP` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                      `maxP` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                      `spawn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `isRemoved` tinyint(1) NOT NULL,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_events` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `date` bigint(30) DEFAULT NULL,
                      `track` int(11) DEFAULT NULL,
                      `state` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `isRemoved` tinyint(1) NOT NULL DEFAULT '0',
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """);

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_heats` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `roundId` int(11) NOT NULL,
                      `heatNumber` int(11) NOT NULL,
                      `state` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `startTime` bigint(30) DEFAULT NULL,
                      `endTime` bigint(30) DEFAULT NULL,
                      `fastestLapUUID` varchar(255) COLLATE utf8mb4_unicode_ci NULL,
                      `totalLaps` int(11) DEFAULT NULL,
                      `totalPitstops` int(11) DEFAULT NULL,
                      `timeLimit` int(11) DEFAULT NULL,
                      `startDelay` int(11) DEFAULT NULL,
                      `maxDrivers` int(11) DEFAULT NULL,
                      `isRemoved` tinyint(1) NOT NULL DEFAULT '0',
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_drivers` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `heatId` int(11) NOT NULL,
                      `position` int(11) NOT NULL,
                      `startPosition` int(11) NOT NULL,
                      `startTime` bigint(30) DEFAULT NULL,
                      `endTime` bigint(30) DEFAULT NULL,
                      `pitstops` int(11) DEFAULT NULL,
                      `isRemoved` tinyint(1) NOT NULL DEFAULT '0',
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_events_signs`(
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `eventId` int(11) NOT NULL,
                      `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_laps` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `heatId` int(11) NOT NULL,
                      `trackId` int(11) NOT NULL,
                      `lapStart` bigint(30) DEFAULT NULL,
                      `lapEnd` bigint(30) DEFAULT NULL,
                      `pitted` tinyint(1) NOT NULL,
                      `isRemoved` tinyint(1) NOT NULL DEFAULT '0',
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_locations` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `trackId` int(11) NOT NULL,
                      `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `index` int(11) DEFAULT NULL,
                      `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;""");

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_points` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `regionId` int(11) NOT NULL,
                      `x` int(11) DEFAULT NULL,
                      `z` int(11) DEFAULT NULL,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;""");


            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_rounds` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `eventId` int(11) NOT NULL,
                      `roundIndex` int(11) NOT NULL DEFAULT 1,
                      `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                      `state` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `isRemoved` tinyint(1) NOT NULL DEFAULT 0,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """);

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_version` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `version` varchar(255) NOT NULL,
                      `date` bigint(30) NOT NULL,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """);

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_tags` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `tag` varchar(255) NOT NULL,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """);

            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_tracks_tags` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `trackId` int(11) NOT NULL,
                      `tag` varchar(255) NOT NULL,
                      PRIMARY KEY (`id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<DbRow> selectPlayers() throws SQLException {
        return DB.getResults("SELECT * FROM `ts_players`;");
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

    private static void updateDatabase(String oldVersion) {
        if (TSDatabase.isNewerVersion(oldVersion, "1.2")) {
            v1_2Update();
        }

        if (TSDatabase.isNewerVersion(oldVersion, "1.3")) {
            v1_3Update();
        }

        if (TSDatabase.isNewerVersion(oldVersion, "1.6")) {
            v1_6Update();
        }

        if (TSDatabase.isNewerVersion(oldVersion, "1.8")) {
            v1_8Update();
        }

        if (TSDatabase.isNewerVersion(oldVersion, "1.9")) {
            v1_9Update();
        }
    }

    private static void rc9Update() {
        try {
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `toggleSound` tinyint(1) NOT NULL DEFAULT '1' AFTER `boat`;");
        } catch (Exception ignored) {

        }
    }

    private static void v1_0Update() {
        try {
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `verbose` tinyint(1) NOT NULL DEFAULT '0' AFTER `boat`;");
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `timetrial` tinyint(1) NOT NULL DEFAULT '1' AFTER `boat`;");
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `override` tinyint(1) NOT NULL DEFAULT '0' AFTER `boat`;");
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `chestBoat` tinyint(1) NOT NULL DEFAULT '0' AFTER `boat`;");
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `color` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#9D9D97' AFTER `boat`;");
        } catch (Exception ignored) {

        }
    }

    private static void v1_2Update() {
        try {
            DB.executeUpdate("ALTER TABLE `ts_tracks` ADD `weight` int(11) NOT NULL DEFAULT '100' AFTER `dateCreated`;");
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `compactScoreboard` tinyint(1) NOT NULL DEFAULT '0' AFTER `chestBoat`;");
            var dbRows = DB.getResults("SELECT * FROM `ts_tracks`;");
            for (DbRow row : dbRows) {
                var first = DB.getFirstRow("SELECT * FROM `ts_locations` WHERE `trackId` = " + row.getInt("id") + " AND `type` = 'LEADERBOARD' AND `index` = 1;");
                if (first == null) {
                    DB.executeUpdate("INSERT INTO `ts_locations` (`trackId`, `index`, `type`, `location`) VALUES(" + row.getInt("id") + ", " + 1 + ", 'LEADERBOARD', '" + row.getString("leaderboard") + "');");
                }

            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void v1_3Update() {
        try {
            DB.executeUpdate("ALTER TABLE `ts_events` ADD `open` tinyint(1) NOT NULL DEFAULT '1' AFTER `state`;");
            DB.executeUpdate("UPDATE `ts_regions` SET `regionIndex` = 1 WHERE `regionType` = 'START' OR `regionType` = 'END' OR `regionType` = 'PIT';");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void v1_6Update() {
        try {
            DB.executeUpdate("ALTER TABLE `ts_tracks` ADD `dateChanged` bigint(30) DEFAULT NULL AFTER `dateCreated`;");
            DB.executeUpdate("ALTER TABLE `ts_tags` ADD `color` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#ffffff' AFTER `tag`;");
            DB.executeUpdate("ALTER TABLE `ts_tags` ADD `item` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '" + ApiUtilities.itemToString(new ItemBuilder(Material.ANVIL).build())+"' AFTER `color`;");
            DB.executeUpdate("ALTER TABLE `ts_tags` ADD `weight` int(11) NOT NULL DEFAULT '100' AFTER `item`;");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void v1_8Update() {
        // Because of an issue with servers not getting 1.6 update with a fresh installation, I make sure they are applied upon upgrade to 1.8.
        try {
            DB.executeUpdate("ALTER TABLE `ts_tracks` ADD `dateChanged` bigint(30) DEFAULT NULL AFTER `dateCreated`;");
            DB.executeUpdate("ALTER TABLE `ts_tags` ADD `color` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#ffffff' AFTER `tag`;");
            DB.executeUpdate("ALTER TABLE `ts_tags` ADD `item` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '" + ApiUtilities.itemToString(new ItemBuilder(Material.ANVIL).build())+"' AFTER `color`;");
            DB.executeUpdate("ALTER TABLE `ts_tags` ADD `weight` int(11) NOT NULL DEFAULT '100' AFTER `item`;");
        } catch (Exception ignored) {}

        try {
            DB.executeUpdate("ALTER TABLE `ts_tracks` ADD `boatUtilsMode` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VANILLA' AFTER `options`;");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = 'BA' WHERE `options` LIKE '%r%';");
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = 'VANILLA' WHERE `options` LIKE '%i%';");
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = 'RALLY' WHERE `options` LIKE '%i%' AND `options` LIKE '%r%';");
            DB.executeUpdate("UPDATE `ts_tracks` SET options=REPLACE(options,'i','');");
            DB.executeUpdate("UPDATE `ts_tracks` SET options=REPLACE(options,'r','');");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void v1_9Update() {
        try {
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `sendFinalLaps` tinyint(1) NOT NULL DEFAULT '0' AFTER `toggleSound`;");
            DB.executeUpdate("ALTER TABLE `ts_tracks` ADD `contributors` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `uuid`;");
        } catch (Exception ignored) {}

        try {
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = '-1' WHERE `boatUtilsMode` = 'VANILLA'");
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = '0' WHERE `boatUtilsMode` = 'RALLY'");
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = '1' WHERE `boatUtilsMode` = 'RALLY_BLUE'");
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = '2' WHERE `boatUtilsMode` = 'BA'");
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = '2' WHERE `boatUtilsMode` = 'BA_NOFD'");
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = '3' WHERE `boatUtilsMode` = 'PARKOUR'");
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = '5' WHERE `boatUtilsMode` = 'PARKOUR_BLUE'");
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = '4' WHERE `boatUtilsMode` = 'BA_BLUE'");
            DB.executeUpdate("UPDATE `ts_tracks` SET `boatUtilsMode` = '4' WHERE `boatUtilsMode` = 'BA_BLUE_NOFD'");

            DB.executeUpdate("ALTER TABLE `ts_tracks` modify `boatUtilsMode` int(4) NOT NULL DEFAULT '-1'");
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Override
    public void eventSet(long heatId, String column, String value) {
        DB.executeUpdateAsync("UPDATE `ts_events` SET `" + column + "` = '" + TSDatabase.sqlStringOf(value) + "' WHERE `id` = " + heatId + ";");
    }

    @Override
    public void roundSet(long heatId, String column, String value) {
        DB.executeUpdateAsync("UPDATE `ts_rounds` SET `" + column + "` = '" + TSDatabase.sqlStringOf(value) + "' WHERE `id` = " + heatId + ";");
    }

    @Override
    public void heatSet(long heatId, String column, String value) {
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `" + column + "` = '" + TSDatabase.sqlStringOf(value) + "' WHERE `id` = " + heatId + ";");
    }

    @Override
    public void driverSet(long driverId, String column, String value) {
        DB.executeUpdateAsync("UPDATE `ts_drivers` SET `" + column + "` = '" + TSDatabase.sqlStringOf(value) + "' WHERE `id` = " + driverId + ";");
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
}