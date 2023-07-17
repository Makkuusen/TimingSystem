package me.makkuusen.timing.system.theme;

import com.destroystokyo.paper.ClientOption;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.messages.ActionBar;
import me.makkuusen.timing.system.theme.messages.Message;
import me.makkuusen.timing.system.theme.messages.MessageNoColor;
import me.makkuusen.timing.system.theme.messages.Success;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Text {

    public static TimingSystem plugin;

    public static void send(@NotNull CommandSender sender, @NotNull Message key, String... replacements) {
        String text = TimingSystem.getLanguageManager().getNewValue(key.getKey(), getLocale(sender), replacements);

        if (!text.contains("&")) {
            sender.sendMessage(Component.text(text));
            return;
        }
        sender.sendMessage(MessageParser.getComponentWithColors(text, key,  Theme.getTheme(sender)));
    }

    public static void send(@NotNull CommandSender sender, @NotNull Message key) {
        var text = TimingSystem.getLanguageManager().getNewValue(key.getKey(), getLocale(sender));

        if (text == null) {
            return;
        }

        if (!text.contains("&")) {
            sender.sendMessage(Component.text(text));
            return;
        }
        sender.sendMessage(MessageParser.getComponentWithColors(text, key, Theme.getTheme(sender)));
    }

    public static void send(@NotNull CommandSender sender, @NotNull String key, TextColor textColor) {
        var text = TimingSystem.getLanguageManager().getValue(key, getLocale(sender));
        if (text != null && !text.isEmpty()) {
            sender.sendMessage(Component.text(text).color(textColor));
        }
    }

    public static Component get(CommandSender sender, Message key) {
        var text = TimingSystem.getLanguageManager().getNewValue(key.getKey(), getLocale(sender));

        if (text == null) {
            return Component.empty();
        }

        if (!text.contains("&")) {
            return Component.text(text);
        }
        return MessageParser.getComponentWithColors(text, key, Theme.getTheme(sender));
    }

    public static Component get(CommandSender sender, Message key, String... replacements) {
        var text = TimingSystem.getLanguageManager().getNewValue(key.getKey(), getLocale(sender), replacements);

        if (text == null) {
            return Component.empty();
        }

        if (!text.contains("&")) {
            return Component.text(text);
        }
        return MessageParser.getComponentWithColors(text, key, Theme.getTheme(sender));
    }

    public static Component get(TPlayer tPlayer, Message key, String... replacements) {
        var text = TimingSystem.getLanguageManager().getNewValue(key.getKey(), getLocale(tPlayer.getPlayer()), replacements);

        if (text == null) {
            return Component.empty();
        }

        if (!text.contains("&")) {
            return Component.text(text);
        }
        return MessageParser.getComponentWithColors(text, key, tPlayer.getTheme());
    }

    public static Component get(CommandSender player, MessageNoColor key) {
        return Component.text(TimingSystem.getLanguageManager().getNewValue(key.getKey(), getLocale(player)));
    }

    public static Component get(CommandSender sender, String message) {
        return MessageParser.getComponentWithColors(message, Success.CREATED, Theme.getTheme(sender));
    }

    public static Component getActionBar(CommandSender sender, String message) {
        return MessageParser.getComponentWithColors(message, ActionBar.RACE,  Theme.getTheme(sender));
    }

    private static @NotNull String getLocale(@NotNull CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getClientOption(ClientOption.LOCALE);
        } else {
            return plugin.getConfig().getString("settings.locale", "en_us");
        }
    }
}
