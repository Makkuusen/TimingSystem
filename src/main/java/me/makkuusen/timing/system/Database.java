package me.makkuusen.timing.system;

import co.aikar.idb.BukkitDB;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.event.EventDatabase;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.UUID;

public class Database {

    public static TimingSystem plugin;

    public static boolean initialize() {
        try {
            BukkitDB.createHikariDatabase(TimingSystem.getPlugin(),
                    TimingSystem.configuration.getSqlUsername(),
                    TimingSystem.configuration.getSqlPassword(),
                    TimingSystem.configuration.getSqlDatabase(),
                    TimingSystem.configuration.getSqlHost() + ":" + TimingSystem.configuration.getSqlPort()
            );
            return createTables();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }
    }

    public static boolean synchronize() {
        try {
            // Load players;
            var result = DB.getResults("SELECT * FROM `players`;");

            for (DbRow row : result) {
                TPlayer player = new TPlayer(plugin, row);
                TimingSystem.players.put(player.getUniqueId(), player);
            }

            DatabaseTrack.initDatabaseSynchronize();
            EventDatabase.initDatabaseSynchronize();
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            plugin.getLogger().warning("Failed to synchronize database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }
    }

    static TPlayer getPlayer(UUID uuid, String name) {
        TPlayer TPlayer = TimingSystem.players.get(uuid);

        if (TPlayer == null) {
            if (name == null) {
                return null;
            }

            try {
                DB.executeUpdate("INSERT INTO `players` (`uuid`, `name`, `dateJoin`, `dateNameChange`, `dateNameCheck`, `dateSeen`) VALUES('" + uuid + "', " + sqlString(name) + ", " + ApiUtilities.getTimestamp() + ", -1, 0, 0);");
                var dbRow = DB.getFirstRow("SELECT * FROM `players` WHERE `uuid` = '" + uuid + "';");

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

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `players` (\n" +
                    "  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',\n" +
                    "  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `dateJoin` bigint(30) DEFAULT NULL,\n" +
                    "  `dateNameChange` bigint(30) DEFAULT NULL,\n" +
                    "  `dateNameCheck` bigint(30) DEFAULT NULL,\n" +
                    "  `dateSeen` bigint(30) DEFAULT NULL,\n" +
                    "  PRIMARY KEY (`uuid`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `tracks` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,\n" +
                    "  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `dateCreated` bigint(30) DEFAULT NULL,\n" +
                    "  `guiItem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `spawn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `leaderboard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `type` longblob NOT NULL,\n" +
                    "  `mode` longblob DEFAULT NULL,\n" +
                    "  `toggleOpen` tinyint(1) NOT NULL,\n" +
                    "  `toggleGovernment` tinyint(1) NOT NULL,\n" +
                    "  `options` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,\n" +
                    "  `isRemoved` tinyint(1) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `tracksFinishes` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `trackId` int(11) NOT NULL,\n" +
                    "  `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `date` bigint(30) NOT NULL,\n" +
                    "  `time` int(11) NOT NULL,\n" +
                    "  `isRemoved` tinyint(1) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `tracksRegions` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `trackId` int(11) NOT NULL,\n" +
                    "  `regionIndex` int(11) DEFAULT NULL,\n" +
                    "  `regionType` longblob DEFAULT NULL,\n" +
                    "  `minP` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,\n" +
                    "  `maxP` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,\n" +
                    "  `spawn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `isRemoved` tinyint(1) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_events` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `date` bigint(30) DEFAULT NULL,\n" +
                    "  `track` int(11) DEFAULT NULL,\n" +
                    "  `state` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `isRemoved` tinyint(1) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_heats` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `eventId` int(11) NOT NULL,\n" +
                    "  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `state` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `startTime` bigint(30) DEFAULT NULL,\n" +
                    "  `endTime` bigint(30) DEFAULT NULL,\n" +
                    "  `fastestLap` int(11) DEFAULT NULL,\n" +
                    "  `totalLaps` int(11) DEFAULT NULL,\n" +
                    "  `totalPitstops` int(11) DEFAULT NULL,\n" +
                    "  `timeLimit` int(11) DEFAULT NULL,\n" +
                    "  `isRemoved` tinyint(1) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_drivers` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `heatId` int(11) NOT NULL,\n" +
                    "  `isFinished` tinyint(1) NOT NULL,\n" +
                    "  `position` int(11) NOT NULL,\n" +
                    "  `startPosition` int(11) NOT NULL,\n" +
                    "  `startTime` bigint(30) DEFAULT NULL,\n" +
                    "  `endTime` bigint(30) DEFAULT NULL,\n" +
                    "  `pitstops` int(11) DEFAULT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_laps` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `heatId` int(11) NOT NULL,\n" +
                    "  `trackId` int(11) NOT NULL,\n" +
                    "  `lapStart` bigint(30) DEFAULT NULL,\n" +
                    "  `lapEnd` bigint(30) DEFAULT NULL,\n" +
                    "  `pitted` tinyint(1) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            return true;


        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }
}
