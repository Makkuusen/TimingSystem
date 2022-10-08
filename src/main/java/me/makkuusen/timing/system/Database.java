package me.makkuusen.timing.system;

import co.aikar.idb.BukkitDB;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

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
            var result = DB.getResults("SELECT * FROM `ts_players`;");

            for (DbRow row : result) {
                TPlayer player = new TPlayer(plugin, row);
                TimingSystem.players.put(player.getUniqueId(), player);
            }

            for(Player player: Bukkit.getOnlinePlayers()) {
                TPlayer TPlayer = Database.getPlayer(player.getUniqueId());

                TPlayer.setPlayer(player);

                if (!TPlayer.getName().equals(player.getName())) {
                    // Update name
                    TPlayer.setName(player.getName());
                }
            }

            TrackDatabase.initDatabaseSynchronize();
            EventDatabase.initDatabaseSynchronize();
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            plugin.getLogger().warning("Failed to synchronize database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return false;
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
                DB.executeUpdate("INSERT INTO `ts_players` (`uuid`, `name`, `boat`) VALUES('" + uuid + "', " + sqlString(name) + ", '" + Boat.Type.OAK.name() + "');");
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

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_players` (\n" +
                    "  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',\n" +
                    "  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `boat` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,\n" +
                    " `toggleSound` tinyint(1) DEFAULT 1 NOT NULL,\n" +
                    "  PRIMARY KEY (`uuid`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_tracks` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,\n" +
                    "  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `dateCreated` bigint(30) DEFAULT NULL,\n" +
                    "  `guiItem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `spawn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `leaderboard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `mode` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `toggleOpen` tinyint(1) NOT NULL,\n" +
                    "  `options` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,\n" +
                    "  `isRemoved` tinyint(1) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_finishes` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `trackId` int(11) NOT NULL,\n" +
                    "  `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `date` bigint(30) NOT NULL,\n" +
                    "  `time` int(11) NOT NULL,\n" +
                    "  `isRemoved` tinyint(1) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_regions` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `trackId` int(11) NOT NULL,\n" +
                    "  `regionIndex` int(11) DEFAULT NULL,\n" +
                    "  `regionType` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,\n" +
                    "  `regionShape` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
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
                    "  `isRemoved` tinyint(1) NOT NULL DEFAULT '0',\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_heats` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `roundId` int(11) NOT NULL,\n" +
                    "  `heatNumber` int(11) NOT NULL,\n" +
                    "  `state` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `startTime` bigint(30) DEFAULT NULL,\n" +
                    "  `endTime` bigint(30) DEFAULT NULL,\n" +
                    "  `fastestLapUUID` varchar(255) COLLATE utf8mb4_unicode_ci NULL,\n" +
                    "  `totalLaps` int(11) DEFAULT NULL,\n" +
                    "  `totalPitstops` int(11) DEFAULT NULL,\n" +
                    "  `timeLimit` int(11) DEFAULT NULL,\n" +
                    "  `startDelay` int(11) DEFAULT NULL,\n" +
                    "  `maxDrivers` int(11) DEFAULT NULL,\n" +
                    "  `isRemoved` tinyint(1) NOT NULL DEFAULT '0',\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_drivers` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `heatId` int(11) NOT NULL,\n" +
                    "  `position` int(11) NOT NULL,\n" +
                    "  `startPosition` int(11) NOT NULL,\n" +
                    "  `startTime` bigint(30) DEFAULT NULL,\n" +
                    "  `endTime` bigint(30) DEFAULT NULL,\n" +
                    "  `pitstops` int(11) DEFAULT NULL,\n" +
                    "  `isRemoved` tinyint(1) NOT NULL DEFAULT '0',\n" +
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
                    "  `isRemoved` tinyint(1) NOT NULL DEFAULT '0',\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_locations` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `trackId` int(11) NOT NULL,\n" +
                    "  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `index` int(11) DEFAULT NULL,\n" +
                    "  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_points` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `regionId` int(11) NOT NULL,\n" +
                    "  `x` int(11) DEFAULT NULL,\n" +
                    "  `z` int(11) DEFAULT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");


            DB.executeUpdate("CREATE TABLE IF NOT EXISTS `ts_rounds` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `eventId` int(11) NOT NULL,\n" +
                    "  `roundIndex` int(11) NOT NULL DEFAULT 1,\n" +
                    "  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,\n" +
                    "  `state` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `isRemoved` tinyint(4) NOT NULL DEFAULT 0,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n");

            return true;


        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }
}
