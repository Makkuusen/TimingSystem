package me.makkuusen.timing.system;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.UUID;

public class ApiDatabase {

    private static TimingSystem plugin;
    private static HikariDataSource dataSource;

    static boolean initialize(TimingSystem plugin) {
        ApiDatabase.plugin = plugin;

        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setMaximumPoolSize(20);
        dataSource.setJdbcUrl("jdbc:mysql://" + plugin.getConfig().getString("sql.host") + ":" + plugin.getConfig().getInt("sql.port") + "/" + plugin.getConfig().getString("sql.database") + "?useSSL=false");
        dataSource.setUsername( plugin.getConfig().getString("sql.username"));
        dataSource.setPassword( plugin.getConfig().getString("sql.password"));

        ApiDatabase.dataSource = dataSource;
        return createTables();

    }
    static boolean synchronize() {
        try {
            Connection connection = ApiDatabase.getConnection();
            Statement statement = connection.createStatement();
            // Load players
            ResultSet result = statement.executeQuery("SELECT * FROM `players`;");

            while (result.next()) {
                TPlayer player = new TPlayer(plugin, result);
                plugin.players.put(player.getUniqueId(), player);
            }

            result.close();
            connection.close();
            return true;
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
            return false;
        }
    }
    public static Connection getConnection()
    {
        try
        {
            return dataSource.getConnection();
        }

        catch (SQLException exception)
        {
            plugin.getLogger().warning("Failed to get a pooled connection: " + exception.getMessage());
            return null;
        }
    }

    public static void asynchronousQuery(final String[] query)
    {
        asynchronousQuery(query, false);
    }

    public static void asynchronousQuery(final String[] query, boolean isSelect)
    {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable()
        {
            @Override
            public void run()
            {
                int i = 0;

                try
                {
                    Connection connection = getConnection();
                    Statement statement = connection.createStatement();

                    for (i = 0; i < query.length; i++)
                    {
                        if (isSelect) { statement.executeQuery(query[i]); }
                        else { statement.executeUpdate(query[i]); }
                    }

                    statement.close();
                    connection.close();
                }

                catch (Exception exception)
                {
                    plugin.getLogger().warning("Asynchronous SQL query " + i + " failed.");
                    plugin.getLogger().warning("--- Start of Queries ---");

                    for (int j = 0; j < query.length; j++)
                    {
                        plugin.getLogger().warning(j + ": " + query[j]);
                    }

                    plugin.getLogger().warning("--- End of Queries ---");

                    exception.printStackTrace();
                }
            }
        });
    }

    public static void synchronousQuery(final String[] query)
    {
        synchronousQuery(query, false);
    }

    public static void synchronousQuery(final String[] query, boolean isSelect)
    {
        int i = 0;

        try
        {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();

            for (i = 0; i < query.length; i++)
            {
                if (isSelect) { statement.executeQuery(query[i]); }
                else { statement.executeUpdate(query[i]); }
            }

            statement.close();
            connection.close();
        }

        catch (Exception exception)
        {
            plugin.getLogger().warning("Synchronous SQL query " + i + " failed.");
            plugin.getLogger().warning("--- Start of Queries ---");

            for (int j = 0; j < query.length; j++)
            {
                plugin.getLogger().warning(j + ": " + query[j]);
            }

            plugin.getLogger().warning("--- End of Queries ---");

            exception.printStackTrace();
        }
    }

    public static String sqlString(String string)
    {
        return string == null ? "NULL" : "'" + string.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    static TPlayer getPlayer(UUID uuid, String name)
    {
        TPlayer TPlayer = plugin.players.get(uuid);

        if (TPlayer == null)
        {
            if (name == null) { return null; }

            try
            {
                Connection connection = ApiDatabase.getConnection();
                Statement statement = connection.createStatement();

                statement.executeUpdate("INSERT INTO `players` (`uuid`, `name`) VALUES('" + uuid + "', " + ApiDatabase.sqlString(name) +");");

                ResultSet result = statement.executeQuery("SELECT * FROM `players` WHERE `uuid` = '" + uuid + "';");
                result.next();

                TPlayer = new TPlayer(plugin, result);
                plugin.players.put(uuid, TPlayer);

                result.close();

                connection.close();
            }

            catch (SQLException exception)
            {
                plugin.getLogger().warning("Failed to create new player: " + exception.getMessage());
                return null;
            }
        }

        return TPlayer;
    }

    public static TPlayer getPlayer(UUID uuid)
    {
        return getPlayer(uuid, null);
    }

    public static TPlayer getPlayer(String name)
    {
        for (TPlayer player : plugin.players.values())
        {
            if (player.getName().equalsIgnoreCase(name)) { return player; }
        }

        return null;
    }

    public static TPlayer getPlayer(CommandSender sender)
    {
        return sender instanceof org.bukkit.entity.Player ? plugin.players.get(((org.bukkit.entity.Player) sender).getUniqueId()) : null;
    }

    public static Collection<TPlayer> getPlayers()
    {
        return plugin.players.values();
    }

    public static boolean createTables(){
        try {

            Connection connection = getConnection();
            Statement statement = connection.createStatement();

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `players` (\n" +
                    "  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',\n" +
                    "  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `dateJoin` bigint(30) DEFAULT NULL,\n" +
                    "  `dateNameChange` bigint(30) DEFAULT NULL,\n" +
                    "  `dateNameCheck` bigint(30) DEFAULT NULL,\n" +
                    "  `dateSeen` bigint(30) DEFAULT NULL,\n" +
                    "  PRIMARY KEY (`uuid`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `tracks` (\n" +
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

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `tracksFinishes` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `trackId` int(11) NOT NULL,\n" +
                    "  `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                    "  `date` bigint(30) NOT NULL,\n" +
                    "  `time` int(11) NOT NULL,\n" +
                    "  `isRemoved` tinyint(1) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `tracksRegions` (\n" +
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

            statement.close();
            connection.close();
            return true;


        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }


    }
}
