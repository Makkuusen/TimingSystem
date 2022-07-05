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

        try
        {
            Connection connection = ApiDatabase.getConnection();
            Statement statement = connection.createStatement();


            statement.close();
            connection.close();

            return true;
        }

        catch (SQLException exception)
        {
            plugin.getLogger().warning(exception.getMessage());
            return false;
        }
    }
    static boolean synchronize() {
        try {
            Connection connection = ApiDatabase.getConnection();
            Statement statement = connection.createStatement();
            // Load players
            ResultSet result = statement.executeQuery("SELECT * FROM `players`;");

            while (result.next()) {
                TSPlayer player = new TSPlayer(plugin, result);
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

    static TSPlayer getPlayer(UUID uuid, String name)
    {
        TSPlayer TSPlayer = plugin.players.get(uuid);

        if (TSPlayer == null)
        {
            if (name == null) { return null; }

            try
            {
                Connection connection = ApiDatabase.getConnection();
                Statement statement = connection.createStatement();

                statement.executeUpdate("INSERT INTO `players` (`uuid`, `name`) VALUES('" + uuid + "', " + ApiDatabase.sqlString(name) +");");

                ResultSet result = statement.executeQuery("SELECT * FROM `players` WHERE `uuid` = '" + uuid + "';");
                result.next();

                TSPlayer = new TSPlayer(plugin, result);
                plugin.players.put(uuid, TSPlayer);

                result.close();

                connection.close();
            }

            catch (SQLException exception)
            {
                plugin.getLogger().warning("Failed to create new player: " + exception.getMessage());
                return null;
            }
        }

        return TSPlayer;
    }

    public static TSPlayer getPlayer(UUID uuid)
    {
        return getPlayer(uuid, null);
    }

    public static TSPlayer getPlayer(String name)
    {
        for (TSPlayer player : plugin.players.values())
        {
            if (player.getName().equalsIgnoreCase(name)) { return player; }
        }

        return null;
    }

    public static TSPlayer getPlayer(CommandSender sender)
    {
        return sender instanceof org.bukkit.entity.Player ? plugin.players.get(((org.bukkit.entity.Player) sender).getUniqueId()) : null;
    }

    public static Collection<TSPlayer> getPlayers()
    {
        return plugin.players.values();
    }
}
