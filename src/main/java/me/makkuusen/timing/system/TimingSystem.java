package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class TimingSystem extends JavaPlugin
{

    public Logger logger = null;
    public static TimingSystemConfiguration configuration;
    private static TimingSystem plugin;
    public static boolean enableLeaderboards = true;
    public Set<UUID> override = new HashSet<>();
    public Set<UUID> verbose = new HashSet<>();
    public static Map<UUID, TSPlayer> players = new HashMap<UUID, TSPlayer>();
    private LanguageManager languageManager;
    public Instant currentTime = Instant.now();

    public void onEnable()
    {

        plugin = this;
        this.logger = getLogger();
        configuration = new TimingSystemConfiguration(this);
        TrackDatabase.plugin = this;
        CommandRace.plugin = this;
        CommandTrack.plugin = this;
        TSListener.plugin = this;
        TimeTrial.plugin = this;
        Race.plugin = this;
        RaceDriver.plugin = this;
        this.languageManager = new LanguageManager(this, "en_us");

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new GUIListener(), plugin);
        pm.registerEvents(new TSListener(), plugin);

        GUITrack.init();
        PlayerTimer.initPlayerTimer();

        getCommand("track").setExecutor(new CommandTrack());
        getCommand("race").setExecutor(new CommandRace());

        TrackDatabase.connect();

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays"))
        {
            ApiUtilities.msgConsole("&cWARNING HOLOGRAPHICDISPLAYS NOT INSTALLED OR ENABLED");
            ApiUtilities.msgConsole("&cDISABLING LEADERBOARDS.");
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
        TrackDatabase.plugin = null;
        CommandRace.plugin = null;
        CommandTrack.plugin = null;
        TSListener.plugin = null;
        TimeTrial.plugin = null;
        Race.plugin = null;
        RaceDriver.plugin = null;
        logger = null;
        plugin = null;
    }

    public static TimingSystem getPlugin()
    {
        return plugin;
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull String key, String... replacements) {
        String message = this.languageManager.getValue(key, getLocale(sender), replacements);

        if (message != null && !message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public @Nullable String getLocalizedMessage(@NotNull CommandSender sender, @NotNull String key) {
        return this.languageManager.getValue(key, getLocale(sender));
    }

    public @Nullable String getLocalizedMessage(@NotNull CommandSender sender, @NotNull String key, String... replacements) {
        return this.languageManager.getValue(key, getLocale(sender), replacements);
    }

    private @NotNull String getLocale(@NotNull CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getLocale();
        } else {
            return this.getConfig().getString("settings.locale", "en_us");
        }
    }

}
