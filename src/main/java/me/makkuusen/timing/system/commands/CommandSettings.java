package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.gui.SettingsGui;
import me.makkuusen.timing.system.permissions.PermissionTimingSystem;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandAlias("settings|s")
public class CommandSettings extends BaseCommand {

    @Default
    public static void onSettings(Player player) {
        if(!player.hasPermission(PermissionTimingSystem.SETTINGS.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        new SettingsGui(Database.getPlayer(player.getUniqueId())).show(player);
    }

    @Subcommand("verbose")
    public static void onVerbose(Player player) {
        if(!player.hasPermission(PermissionTimingSystem.SETTINGS.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        var tPlayer = Database.getPlayer(player);
        tPlayer.toggleVerbose();
        Text.send(player, tPlayer.isVerbose() ? Success.CHECKPOINTS_ANNOUNCEMENTS_ON : Success.CHECKPOINTS_ANNOUNCEMENTS_OFF);
    }

    @Subcommand("boat")
    @CommandCompletion("@boat")
    public static void onBoat(Player player, Boat.Type type) {
        if(!player.hasPermission(PermissionTimingSystem.SETTINGS.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        TPlayer tPlayer = Database.getPlayer(player.getUniqueId());
        tPlayer.setBoat(type);
        if (player.getVehicle() instanceof Boat boat) {
            boat.setBoatType(type);
        }
        Text.send(player, Success.SAVED);
    }

    @Subcommand("sound")
    public static void onTTSound(Player player) {
        if(!player.hasPermission(PermissionTimingSystem.SETTINGS.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        TPlayer tPlayer = Database.getPlayer(player.getUniqueId());
        tPlayer.toggleSound();
        Text.send(player, tPlayer.isSound() ? Success.SOUND_ON : Success.SOUND_OFF);
    }

    @Subcommand("compactScoreboard")
    public static void onCompactScoreboard(Player player) {
        if(!player.hasPermission(PermissionTimingSystem.SETTINGS.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        TPlayer tPlayer = Database.getPlayer(player.getUniqueId());
        tPlayer.toggleCompactScoreboard();
        Text.send(player, tPlayer.isCompactScoreboard() ? Success.COMPACT_SCOREBOARD_ON : Success.COMPACT_SCOREBOARD_OFF);
    }

    @Subcommand("override")
    public static void onOverride(Player player) {
        if(!player.hasPermission(PermissionTimingSystem.SETTINGS_OVERRIDE.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        var tPlayer = Database.getPlayer(player);
        tPlayer.toggleOverride();
        Text.send(player, tPlayer.isOverride() ? Success.OVERRIDE_ON : Success.OVERRIDE_OFF);
    }


    @Subcommand("color")
    @CommandCompletion("<hexcolorcode>")
    public static void onColor(Player player, String hex) {
        if(!player.hasPermission(PermissionTimingSystem.SETTINGS.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        if (isValidHexCode(hex)) {
            var tPlayer = Database.getPlayer(player);
            tPlayer.setHexColor(hex);
            player.sendMessage(Text.get(player, Success.COLOR_UPDATED).color(tPlayer.getTextColor()));
            return;
        }
        Text.send(player, Error.COLOR_FORMAT);
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
