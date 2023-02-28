package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.gui.TimeTrialGui;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Random;

@CommandAlias("timetrial|tt")
public class CommandTimeTrial extends BaseCommand {
    static TimingSystem plugin;

    @Default
    @CommandCompletion("@track")
    public static void onTimeTrial(CommandSender sender, @Optional Track track) {
        Player player = null;
        if (sender instanceof BlockCommandSender blockCommandSender) {
            Location location = ((BlockCommandSender) sender).getBlock().getLocation();
            double closest = -1;

            for (Player tmp : Bukkit.getOnlinePlayers()) {
                if (tmp.getWorld().equals(location.getWorld())) {

                    double distance = tmp.getLocation().distance(location);

                    if (distance < closest || closest == -1) {
                        player = tmp;
                        closest = distance;
                    }
                }
            }

            if (player == null) {
                return;
            }
        } else if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            sender.sendMessage("This command could not be executed!");
            return;
        }
        if (track == null) {
            var tPlayer = Database.getPlayer(player.getUniqueId());
            new TimeTrialGui(tPlayer, 0).show(player);
        } else {
            if (!track.getSpawnLocation().isWorldLoaded()) {
                player.sendMessage("§cWorld is not loaded!");
                return;
            }

            if (!track.isOpen() && !(player.isOp() || player.hasPermission("track.admin"))) {
                player.sendMessage("§cTrack is closed!");
                return;
            }
            ApiUtilities.teleportPlayerAndSpawnBoat(player,track.isBoatTrack(), track.getSpawnLocation());
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

    @Subcommand("random|r")
    public static void onRandom(Player player){
        if(TrackDatabase.getOpenTracks().isEmpty()){
            plugin.sendMessage(player, "messages.randomTrack.noTracks");
            return;
        }

        Track t = TrackDatabase.getOpenTracks().get(new Random().nextInt(TrackDatabase.getOpenTracks().size()));

        if (!t.getSpawnLocation().isWorldLoaded()) {
            player.sendMessage("§cWorld is not loaded!");
            return;
        }

        if(t.getPlayerTopListPosition(Database.getPlayer(player.getUniqueId())) != -1){
            plugin.sendMessage(player, "messages.randomTrack.teleport", "%track%", t.getDisplayName(),
                    "%pos%", String.valueOf(t.getPlayerTopListPosition(Database.getPlayer(player.getUniqueId())))
            );
        } else {
            plugin.sendMessage(player, "messages.randomTrack.teleportNoPos", "%track%", t.getDisplayName());
        }

        ApiUtilities.teleportPlayerAndSpawnBoat(player, t.isBoatTrack(), t.getSpawnLocation());
    }
}