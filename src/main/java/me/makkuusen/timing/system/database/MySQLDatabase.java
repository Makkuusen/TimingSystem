package me.makkuusen.timing.system.database;

import co.aikar.idb.BukkitDB;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import co.aikar.idb.PooledDatabaseOptions;
import me.makkuusen.timing.system.*;
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

import static me.makkuusen.timing.system.TimingSystem.getPlugin;

public class MySQLDatabase implements TSDatabase {
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
                      `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
                      `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                      `boat` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                      `color` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#9D9D97',
                      `chestBoat` tinyint(1) NOT NULL DEFAULT '0',
                      `compactScoreboard` tinyint(1) NOT NULL DEFAULT '0',
                      `override` tinyint(1) NOT NULL DEFAULT '0',
                      `verbose` tinyint(1) NOT NULL DEFAULT '0',
                      `toggleSound` tinyint(1) NOT NULL DEFAULT '1',
                      PRIMARY KEY (`uuid`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;""");

            DB.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `ts_tracks` (
                          `id` int(11) NOT NULL AUTO_INCREMENT,
                          `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                          `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                          `dateCreated` bigint(30) DEFAULT NULL,
                          `dateChanged` bigint(30) DEFAULT NULL,
                          `weight` int(11) NOT NULL DEFAULT '100',
                          `guiItem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                          `spawn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                          `leaderboard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                          `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                          `mode` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                          `toggleOpen` tinyint(1) NOT NULL,
                          `options` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                          `boatUtilsMode` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VANILLA',
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
                          `open` tinyint(1) NOT NULL DEFAULT '1',
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
                          `color` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#ffffff',
                          `item` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                          `weight` int(11) NOT NULL DEFAULT '100',
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

    private static void updateDatabase(String oldVersion) {
        if (TSDatabase.isNewerVersion(oldVersion, "1.9"))
            v1_9Update();
    }

    private static void v1_9Update() {
        try {
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `sendFinalLaps` tinyint(1) NOT NULL DEFAULT '0' AFTER `toggleSound`;");
            DB.executeUpdate("ALTER TABLE `ts_tracks` ADD `contributors` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `uuid`;");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
