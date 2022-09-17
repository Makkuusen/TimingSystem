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
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.util.Random;

@CommandAlias("timetrial|tt")
public class CommandTimeTrial extends BaseCommand {
    static TimingSystem plugin;

    @Default
    @CommandCompletion("@track")
    public static void onTimeTrial(Player player, @Optional Track track) {
        if (track == null) {
            GUITrack.openTrackGUI(player);
        } else {
            if (!track.getSpawnLocation().isWorldLoaded()) {
                player.sendMessage("§cWorld is not loaded!");
                return;
            }
            track.getSpawnLocation().setPitch(player.getLocation().getPitch());
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

    @Subcommand("boat")
    @CommandCompletion("@boat")
    public static void onBoat(Player player, TreeSpecies treeSpecies){
        TPlayer tPlayer = Database.getPlayer(player.getUniqueId());
        tPlayer.setBoat(treeSpecies);
        if (player.getVehicle() instanceof Boat boat) {
            boat.setWoodType(treeSpecies);
        }
        plugin.sendMessage(player, "messages.save.generic");
    }

    @Subcommand("help")
    public static void onHelp(Player player) {
        player.sendMessage("");
        plugin.sendMessage(player, "messages.help", "%command%", "race");
        player.sendMessage("§2/tt");
        player.sendMessage("§2/tt cancel");
        player.sendMessage("§2/tt verbose");
    }

    @Subcommand("random|r")
    public static void onRandom(Player player){
        if(TrackDatabase.getOpenTracks().isEmpty()){
            player.sendMessage(plugin.getLocalizedMessage(player, "messages.randomTrack.noTracks"));
            return;
        }

        Track t = TrackDatabase.getOpenTracks().get(new Random().nextInt(TrackDatabase.getOpenTracks().size()));

        if (!t.getSpawnLocation().isWorldLoaded()) {
            player.sendMessage("§cWorld is not loaded!");
            return;
        }
        if(t.getPlayerTopListPosition(Database.getPlayer(player.getUniqueId())) != -1){
            player.sendMessage(plugin.getLocalizedMessage(player, "messages.timer.randomTrack", "%track%", t.getDisplayName(),
                    "%pos%", String.valueOf(t.getPlayerTopListPosition(Database.getPlayer(player.getUniqueId())))
            ));
        } else {
            player.sendMessage(plugin.getLocalizedMessage(player, "messages.timer.randomTrackNoPos", "%track%", t.getDisplayName()));
        }

        t.getSpawnLocation().setPitch(player.getLocation().getPitch());
        player.teleport(t.getSpawnLocation());
    }
}