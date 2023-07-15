package me.makkuusen.timing.system;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import com.destroystokyo.paper.ClientOption;
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
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.gui.GuiCommon;
import me.makkuusen.timing.system.gui.GUIListener;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.theme.messages.ActionBar;
import me.makkuusen.timing.system.theme.messages.Message;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.TSColor;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.timetrial.TimeTrial;
import me.makkuusen.timing.system.timetrial.TimeTrialListener;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackRegion;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
    private LanguageManager languageManager;
    public static Instant currentTime = Instant.now();
    private static TaskChainFactory taskChainFactory;

    public void onEnable() {

        plugin = this;
        logger = getLogger();
        configuration = new TimingSystemConfiguration(this);
        TrackDatabase.plugin = this;
        CommandTimeTrial.plugin = this;
        CommandSettings.plugin = this;
        CommandTrack.plugin = this;
        CommandTimingSystem.plugin = this;
        CommandEvent.plugin = this;
        CommandRound.plugin = this;
        CommandHeat.plugin = this;
        CommandRace.plugin = this;
        TSListener.plugin = this;
        TimeTrial.plugin = this;
        Database.plugin = this;
        ApiUtilities.plugin = this;
        EventAnnouncements.plugin = this;
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
                TSColor.class, TSColor.getColorContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("tsColor", context -> {
            List<String> res = new ArrayList<>();
            for (TSColor color : TSColor.values()) {
                res.add(color.name().toLowerCase());
            }
            return res;
        });

        manager.getCommandContexts().registerContext(
                NamedTextColor.class, getColorContextResolver());
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

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            ApiUtilities.msgConsole("&cWARNING HOLOGRAPHICDISPLAYS NOT INSTALLED OR ENABLED");
            ApiUtilities.msgConsole("&cDISABLING LEADERBOARDS.");
            enableLeaderboards = false;
        } else {
            LeaderboardManager.startUpdateTask();
        }

        logger.info("Version " + getPluginMeta().getVersion() + " enabled.");

        int pluginId = 16012;
        new Metrics(this, pluginId);

    }

    private ContextResolver<NamedTextColor, BukkitCommandExecutionContext> getColorContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return NamedTextColor.NAMES.value(name);
            } catch (IllegalArgumentException e) {
                //no matching boat types
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    @Override
    public void onDisable() {
        EventDatabase.getHeats().stream().filter(Heat::isActive).forEach(Heat::onShutdown);
        logger.info("Version " + getPluginMeta().getVersion() + " disabled.");
        DB.close();
        TrackDatabase.plugin = null;
        Database.plugin = null;
        CommandTimeTrial.plugin = null;
        CommandSettings.plugin = null;
        CommandTrack.plugin = null;
        CommandTimingSystem.plugin = null;
        CommandRound.plugin = null;
        CommandHeat.plugin = null;
        CommandEvent.plugin = null;
        CommandRace.plugin = null;
        TSListener.plugin = null;
        TimeTrial.plugin = null;
        ApiUtilities.plugin = null;
        EventAnnouncements.plugin = null;
        logger = null;
        plugin = null;
    }

    public static TimingSystem getPlugin() {
        return plugin;
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull Message key, String... replacements) {
        String text = this.languageManager.getNewValue(key.getKey(), getLocale(sender), replacements);

        if (!text.contains("&")) {
            sender.sendMessage(Component.text(text));
            return;
        }
        sender.sendMessage(getComponentWithColors(text, key,  getTheme(sender)));
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull Message key) {
        var text = this.languageManager.getNewValue(key.getKey(), getLocale(sender));

        if (text == null) {
            return;
        }

        if (!text.contains("&")) {
            sender.sendMessage(Component.text(text));
            return;
        }
        sender.sendMessage(getComponentWithColors(text, key, getTheme(sender)));
    }


    public Component getText(CommandSender sender, Message key) {
        var text = this.languageManager.getNewValue(key.getKey(), getLocale(sender));

        if (text == null) {
            return Component.empty();
        }

        if (!text.contains("&")) {
            return Component.text(text);
        }
        return getComponentWithColors(text, key, getTheme(sender));
    }

    public Component getText(CommandSender sender, Message key, String... replacements) {
        var text = this.languageManager.getNewValue(key.getKey(), getLocale(sender), replacements);

        if (text == null) {
            return Component.empty();
        }

        if (!text.contains("&")) {
            return Component.text(text);
        }
        return getComponentWithColors(text, key, getTheme(sender));
    }

    public Component getText(TPlayer tPlayer, Message key, String... replacements) {
        var text = this.languageManager.getNewValue(key.getKey(), getLocale(tPlayer.getPlayer()), replacements);

        if (text == null) {
            return Component.empty();
        }

        if (!text.contains("&")) {
            return Component.text(text);
        }
        return getComponentWithColors(text, key, tPlayer.getTheme());
    }

    public Component getActionBarText(CommandSender sender, String message) {
        return getComponentWithColors(message, ActionBar.RACE,  getTheme(sender));
    }

    public Component getText(CommandSender sender, String message) {
        return getComponentWithColors(message, Success.CREATED, getTheme(sender));
    }

    private Theme getTheme(CommandSender sender) {
        return sender instanceof Player ? Database.getPlayer(((Player) sender).getUniqueId()).getTheme() : new Theme();
    }


    private Component getComponentWithColors(String text, Message level, Theme theme) {

        TextColor color = NamedTextColor.WHITE;
        List<TextDecoration> decorations = new ArrayList<>();

        String[] strings = text.split("&");
        Component component = Component.empty();

        boolean first = true;
        for (String string : strings) {
            String message = first ? "" : "&";
            if (string.length() > 0) {
                String option = string.substring(0, 1);
                message = string.substring(1);
                switch (option) {
                    case "1" -> color = theme.getPrimary();
                    case "2" -> color = theme.getSecondary();
                    case "s" -> color = theme.getSuccess();
                    case "w" -> color = theme.getWarning();
                    case "e" -> color = theme.getError();
                    case "b" -> color = theme.getBroadcast();
                    case "a" -> color = theme.getAwardSecondary();
                    case "c" -> color = theme.getAward();
                    case "t" -> color = theme.getTitle();
                    case "o" -> decorations.add(TextDecoration.ITALIC);
                    case "l" -> decorations.add(TextDecoration.BOLD);
                    case "r" -> {
                        decorations = new ArrayList<>();
                        color = NamedTextColor.WHITE;
                    }
                    default -> {
                        message = first ? string : "&" + string;
                    }
                }
            }
            first = false;
            if (level instanceof ActionBar) {
                if (!color.asHexString().equalsIgnoreCase("#ffffff")) {
                    color = TextColor.fromHexString(darkenHexColor(color.asHexString(), 0.1));
                }
            }
            component = component.append(buildComponent(message, color, decorations));
        }

        return component;

    }

    private Component buildComponent(String message, TextColor color, List<TextDecoration> decorations) {
        var newComponent = Component.text(message).color(color);
        for (TextDecoration decoration : decorations) {
            newComponent = newComponent.decorate(decoration);
        }
        return newComponent;
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull String key, TextColor textColor) {
        var text = this.languageManager.getValue(key, getLocale(sender));
        if (text != null && !text.isEmpty()) {
            sender.sendMessage(Component.text(text).color(textColor));
        }
    }

    private @NotNull String getLocale(@NotNull CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getClientOption(ClientOption.LOCALE);
        } else {
            return this.getConfig().getString("settings.locale", "en_us");
        }
    }

    public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }


    public static String darkenHexColor(String hexColor, double darkenAmount) {
        // Remove the '#' symbol and convert to RGB values
        int r = Integer.parseInt(hexColor.substring(1, 3), 16);
        int g = Integer.parseInt(hexColor.substring(3, 5), 16);
        int b = Integer.parseInt(hexColor.substring(5, 7), 16);

        // Darken each color component
        r = (int) (r * (1 - darkenAmount));
        g = (int) (g * (1 - darkenAmount));
        b = (int) (b * (1 - darkenAmount));

        // Ensure the color components are within the valid range (0-255)
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        // Convert the darkened RGB values back to hex
        return String.format("#%02X%02X%02X", r, g, b);
    }

}
