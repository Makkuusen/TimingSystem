package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.gui.GUISettings;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

@CommandAlias("settings|s")
public class CommandSettings extends BaseCommand {

    static TimingSystem plugin;

    @Default
    public static void onSettings(Player player){
        GUISettings.openSettingsGui(player);
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
}
