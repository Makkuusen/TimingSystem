package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

public class Version3 {

    public static void updateMySQL() {
        DB.executeUpdateAsync("ALTER TABLE `ts_players` ADD `shortName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL after `name`;");
        DB.executeUpdateAsync("ALTER TABLE `ts_tracks` ADD `timeTrial` tinyint(1) NOT NULL DEFAULT 1 after `type`;");
        DB.executeUpdateAsync("ALTER TABLE `ts_tracks` DROP COLUMN `mode`;");
    }

    public static void updateSQLite() {
        DB.executeUpdateAsync("ALTER TABLE `ts_players` ADD `shortName` TEXT DEFAULT NULL after `name`;");
        DB.executeUpdateAsync("ALTER TABLE `ts_tracks` ADD `timeTrial` INTEGER NOT NULL DEFAULT 1 after `type`;");
        DB.executeUpdateAsync("ALTER TABLE `ts_tracks` DROP COLUMN `mode`;");
    }
}
