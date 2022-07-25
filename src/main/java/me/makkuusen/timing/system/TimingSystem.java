package me.makkuusen.timing.system;

import co.aikar.commands.PaperCommandManager;
import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.gui.GUIListener;
import me.makkuusen.timing.system.gui.GUITrack;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.timetrial.TimeTrial;
import me.makkuusen.timing.system.track.Track;
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

public class TimingSystem extends JavaPlugin {

    public Logger logger = null;
    public static TimingSystemConfiguration configuration;
    private static TimingSystem plugin;
    public static boolean enableLeaderboards = true;
    public Set<UUID> override = new HashSet<>();
    public Set<UUID> verbose = new HashSet<>();
    public static HashMap<UUID, Track> playerEditingSession = new HashMap<>();
    public static Map<UUID, TPlayer> players = new HashMap<UUID, TPlayer>();
    private LanguageManager languageManager;
    public static Instant currentTime = Instant.now();
    private static TaskChainFactory taskChainFactory;
    Tasks tasks;

    public void onEnable() {

        plugin = this;
        this.logger = getLogger();
        configuration = new TimingSystemConfiguration(this);
        DatabaseTrack.plugin = this;
        CommandRace.plugin = this;
        CommandTrack.plugin = this;
        TSListener.plugin = this;
        TimeTrial.plugin = this;
        Database.plugin = this;
        this.languageManager = new LanguageManager(this, "en_us");

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new GUIListener(), plugin);
        pm.registerEvents(new TSListener(), plugin);

        GUITrack.init();
        PlayerTimer.initPlayerTimer();

        getCommand("track").setExecutor(new CommandTrack());
        getCommand("race").setExecutor(new CommandRace());

        PaperCommandManager manager = new PaperCommandManager(this);
        // enable brigadier integration for paper servers
        manager.enableUnstableAPI("brigadier");

        // optional: enable unstable api to use help
        manager.enableUnstableAPI("help");

        manager.getCommandContexts().registerContext(
                Event.class, EventDatabase.getEventContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("event", context ->
                EventDatabase.getEventsAsStrings()
        );
        manager.getCommandContexts().registerContext(
                Heat.class, EventDatabase.getHeatContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("heat", context ->
                EventDatabase.getHeatsAsStrings(context.getPlayer().getUniqueId())
        );
        manager.getCommandContexts().registerContext(
                Track.class, DatabaseTrack.getTrackContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("track", context ->
                DatabaseTrack.getTracksAsStrings()
        );
        manager.registerCommand(new CommandEvent());
        manager.registerCommand(new CommandHeat());
        taskChainFactory = BukkitTaskChainFactory.create(this);


        Database.initialize();
        Database.synchronize();


        tasks = new Tasks(this);

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            ApiUtilities.msgConsole("&cWARNING HOLOGRAPHICDISPLAYS NOT INSTALLED OR ENABLED");
            ApiUtilities.msgConsole("&cDISABLING LEADERBOARDS.");
            enableLeaderboards = false;
        } else {
            LeaderboardManager.startUpdateTask();
        }

        logger.info("Version " + getDescription().getVersion() + " enabled.");

    }

    @Override
    public void onDisable() {
        logger.info("Version " + getDescription().getVersion() + " disabled.");
        DatabaseTrack.plugin = null;
        Database.plugin = null;
        CommandRace.plugin = null;
        CommandTrack.plugin = null;
        TSListener.plugin = null;
        TimeTrial.plugin = null;
        logger = null;
        plugin = null;
    }

    public static TimingSystem getPlugin() {
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

    public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }

    public static <T> TaskChain<T> newSharedChain(String name) {
        return taskChainFactory.newSharedChain(name);
    }

}
