package me.makkuusen.timing.system.database;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

// TODO: Convert other database classes to a similar system(?)
public interface TSDatabase {

    boolean initialize();

    boolean update();

    boolean createTables();

    List<DbRow> selectPlayers() throws SQLException;

    TPlayer createPlayer(UUID uuid, String name);

    void playerUpdateValue(UUID uuid, String column, String value);

    void playerUpdateValue(UUID uuid, String column, Boolean value);


    static void synchronize() {
        try {
            // Load players;
            var result = TimingSystem.getDatabase().selectPlayers();

            for (DbRow row : result) {
                TPlayer player = new TPlayer(TimingSystem.getPlugin(), row);
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
            TimingSystem.getPlugin().getLogger().warning("Failed to synchronize database, disabling plugin.");
            TimingSystem.getPlugin().getServer().getPluginManager().disablePlugin(TimingSystem.getPlugin());
        }
    }

    static String sqlStringOf(String s) {
        return s == null ? "NULL" : "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    static TPlayer getPlayer(UUID uuid) {
        return getPlayer(uuid, null);
    }

    static TPlayer getPlayer(String name) {
        for (TPlayer player : TimingSystem.players.values()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    static TPlayer getPlayer(CommandSender sender) {
        return sender instanceof Player ? TimingSystem.players.get(((org.bukkit.entity.Player) sender).getUniqueId()) : null;
    }

    static TPlayer getPlayer(UUID uuid, String name) {
        TPlayer tPlayer = TimingSystem.players.get(uuid);

        if(tPlayer == null) {
            if(name == null)
                return null;

            tPlayer = get().createPlayer(uuid, name);
            TimingSystem.players.put(uuid, tPlayer);
        }

        return tPlayer;
    }

    static void reload() {
        TrackDatabase.unload();
        try {
            TrackDatabase.initDatabaseSynchronize();
            TrackDatabase.loadTrackFinishesAsync();
        } catch (Exception exception) {
            exception.printStackTrace();
            TimingSystem.getPlugin().getLogger().warning("Failed to synchronize database, disabling plugin.");
            TimingSystem.getPlugin().getServer().getPluginManager().disablePlugin(TimingSystem.getPlugin());
        }
    }

    static boolean isNewerVersion(String oldVersion, String newVersion) {
        if (oldVersion.equalsIgnoreCase(newVersion))
            return false;

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

    static TSDatabase get() {
        return TimingSystem.getDatabase();
    }
}
