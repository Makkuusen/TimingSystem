package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.gui.SettingsGui;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandAlias("settings|s")
public class CommandSettings extends BaseCommand {

    static TimingSystem plugin;

    @Default
    public static void onSettings(Player player){
        new SettingsGui(Database.getPlayer(player.getUniqueId())).show(player);
    }

    @Subcommand("verbose")
    public static void onVerbose(Player player) {
        var tPlayer = Database.getPlayer(player);
        if (tPlayer.isVerbose()) {
            tPlayer.toggleVerbose();
            plugin.sendMessage(player, "messages.toggle.race.checkpointsOff");
        } else {
            tPlayer.toggleVerbose();
            plugin.sendMessage(player, "messages.toggle.race.checkpointsOn");
        }
    }

    @Subcommand("boat")
    @CommandCompletion("@boat")
    public static void onBoat(Player player, Boat.Type type){
        TPlayer tPlayer = Database.getPlayer(player.getUniqueId());
        tPlayer.setBoat(type);
        if (player.getVehicle() instanceof Boat boat) {
            boat.setBoatType(type);
        }
        plugin.sendMessage(player, "messages.save.generic");
    }

    @Subcommand("sound")
    public static void onTTSound(Player player){
        TPlayer tPlayer = Database.getPlayer(player.getUniqueId());
        tPlayer.switchToggleSound();
        player.sendMessage("§2Switched sounds to §a" + (tPlayer.isSound() ? "on" : "off") + "§2.");
    }

    @Subcommand("override")
    @CommandPermission("track.admin")
    public static void onOverride(Player player) {
        var tPlayer = Database.getPlayer(player);

        if (tPlayer.isOverride()) {
            tPlayer.toggleOverride();
            plugin.sendMessage(player, "messages.remove.override");
        } else {
            tPlayer.toggleOverride();
            plugin.sendMessage(player, "messages.create.override");
        }
    }

    @Subcommand("color")
    @CommandCompletion("<hexcolorcode>")
    public static void onColor(Player player, String hex) {
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        if (isValidHexaCode(hex)) {
            var tPlayer = Database.getPlayer(player);
            tPlayer.setHexColor(hex);
            player.sendMessage("§aYour " + tPlayer.getColorCode() + "color §awas updated");
            return;
        }
        player.sendMessage("§cYou didn't enter a valid hexadecimal color code");
    }

    public static boolean isValidHexaCode(String str)
    {
        // Regex to check valid hexadecimal color code.
        String regex = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";

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
