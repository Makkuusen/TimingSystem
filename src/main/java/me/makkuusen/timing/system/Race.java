package me.makkuusen.timing.system;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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

public class Race extends JavaPlugin
{

    public Logger logger = null;
    static RaceConfiguration configuration;
    private static Race plugin;
    public static boolean enableLeaderboards = true;
    Set<UUID> override = new HashSet<>();
    Set<UUID> verbose = new HashSet<>();
    public static Map<UUID, RPlayer> players = new HashMap<UUID, RPlayer>();
    private LanguageManager languageManager;
    public Instant currentTime = Instant.now();

    public void onEnable()
    {

        plugin = this;
        this.logger = getLogger();
        configuration = new RaceConfiguration(this);
        RaceDatabase.plugin = this;
        RaceCommandRace.plugin = this;
        RaceCommandTrack.plugin = this;
        RaceListener.plugin = this;
        TimeTrial.plugin = this;
        this.languageManager = new LanguageManager(this, "en_us");

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new MainGUIListener(), plugin);
        pm.registerEvents(new RaceListener(), plugin);

        GUIManager.init();
        RaceController.initTimeTrials();

        getCommand("track").setExecutor(new RaceCommandTrack());
        getCommand("race").setExecutor(new RaceCommandRace());

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
        RaceCommandRace.plugin = null;
        RaceCommandTrack.plugin = null;
        RaceListener.plugin = null;
        TimeTrial.plugin = null;
        logger = null;
        plugin = null;
    }

    public static Race getPlugin()
    {
        return plugin;
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull String key, String... replacements) {
        String message = this.languageManager.getValue(key, getLocale(sender), replacements);

        if (message != null && !message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public void sendSystemMessage(@NotNull Player player, @NotNull String key) {
        String message = this.languageManager.getValue(key, getLocale(player));

        if (message == null) {
            return;
        }

        int newline = message.indexOf('\n');
        if (newline != -1) {
            // No newlines in action bar chat.
            message = message.substring(0, newline);
        }

        if (message.isEmpty()) {
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
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
