package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.track.TrackOption;

import java.sql.SQLException;

public class Version2 {

    public static void update() throws SQLException {
        var dbRows = DB.getResults("SELECT * FROM `ts_tracks`;");
        for (DbRow row : dbRows) {
            char[] options;
            options = row.getString("options") == null ? new char[0] : row.getString("options").toCharArray();

            for (char c : options) {
                TrackOption option = switch (c) {
                    case 'b' -> TrackOption.FORCE_BOAT;
                    case 'c' -> TrackOption.RESET_TO_LATEST_CHECKPOINT;
                    case 'e' -> TrackOption.NO_ELYTRA;
                    case 's' -> TrackOption.NO_SOUL_SPEED;
                    case 'p' -> TrackOption.NO_POTION_EFFECTS;
                    case 't' -> TrackOption.NO_RIPTIDE;
                    case 'g' -> TrackOption.FORCE_ELYTRA;
                    default -> null;
                };
                if (option != null) {
                    DB.executeUpdate("INSERT INTO `ts_tracks_options` (`trackId`, `option`) VALUES(" + row.getInt("id") + ", " + option.getId() + ");");
                }
            }
            DB.executeUpdateAsync("ALTER TABLE `ts_tracks` DROP COLUMN `options`;");
            DB.executeUpdateAsync("ALTER TABLE `ts_tracks` DROP COLUMN `leaderboard`;");
        }
    }
}
