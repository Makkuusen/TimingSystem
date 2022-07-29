package me.makkuusen.timing.system;

import co.aikar.commands.PaperCommandManager;
import co.aikar.idb.DB;
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
import org.bukkit.TreeSpecies;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
        CommandTimeTrial.plugin = this;
        CommandTrack.plugin = this;
        TSListener.plugin = this;
        TimeTrial.plugin = this;
        Database.plugin = this;
        ApiUtilities.plugin = this;
        this.languageManager = new LanguageManager(this, "en_us");

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new GUIListener(), plugin);
        pm.registerEvents(new TSListener(), plugin);

        GUITrack.init();
        PlayerTimer.initPlayerTimer();

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

        manager.getCommandContexts().registerContext(
                Track.TrackType.class, Track.getTrackTypeContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("trackType", context -> {
            List<String> res = new ArrayList<>();
            for(Track.TrackType type : Track.TrackType.values()){
                res.add(type.name());
            }
            return res;
        });

        manager.getCommandContexts().registerContext(
                Track.TrackMode.class, Track.getTrackModeContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("trackMode", context -> {
            List<String> res = new ArrayList<>();
            for (Track.TrackMode mode : Track.TrackMode.values()){
                res.add(mode.name());
            }
            return res;
        });

        manager.getCommandContexts().registerContext(
                TreeSpecies.class, TPlayer.getBoatContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("boat", context -> {
            List<String> res = new ArrayList<>();
            for (TreeSpecies tree : TreeSpecies.values()){
                res.add(tree.name());
            }
            return res;
        });

        manager.registerCommand(new CommandEvent());
        manager.registerCommand(new CommandHeat());
        manager.registerCommand(new CommandTrack());
        manager.registerCommand(new CommandTimeTrial());
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
        DB.close();
        DatabaseTrack.plugin = null;
        Database.plugin = null;
        CommandTimeTrial.plugin = null;
        CommandTrack.plugin = null;
        TSListener.plugin = null;
        TimeTrial.plugin = null;
        ApiUtilities.plugin = null;
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
