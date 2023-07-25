package me.makkuusen.timing.system;

import co.aikar.idb.BukkitDB;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import co.aikar.idb.PooledDatabaseOptions;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public class Database {

    public static TimingSystem plugin;

    public static boolean initialize() {
        try {
            String hostAndPort = TimingSystem.configuration.getSqlHost() + ":" + TimingSystem.configuration.getSqlPort();

            PooledDatabaseOptions options = BukkitDB.getRecommendedOptions(TimingSystem.getPlugin(), TimingSystem.configuration.getSqlUsername(), TimingSystem.configuration.getSqlPassword(), TimingSystem.configuration.getSqlDatabase(), hostAndPort);

            if (options.getOptions().getDataSourceClassName().equalsIgnoreCase("org.mariadb.jdbc.MariaDbDataSource")) {
                options.getOptions().setDsn("mariadb://" + hostAndPort + "/" + TimingSystem.configuration.getSqlDatabase());
            }

            BukkitDB.createHikariDatabase(TimingSystem.getPlugin(), options);
            return createTables();
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().warning("Failed to initialize database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static boolean update() {
        try {
            var row = DB.getFirstRow("SELECT * FROM `ts_version` ORDER BY `date` DESC;");
            if (row == null) {
                DB.executeInsert("INSERT INTO `ts_version` (`version`, `date`) VALUES('" + plugin.getPluginMeta().getVersion() + "', " + ApiUtilities.getTimestamp() + ");");
                rc9Update();
                v1_0Update();
                v1_2Update();
                v1_3Update();
            } else {
                if (isNewerVersion(row.getString("version"), plugin.getPluginMeta().getVersion())) {
                    updateDatabase(row.getString("version"));
                    plugin.getLogger().warning("UPDATING DATABASE FROM " + row.getString("version") + " to " + plugin.getPluginMeta().getVersion());
                    DB.executeInsert("INSERT INTO `ts_version` (`version`, `date`) VALUES('" + plugin.getPluginMeta().getVersion() + "', " + ApiUtilities.getTimestamp() + ");");
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().warning("Failed to update database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }
    }

    public static void synchronize() {
        try {
            // Load players;
            var result = DB.getResults("SELECT * FROM `ts_players`;");

            for (DbRow row : result) {
                TPlayer player = new TPlayer(plugin, row);
                TimingSystem.players.put(player.getUniqueId(), player);
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                TPlayer TPlayer = Database.getPlayer(player.getUniqueId());

                TPlayer.setPlayer(player);

                if (!TPlayer.getName().equals(player.getName())) {
                    // Update name
                    TPlayer.setName(player.getName());
                }
            }

            TrackDatabase.initDatabaseSynchronize();
            EventDatabase.initDatabaseSynchronize();
        } catch (Exception exception) {
            exception.printStackTrace();
            plugin.getLogger().warning("Failed to synchronize database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public static void reload() {
        TrackDatabase.unload();
        try {
            TrackDatabase.initDatabaseSynchronize();
        } catch (Exception exception) {
            exception.printStackTrace();
            plugin.getLogger().warning("Failed to synchronize database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    static TPlayer getPlayer(UUID uuid, String name) {
        TPlayer TPlayer = TimingSystem.players.get(uuid);

        if (TPlayer == null) {
            if (name == null) {
                return null;
            }

            try {
                DB.executeUpdate("INSERT INTO `ts_players` (`uuid`, `name`, `boat`) VALUES('" + uuid + "', " + sqlString(name) + ", '" + Boat.Type.BIRCH.name() + "');");
                var dbRow = DB.getFirstRow("SELECT * FROM `ts_players` WHERE `uuid` = '" + uuid + "';");

                TPlayer = new TPlayer(plugin, dbRow);
                TimingSystem.players.put(uuid, TPlayer);
            } catch (SQLException exception) {
                plugin.getLogger().warning("Failed to create new player: " + exception.getMessage());
                return null;
            }
        }

        return TPlayer;
    }

    public static TPlayer getPlayer(UUID uuid) {
        return getPlayer(uuid, null);
    }

    public static TPlayer getPlayer(String name) {
        for (TPlayer player : TimingSystem.players.values()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }

        return null;
    }

    public static TPlayer getPlayer(CommandSender sender) {
        return sender instanceof org.bukkit.entity.Player ? TimingSystem.players.get(((org.bukkit.entity.Player) sender).getUniqueId()) : null;
    }

    public static String sqlString(String string) {
        return string == null ? "NULL" : "'" + string.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    public static boolean createTables() {
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
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private static void updateDatabase(String oldVersion) {


        if (isNewerVersion(oldVersion, "1.2")) {
            v1_2Update();
        }

        if (isNewerVersion(oldVersion, "1.3")) {
            v1_3Update();
        }

        if (isNewerVersion(oldVersion, "1.6")) {
            v1_6Update();
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

    private static boolean isNewerVersion(String oldVersion, String newVersion) {
        if (oldVersion.equalsIgnoreCase(newVersion)) {
            return false;
        }
        try {
            String[] old = oldVersion.split("\\.");
            int oldMajor = Integer.parseInt(old[0]);
            int oldMinor = Integer.parseInt(old[1]);
            String[] newer = newVersion.split("\\.");
            int newMajor = Integer.parseInt(newer[0]);
            int newMinor = Integer.parseInt(newer[1]);
            if (newMajor > oldMajor) {
                return true;
            } else return newMajor == oldMajor && newMinor > oldMinor;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }
}
