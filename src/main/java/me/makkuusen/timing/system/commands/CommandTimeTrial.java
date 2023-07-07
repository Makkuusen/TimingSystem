package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.gui.TimeTrialGui;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@CommandAlias("timetrial|tt")
public class CommandTimeTrial extends BaseCommand {
    public static TimingSystem plugin;

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

        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().isRunning()) {
                player.sendMessage("§cYou can't time trial when you are in a heat.");
                return;
            }
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
            ApiUtilities.teleportPlayerAndSpawnBoat(player, track, track.getSpawnLocation());
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

    public static void onRandom(Player player) {
        onRandom(player, null);
    }

    @Subcommand("random|r")
    @CommandCompletion("@trackTag")
    public static void onRandom(Player player, @Optional TrackTag trackTag) {
        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().isRunning()) {
                player.sendMessage("§cYou can't time trial when you are in a heat.");
                return;
            }
        }

        if (TrackDatabase.getOpenTracks().isEmpty()) {
            plugin.sendMessage(player, "messages.randomTrack.noTracks");
            return;
        }

        List<Track> tracks;

        if (trackTag != null) {
            tracks = TrackDatabase.getOpenTracks().stream().filter(track -> track.hasTag(trackTag)).collect(Collectors.toList());
            if (tracks.size() < 1) {
                player.sendMessage("§cThere are no tracks with that filter");
                return;
            }
        } else {
            tracks = TrackDatabase.getOpenTracks();
        }

        Track t = tracks.get(new Random().nextInt(tracks.size()));

        if (!t.getSpawnLocation().isWorldLoaded()) {
            player.sendMessage("§cWorld is not loaded!");
            return;
        }

        if (t.getPlayerTopListPosition(Database.getPlayer(player.getUniqueId())) != -1) {
            plugin.sendMessage(player, "messages.randomTrack.teleport", "%track%", t.getDisplayName(), "%pos%", String.valueOf(t.getPlayerTopListPosition(Database.getPlayer(player.getUniqueId()))));
        } else {
            plugin.sendMessage(player, "messages.randomTrack.teleportNoPos", "%track%", t.getDisplayName());
        }

        ApiUtilities.teleportPlayerAndSpawnBoat(player, t, t.getSpawnLocation());
    }

    @Subcommand("randomunfinished")
    @CommandCompletion("@trackTag")
    public static void onRandomUnfinished(Player player, @Optional TrackTag trackTag) {
        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().isRunning()) {
                player.sendMessage("§cYou can't time trial when you are in a heat.");
                return;
            }
        }

        if (TrackDatabase.getOpenTracks().isEmpty()) {
            plugin.sendMessage(player, "messages.randomTrack.noTracks");
            return;
        }

        List<Track> tracks;
        if (trackTag != null) {
            tracks = TrackDatabase.getOpenTracks().stream().filter(track -> track.hasTag(trackTag)).collect(Collectors.toList());
            if (tracks.size() < 1) {
                player.sendMessage("§cThere are no tracks with that filter");
                return;
            }
        } else {
            tracks = TrackDatabase.getOpenTracks();
        }

        tracks = tracks.stream().filter(track -> track.getPlayerTotalFinishes(Database.getPlayer(player.getUniqueId())) < 1).collect(Collectors.toList());
        if (tracks.size() == 0) {
            player.sendMessage("§cYou have already completed all tracks");
            return;
        }

        Track t = tracks.get(new Random().nextInt(tracks.size()));

        if (!t.getSpawnLocation().isWorldLoaded()) {
            player.sendMessage("§cWorld is not loaded!");
            return;
        }

        if (t.getPlayerTopListPosition(Database.getPlayer(player.getUniqueId())) != -1) {
            plugin.sendMessage(player, "messages.randomTrack.teleport", "%track%", t.getDisplayName(), "%pos%", String.valueOf(t.getPlayerTopListPosition(Database.getPlayer(player.getUniqueId()))));
        } else {
            plugin.sendMessage(player, "messages.randomTrack.teleportNoPos", "%track%", t.getDisplayName());
        }

        ApiUtilities.teleportPlayerAndSpawnBoat(player, t, t.getSpawnLocation());
    }


}