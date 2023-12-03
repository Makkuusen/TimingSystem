package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.database.Database;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.gui.TimeTrialGui;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.Component;
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

    @Default
    @CommandCompletion("@track")
    @CommandPermission("%permissiontimetrial_menu")
    public static void onTimeTrial(CommandSender sender, @Optional Track track) {
        Player player = null;
        if (sender instanceof BlockCommandSender blockCommandSender) {
            Location location = blockCommandSender.getBlock().getLocation();
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
        } else if (sender instanceof Player p) {
            player = p;
        } else {
            Text.send(sender, Error.ONLY_PLAYERS);
            return;
        }

        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().isRunning()) {
                Text.send(player, Error.NOT_NOW);
                return;
            }
        }

        if (track == null) {
            var tPlayer = Database.getPlayer(player.getUniqueId());
            new TimeTrialGui(tPlayer).show(player);
        } else {
            if (!track.getSpawnLocation().isWorldLoaded()) {
                Text.send(player, Error.WORLD_NOT_LOADED);
                return;
            }

            if (!track.isOpen() && !(player.hasPermission("timingsystem.packs.trackadmin"))) {
                Text.send(player, Error.TRACK_IS_CLOSED);
                return;
            }
            ApiUtilities.teleportPlayerAndSpawnBoat(player, track, track.getSpawnLocation());
        }
    }

    @Subcommand("cancel|c")
    @CommandPermission("%permissiontimetrial_cancel")
    public static void onCancel(Player player) {
        if (!TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
            Text.send(player, Error.NOT_NOW);
            return;
        }
        TimeTrialController.playerCancelMap(player);
        Text.send(player, Success.TIME_TRIAL_CANCELLED);
    }

    public static void onRandom(Player player) {
        onRandom(player, null);
    }

    @Subcommand("random|r")
    @CommandCompletion("@trackTag")
    @CommandPermission("%permissiontimetrial_random")
    public static void onRandom(Player player, @Optional TrackTag trackTag) {
        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().isRunning()) {
                Text.send(player, Error.NOT_NOW);
                return;
            }
        }

        if (TrackDatabase.getOpenTracks().isEmpty()) {
            Text.send(player, Error.TRACKS_NOT_FOUND);
            return;
        }

        List<Track> tracks;

        if (trackTag != null) {
            tracks = TrackDatabase.getOpenTracks().stream().filter(track -> track.hasTag(trackTag)).collect(Collectors.toList());
            if (tracks.isEmpty()) {
                Text.send(player, Error.TRACKS_NOT_FOUND);
                return;
            }
        } else {
            tracks = TrackDatabase.getOpenTracks();
        }
        teleportPlayerToRandomTrack(tracks, player);
    }

    @Subcommand("randomunfinished")
    @CommandCompletion("@trackTag")
    @CommandPermission("%permissiontimetrial_random")
    public static void onRandomUnfinished(Player player, @Optional TrackTag trackTag) {
        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().isRunning()) {
                Text.send(player, Error.NOT_NOW);
                return;
            }
        }

        if (TrackDatabase.getOpenTracks().isEmpty()) {
            Text.send(player, Error.TRACKS_NOT_FOUND);
            return;
        }

        List<Track> tracks;
        if (trackTag != null) {
            tracks = TrackDatabase.getOpenTracks().stream().filter(track -> track.hasTag(trackTag)).collect(Collectors.toList());
            if (tracks.isEmpty()) {
                Text.send(player, Error.TRACKS_NOT_FOUND);
                return;
            }
        } else {
            tracks = TrackDatabase.getOpenTracks();
        }

        tracks = tracks.stream().filter(track -> track.getPlayerTotalFinishes(Database.getPlayer(player.getUniqueId())) < 1).collect(Collectors.toList());
        if (tracks.isEmpty()) {
            Text.send(player, Error.TRACKS_NOT_FOUND);
            return;
        }

        teleportPlayerToRandomTrack(tracks, player);
    }

    private static void teleportPlayerToRandomTrack(List<Track> tracks, Player player) {
        Track track = tracks.get(new Random().nextInt(tracks.size()));

        if (!track.getSpawnLocation().isWorldLoaded()) {
            Text.send(player, Error.WORLD_NOT_LOADED);
            return;
        }
        TPlayer tPlayer = Database.getPlayer(player.getUniqueId());

        if (track.getCachedPlayerPosition(tPlayer) != -1) {
            Component message = Text.get(player, Success.TELEPORT_TO_TRACK, "%track%", track.getDisplayName());
            var leaderboardPosition = track.getCachedPlayerPosition(tPlayer);
            Component positionComponent = tPlayer.getTheme().getParenthesized(String.valueOf(leaderboardPosition));
            if (message != null) {
                player.sendMessage(message.append(Component.space()).append(positionComponent));
            }
        } else {
            Text.send(player, Success.TELEPORT_TO_TRACK, "%track%", track.getDisplayName());
        }

        ApiUtilities.teleportPlayerAndSpawnBoat(player, track, track.getSpawnLocation());
    }
}