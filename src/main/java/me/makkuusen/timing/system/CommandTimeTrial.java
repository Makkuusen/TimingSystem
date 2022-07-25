package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.gui.GUITrack;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.entity.Player;

@CommandAlias("timetrial|tt")
public class CommandTimeTrial extends BaseCommand {
    static TimingSystem plugin;

    @Default
    @CommandCompletion("@track")
    public static void onTimeTrial(Player player, @Optional Track track) {
        if (track == null) {
            GUITrack.openTrackGUI(player);
        } else {
            player.teleport(track.getSpawnLocation());
        }
    }

    @Subcommand("cancel|c")
    public static void onCancel(Player player) {
        if (!TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
            plugin.sendMessage(player, "messages.error.runNotStarted");
            return;
        }
        TimeTrialController.playerCancelMap(player);
        plugin.sendMessage(player, "messages.cancel");
    }

    @Subcommand("verbose")
    public static void onVerbose(Player player) {
        if (TimingSystem.getPlugin().verbose.contains(player.getUniqueId())) {
            TimingSystem.getPlugin().verbose.remove(player.getUniqueId());
            plugin.sendMessage(player, "messages.toggle.race.checkpointsOff");
        } else {
            TimingSystem.getPlugin().verbose.add(player.getUniqueId());
            plugin.sendMessage(player, "messages.toggle.race.checkpointsOn");
        }
    }

    @Subcommand("help")
    public static void onHelp(Player player) {
        player.sendMessage("");
        plugin.sendMessage(player, "messages.help", "%command%", "race");
        player.sendMessage("§2/tt");
        player.sendMessage("§2/tt cancel");
        player.sendMessage("§2/tt verbose");
    }
}
