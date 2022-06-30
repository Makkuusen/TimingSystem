package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class Race extends JavaPlugin
{

    public Logger logger = null;
    static RaceConfiguration configuration;
    private static Race plugin;
    public static boolean enableLeaderboards = true;
    Set<UUID> override = new HashSet<>();
    Set<UUID> verbose = new HashSet<>();
    public static Map<UUID, RPlayer> players = new HashMap<UUID, RPlayer>();

    public void onEnable()
    {

        plugin = this;
        this.logger = getLogger();
        configuration = new RaceConfiguration(this);
        RaceDatabase.plugin = this;

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new MainGUIListener(), plugin);
        pm.registerEvents(new RaceListener(), plugin);

        GUIManager.init();
        PlayerTimer.init();

        getCommand("track").setExecutor(new RaceCommandTrack());
        getCommand("race").setExecutor(new RaceCommandRace());

        if (!ApiDatabase.initialize(this))
        {
            getLogger().warning("Failed to initialize database, disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
        }

        if (!ApiDatabase.synchronize())
        {
            getLogger().warning("Failed to synchronize database, disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
        }

        RaceDatabase.connect();

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays"))
        {
            RaceUtilities.msgConsole("&cWARNING HOLOGRAPHICDISPLAYS NOT INSTALLED OR ENABLED");
            RaceUtilities.msgConsole("&cDISABLING LEADERBOARDS.");
            enableLeaderboards = false;
        }
        else
        {
            LeaderboardManager.startUpdateTask();
        }

        logger.info("Version " + getDescription().getVersion() + " enabled.");

    }

    @Override
    public void onDisable()
    {
        logger.info("Version " + getDescription().getVersion() + " disabled.");
        RaceDatabase.plugin = null;
        logger = null;
        plugin = null;
    }

    public static Race getPlugin()
    {
        return plugin;
    }

}
