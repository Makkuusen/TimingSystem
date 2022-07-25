package me.makkuusen.timing.system;

import me.makkuusen.timing.system.gui.GUITrack;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandRace implements CommandExecutor {
    static TimingSystem plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] arguments) {
        if (!(sender instanceof Player player)) {
            ApiUtilities.msgConsole("§cCommand can only be used by players");
            return true;
        }
        if (arguments.length == 0) {
            if (!player.hasPermission("race.command.race") && !player.isOp()) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            GUITrack.openTrackGUI(player);
            return true;
        }

        if (arguments[0].equalsIgnoreCase("cancel")) {
            if (!player.isOp() && !player.hasPermission("race.command.cancel") && !player.isOp()) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (!TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                plugin.sendMessage(player, "messages.error.runNotStarted");
                return true;
            }
            TimeTrialController.playerCancelMap(player);
            plugin.sendMessage(player, "messages.cancel");
            return true;
        } else if (arguments[0].equalsIgnoreCase("help")) {
            if (!player.isOp() && !player.hasPermission("race.command.help") && !player.isOp()) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            cmdHelp(player);
            return true;
        } else if (arguments[0].equalsIgnoreCase("toggle")) {
            if (!player.isOp() && !player.hasPermission("race.command.toggle") && !player.isOp()) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length < 2) {
                player.sendMessage("§7Syntax: /race toggle checkpointmessages");
                return true;
            }
            cmdToggle(player, arguments);
            return true;
        }
        return true;
    }

    static void cmdHelp(Player player) {
        player.sendMessage("");
        plugin.sendMessage(player, "messages.help", "%command%", "race");

        if (player.isOp() || player.hasPermission("race.command.cancel")) {
            player.sendMessage("§2/race cancel");
        }
        if (player.isOp() || player.hasPermission("race.command.toggle")) {
            player.sendMessage("§2/race toggle checkpointmessages");
        }

    }

    static void cmdToggle(Player player, String[] arguments) {
        if (TimingSystem.getPlugin().verbose.contains(player.getUniqueId())) {
            TimingSystem.getPlugin().verbose.remove(player.getUniqueId());
            plugin.sendMessage(player, "messages.toggle.race.checkpointsOff");
        } else {
            TimingSystem.getPlugin().verbose.add(player.getUniqueId());
            plugin.sendMessage(player, "messages.toggle.race.checkpointsOn");
        }
    }








}
