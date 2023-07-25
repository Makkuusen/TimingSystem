package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.theme.TSColor;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandAlias("timingsystem|ts")
@CommandPermission("timingsystem.admin")
public class CommandTimingSystem extends BaseCommand {
    @Subcommand("tag create")
    @CommandCompletion("<tag>")
    public static void onCreateTag(CommandSender commandSender, String value) {

        if (!value.matches("[A-Za-zÅÄÖåäöØÆøæ0-9]+")) {
            Text.send(commandSender, Error.INVALID_NAME);
            return;
        }

        if (TrackTagManager.createTrackTag(value)) {
            Text.send(commandSender, Success.CREATED_TAG, "%tag%", value);
            return;
        }

        Text.send(commandSender, Error.FAILED_TO_CREATE_TAG);
    }

    @Subcommand("tag color")
    @CommandCompletion("@trackTag <hexcolorcode>")
    public static void onSetTagColor(CommandSender commandSender, TrackTag tag, String color) {
        if (!color.startsWith("#")) {
            color = "#" + color;
        }
        if (TextColor.fromHexString(color) == null) {
            Text.send(commandSender, Error.COLOR_FORMAT);
            return;
        }

        tag.setColor(Objects.requireNonNull(TextColor.fromHexString(color)));
        Text.send(commandSender, Success.SAVED);

    }

    @Subcommand("tag item")
    @CommandCompletion("@trackTag")
    public static void onSetTagItem(Player player, TrackTag tag) {
        var item = player.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null) {
            Text.send(player, Error.ITEM_NOT_FOUND);
            return;
        }
        tag.setItem(item);
        Text.send(player, Success.SAVED);
    }

    @Subcommand("tag weight")
    @CommandCompletion("@trackTag <value>")
    public static void onSetTagItem(Player player, TrackTag tag, int weight) {
        tag.setWeight(weight);
        Text.send(player, Success.SAVED);
    }

    @Subcommand("tag delete")
    @CommandCompletion("@trackTag <value>")
    public static void onDeleteTag(Player player, TrackTag tag) {
        TrackTagManager.deleteTag(tag);
        Text.send(player, Success.SAVED);
    }






    @Subcommand("hexcolor")
    @CommandCompletion("@tscolor <hexcolorcode>")
    public static void onColorChange(CommandSender sender, TSColor tsColor, String hex) {
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        TextColor color;
        Theme theme = Theme.getTheme(sender);
        if (isValidHexCode(hex)) {
            color = TextColor.fromHexString(hex);
            switch (tsColor) {
                case SECONDARY -> theme.setSecondary(color);
                case PRIMARY -> theme.setPrimary(color);
                case AWARD -> theme.setAward(color);
                case AWARD_SECONDARY -> theme.setAwardSecondary(color);
                case ERROR -> theme.setError(color);
                case BROADCAST -> theme.setBroadcast(color);
                case SUCCESS -> theme.setSuccess(color);
                case WARNING -> theme.setWarning(color);
                case TITLE -> theme.setTitle(color);
                case BUTTON -> theme.setButton(color);
                case BUTTON_ADD -> theme.setButtonAdd(color);
                case BUTTON_REMOVE -> theme.setButtonRemove(color);
                default -> {
                }
            }
            sender.sendMessage(Text.get(sender, Success.COLOR_UPDATED).color(color));
            return;
        }
        Text.send(sender,Error.COLOR_FORMAT);
    }

    @Subcommand("color")
    @CommandCompletion("@tscolor @namedColor")
    public static void onNamedColorChange(CommandSender sender, TSColor tsColor, NamedTextColor color) {
        Theme theme = Theme.getTheme(sender);
        switch (tsColor) {
            case SECONDARY -> theme.setSecondary(color);
            case PRIMARY -> theme.setPrimary(color);
            case AWARD -> theme.setAward(color);
            case AWARD_SECONDARY -> theme.setAwardSecondary(color);
            case ERROR -> theme.setError(color);
            case BROADCAST -> theme.setBroadcast(color);
            case SUCCESS -> theme.setSuccess(color);
            case WARNING -> theme.setWarning(color);
            case TITLE -> theme.setTitle(color);
            case BUTTON -> theme.setButton(color);
            case BUTTON_ADD -> theme.setButtonAdd(color);
            case BUTTON_REMOVE -> theme.setButtonRemove(color);
            default -> {
            }
        }
        sender.sendMessage(Text.get(sender, Success.COLOR_UPDATED).color(color));
    }

    public static boolean isValidHexCode(String str) {
        // Regex to check valid hexadecimal color code.
        String regex = "^#([A-Fa-f0-9]{6})$";

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        // If the string is empty
        // return false
        if (str == null) {
            return false;
        }

        // Pattern class contains matcher() method
        // to find matching between given string
        // and regular expression.
        Matcher m = p.matcher(str);

        // Return if the string
        // matched the ReGex
        return m.matches();
    }

}
