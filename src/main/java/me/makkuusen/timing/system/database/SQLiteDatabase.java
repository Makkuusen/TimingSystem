package me.makkuusen.timing.system.database;

import co.aikar.idb.BukkitDB;
import co.aikar.idb.DB;
import co.aikar.idb.DatabaseOptions;
import co.aikar.idb.PooledDatabaseOptions;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Material;

import java.io.File;
import java.sql.SQLException;

import static me.makkuusen.timing.system.TimingSystem.getPlugin;

public class SQLiteDatabase extends MySQLDatabase {
    @Override
    public boolean initialize() {
        DatabaseOptions options = DatabaseOptions.builder().poolName(getPlugin().getDescription().getName() + " DB").logger(getPlugin().getLogger()).sqlite(new File(getPlugin().getDataFolder(), "ts.db").getPath()).build();
        PooledDatabaseOptions poolOptions = PooledDatabaseOptions.builder().options(options).build();
        BukkitDB.createHikariDatabase(TimingSystem.getPlugin(), poolOptions);
        return createTables();
    }

    @Override
    public boolean update() {
        // There are no updates :(
        return true;
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
                        `timetrial` INTEGER NOT NULL DEFAULT 1,
                        `toggleSound` INTEGER DEFAULT 1 NOT NULL,
                        `sendFinalLaps` INTEGER DEFAULT 0 NOT NULL
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
                          `boatUtilsMode` INTEGER NOT NULL DEFAULT 0,
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
}
