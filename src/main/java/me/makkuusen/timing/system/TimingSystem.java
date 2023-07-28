package me.makkuusen.timing.system;

import co.aikar.commands.PaperCommandManager;
import co.aikar.idb.DB;
import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import me.makkuusen.timing.system.commands.CommandBoat;
import me.makkuusen.timing.system.commands.CommandEvent;
import me.makkuusen.timing.system.commands.CommandHeat;
import me.makkuusen.timing.system.commands.CommandRace;
import me.makkuusen.timing.system.commands.CommandRound;
import me.makkuusen.timing.system.commands.CommandSettings;
import me.makkuusen.timing.system.commands.CommandTimeTrial;
import me.makkuusen.timing.system.commands.CommandTimingSystem;
import me.makkuusen.timing.system.commands.CommandTrack;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.gui.GUIListener;
import me.makkuusen.timing.system.gui.GuiCommon;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.theme.TSColor;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.timetrial.TimeTrialListener;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackRegion;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Boat;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public class TimingSystem extends JavaPlugin {

    public Logger logger;
    private static TimingSystem plugin;
    public static TimingSystemConfiguration configuration;
    public static boolean enableLeaderboards = true;
    public static HashMap<UUID, Track> playerEditingSession = new HashMap<>();
    public static Map<UUID, TPlayer> players = new HashMap<>();
    private static LanguageManager languageManager;
    public static Instant currentTime = Instant.now();

    public static Theme defaultTheme = new Theme();
    private static TaskChainFactory taskChainFactory;

    public void onEnable() {

        plugin = this;
        logger = getLogger();
        configuration = new TimingSystemConfiguration(this);
        TSListener.plugin = this;
        Database.plugin = this;
        Text.plugin = this;
        languageManager = new LanguageManager(this, "en_us");

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new GUIListener(), plugin);
        pm.registerEvents(new TSListener(), plugin);
        pm.registerEvents(new TimeTrialListener(), plugin);

        GuiCommon.init();

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
        manager.getCommandCompletions().registerAsyncCompletion("region", TrackDatabase::getRegionsAsStrings
        );
        manager.getCommandContexts().registerContext(
                Track.TrackType.class, Track.getTrackTypeContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("trackType", context -> {
            List<String> res = new ArrayList<>();
            for (Track.TrackType type : Track.TrackType.values()) {
                res.add(type.name().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                TrackTag.class, TrackTagManager.getTrackTagContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("trackTag", context -> {
            List<String> res = new ArrayList<>();
            for (String tag : TrackTagManager.getTrackTags().keySet()) {
                res.add(tag.toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                RoundType.class, Round.getRoundTypeContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("roundType", context -> {
            List<String> res = new ArrayList<>();
            for (RoundType type : RoundType.values()) {
                res.add(type.name().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                Track.TrackMode.class, Track.getTrackModeContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("trackMode", context -> {
            List<String> res = new ArrayList<>();
            for (Track.TrackMode mode : Track.TrackMode.values()) {
                res.add(mode.name().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                Boat.Type.class, TPlayer.getBoatContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("boat", context -> {
            List<String> res = new ArrayList<>();
            for (Boat.Type tree : Boat.Type.values()) {
                res.add(tree.name().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                TSColor.class, TSColor.getTSColorContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("tsColor", context -> {
            List<String> res = new ArrayList<>();
            for (TSColor color : TSColor.values()) {
                res.add(color.name().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                NamedTextColor.class, TSColor.getNamedColorContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("namedColor", context -> {
            List<String> res = new ArrayList<>();
            for (NamedTextColor color : NamedTextColor.NAMES.values()) {
                res.add(color.toString());
            }
            return res;
        });

        manager.registerCommand(new CommandEvent());
        manager.registerCommand(new CommandRound());
        manager.registerCommand(new CommandTrack());
        manager.registerCommand(new CommandHeat());
        manager.registerCommand(new CommandTimeTrial());
        manager.registerCommand(new CommandSettings());
        manager.registerCommand(new CommandTimingSystem());
        manager.registerCommand(new CommandBoat());
        manager.registerCommand(new CommandRace());
        taskChainFactory = BukkitTaskChainFactory.create(this);


        if (!Database.initialize()) return;
        Database.update();
        Database.synchronize();

        EventDatabase.getHeats().stream().filter(Heat::isActive).forEach(Heat::resetHeat);


        var tasks = new Tasks();
        tasks.startPlayerTimer(plugin);
        tasks.startParticleSpawner(plugin);
        tasks.generateTotalTime(plugin);

        if (Bukkit.getPluginManager().getPlugin("HolographicDisplays") == null && Bukkit.getPluginManager().getPlugin("DecentHolograms") == null) {
            ApiUtilities.msgConsole("No Hologram Plugin installed. Leaderboards are disabled.");
            enableLeaderboards = false;
        } else {
            LeaderboardManager.startUpdateTask();
        }
        setConfigDefaultColors();
        logger.info("Version " + getPluginMeta().getVersion() + " enabled.");

        int pluginId = 16012;
        new Metrics(this, pluginId);


        TrackDatabase.loadTrackFinishesAsync();
    }



    private void setConfigDefaultColors() {
        for (TSColor tsColor : TSColor.values()) {
            String configColor = plugin.getConfig().getString(tsColor.getKey());
            if (configColor != null && parseColor(configColor) != null) {
                var color = parseColor(configColor);
                switch (tsColor) {
                    case SECONDARY -> defaultTheme.setSecondary(color);
                    case PRIMARY -> defaultTheme.setPrimary(color);
                    case AWARD -> defaultTheme.setAward(color);
                    case AWARD_SECONDARY -> defaultTheme.setAwardSecondary(color);
                    case ERROR -> defaultTheme.setError(color);
                    case BROADCAST -> defaultTheme.setBroadcast(color);
                    case SUCCESS -> defaultTheme.setSuccess(color);
                    case WARNING -> defaultTheme.setWarning(color);
                    case TITLE -> defaultTheme.setTitle(color);
                    case BUTTON -> defaultTheme.setButton(color);
                    case BUTTON_ADD -> defaultTheme.setButtonAdd(color);
                    case BUTTON_REMOVE -> defaultTheme.setButtonRemove(color);
                    default -> {
                    }
                }
            }
        }
    }

    private TextColor parseColor(String color) {
        if (TextColor.fromHexString(color) != null) {
            return TextColor.fromHexString(color);
        }
        if (NamedTextColor.NAMES.value(color.toLowerCase()) != null) {
            return NamedTextColor.NAMES.value(color.toLowerCase());
        }
        return null;
    }

    @Override
    public void onDisable() {
        EventDatabase.getHeats().stream().filter(Heat::isActive).forEach(Heat::onShutdown);
        logger.info("Version " + getPluginMeta().getVersion() + " disabled.");
        DB.close();
        Database.plugin = null;
        TSListener.plugin = null;
        Text.plugin = null;
        logger = null;
        plugin = null;
    }

    public static TimingSystem getPlugin() {
        return plugin;
    }

    public static LanguageManager getLanguageManager() {
        return languageManager;
    }

    public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }
}
