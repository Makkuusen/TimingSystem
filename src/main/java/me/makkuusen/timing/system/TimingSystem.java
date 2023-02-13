package me.makkuusen.timing.system;

import co.aikar.commands.PaperCommandManager;
import co.aikar.idb.DB;
import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.gui.ButtonUtilities;
import me.makkuusen.timing.system.gui.GUIListener;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.timetrial.TimeTrial;
import me.makkuusen.timing.system.timetrial.TimeTrialListener;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

public class TimingSystem extends JavaPlugin {

    public Logger logger = null;
    public static TimingSystemConfiguration configuration;
    private static TimingSystem plugin;
    public static boolean enableLeaderboards = true;
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
        TrackDatabase.plugin = this;
        CommandTimeTrial.plugin = this;
        CommandSettings.plugin = this;
        CommandTrack.plugin = this;
        TSListener.plugin = this;
        TimeTrial.plugin = this;
        Database.plugin = this;
        ApiUtilities.plugin = this;
        this.languageManager = new LanguageManager(this, "en_us");

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new GUIListener(), plugin);
        pm.registerEvents(new TSListener(), plugin);
        pm.registerEvents(new TimeTrialListener(), plugin);

        ButtonUtilities.init();
        PlayerTimer.initPlayerTimer();

        PaperCommandManager manager = new PaperCommandManager(this);
        // enable brigadier integration for paper servers
        manager.enableUnstableAPI("brigadier");

        manager.getCommandContexts().registerContext(
                Event.class, EventDatabase.getEventContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("event", context ->
                EventDatabase.getEventsAsStrings()
        );
        manager.getCommandContexts().registerContext(
                Round.class, EventDatabase.getRoundContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("round", context ->
                EventDatabase.getRoundsAsStrings(context.getPlayer().getUniqueId())
        );
        manager.getCommandContexts().registerContext(
                Heat.class, EventDatabase.getHeatContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("heat", context ->
                EventDatabase.getHeatsAsStrings(context.getPlayer().getUniqueId())
        );
        manager.getCommandContexts().registerContext(
                Track.class, TrackDatabase.getTrackContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("track", context ->
                TrackDatabase.getTracksAsStrings()
        );
        manager.getCommandContexts().registerContext(
                TrackRegion.class, TrackDatabase.getRegionContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("region", context ->
                TrackDatabase.getRegionsAsStrings(context)
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
                RoundType.class, Round.getRoundTypeContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("roundType", context -> {
            List<String> res = new ArrayList<>();
            for(RoundType type : RoundType.values()){
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
                Boat.Type.class, TPlayer.getBoatContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("boat", context -> {
            List<String> res = new ArrayList<>();
            for (Boat.Type tree : Boat.Type.values()){
                res.add(tree.name());
            }
            return res;
        });

        manager.registerCommand(new CommandEvent());
        manager.registerCommand(new CommandRound());
        manager.registerCommand(new CommandHeat());
        manager.registerCommand(new CommandTrack());
        manager.registerCommand(new CommandTimeTrial());
        manager.registerCommand(new CommandSettings());
        taskChainFactory = BukkitTaskChainFactory.create(this);


        Database.initialize();
        Database.update();
        Database.synchronize();

        EventDatabase.getHeats().stream().filter(Heat::isActive).forEach(heat -> heat.resetHeat());


        tasks = new Tasks(this);

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            ApiUtilities.msgConsole("&cWARNING HOLOGRAPHICDISPLAYS NOT INSTALLED OR ENABLED");
            ApiUtilities.msgConsole("&cDISABLING LEADERBOARDS.");
            enableLeaderboards = false;
        } else {
            LeaderboardManager.startUpdateTask();
        }

        logger.info("Version " + getDescription().getVersion() + " enabled.");

        int pluginId = 16012;
        Metrics metrics = new Metrics(this, pluginId);

    }

    @Override
    public void onDisable() {
        EventDatabase.getHeats().stream().filter(Heat::isActive).forEach(heat -> heat.onShutdown());
        logger.info("Version " + getDescription().getVersion() + " disabled.");
        DB.close();
        TrackDatabase.plugin = null;
        Database.plugin = null;
        CommandTimeTrial.plugin = null;
        CommandSettings.plugin = null;
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
