package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.*;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Message;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.editor.TrackEditor;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import me.makkuusen.timing.system.track.options.TrackOption;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import me.makkuusen.timing.system.track.tags.TrackTag;
import org.bukkit.entity.Player;

@CommandAlias("trackedit|te")

public class CommandTrackEdit extends BaseCommand {

    @Subcommand("select|sel")
    @CommandPermission("%permissiontrackedit_select")
    public static void onSelectTrack(Player player, @Optional Track track)  {
        if (track == null) {
            var maybeTrack = ApiUtilities.findClosestTrack(player);
            if (maybeTrack.isPresent()) {
                track = maybeTrack.get();
            } else {
                Text.send(player, Error.TRACKS_NOT_FOUND);
                return;
            }
        }
        TrackEditor.setPlayerTrackSelection(player.getUniqueId(), track);
        Text.send(player, Success.SAVED);
    }

    @Subcommand("view")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrackedit_view")
    public static void onView(Player player, @Optional Track track) {
        var response = TrackEditor.setView(player, track);
        player.sendMessage(response);
    }


    @Subcommand("create")
    @CommandCompletion("@trackType name")
    @CommandPermission("%permissiontrackedit_create")
    public static void onCreate(Player player, Track.TrackType trackType, String name) {
        var response = TrackEditor.createTrack(player, trackType, name);
        player.sendMessage(response);
    }

    @Subcommand("delete")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrackedit_delete_track")
    public static void onDelete(Player player, @Optional Track track) {
        var response = TrackEditor.deleteTrack(player, track);
        player.sendMessage(response);
    }

    @Subcommand("move")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrackedit_move")
    public static void onMove(Player player, @Optional Track track) {
        var response = TrackEditor.moveTrack(player, track);
        player.sendMessage(response);
    }

    @Subcommand("name")
    @CommandCompletion("name @track")
    @CommandPermission("%permissiontrackedit_name")
    public static void onName(Player player, String name, @Optional Track track) {
        var response = TrackEditor.setName(player, name, track);
        player.sendMessage(response);
    }

    @Subcommand("open")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrackedit_open")
    public static void onOpen(Player player, @Optional Track track) {
        Message response = TrackEditor.setOpen(player, true, track);
        Text.send(player, response);
    }

    @Subcommand("close")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrackedit_close")
    public static void onClose(Player player, @Optional Track track) {
        Message response = TrackEditor.setOpen(player, false, track);
        Text.send(player, response);
    }

    @Subcommand("weight")
    @CommandCompletion("<value> @track")
    @CommandPermission("%permissiontrackedit_weight")
    public static void onWeight(Player player, int weight, @Optional Track track) {
        Message response = TrackEditor.setWeight(player, weight, track);
        Text.send(player, response);
    }

    @Subcommand("tag")
    @CommandCompletion("+/- @trackTag @track")
    @CommandPermission("%permissiontrackedit_tag")
    public static void onTag(Player player, String plusOrMinus, TrackTag tag, @Optional Track track) {
        if (!TrackTagManager.hasTag(tag)) {
            Text.send(player, Error.TAG_NOT_FOUND);
            return;
        }
        Message response;
        if (plusOrMinus.equalsIgnoreCase("-")) {
            response = TrackEditor.removeTag(player, tag, track);
        } else {
            response = TrackEditor.addTag(player,tag, track);
        }
        Text.send(player, response);
    }

    @Subcommand("option")
    @CommandCompletion("+/- @trackOption @track")
    @CommandPermission("%permissiontrackedit_option")
    public static void onOption(Player player, String plusOrMinus, TrackOption option, @Optional Track track) {
        Message response;
        if (plusOrMinus.equalsIgnoreCase("-")) {
            response = TrackEditor.removeOption(player, option, track);
        } else {
            response = TrackEditor.addOption(player, option, track);
        }
        Text.send(player, response);
    }

    @Subcommand("type")
    @CommandCompletion("@trackType @track")
    @CommandPermission("%permissiontrackedit_type")
    public static void onType(Player player, Track.TrackType type, @Optional Track track) {
        Message response = TrackEditor.setTrackType(player, type, track);
        Text.send(player, response);
    }

    @Subcommand("boatutils")
    @CommandCompletion("@allBoatUtilsMode @track")
    @CommandPermission("%permissiontrackedit_boatutilsmode")
    public static void onMode(Player player, BoatUtilsMode mode, @Optional Track track) {
        Message response = TrackEditor.setBoatUtilsMode(player, mode, track);
        Text.send(player, response);
    }

    @Subcommand("regionspawn")
    @CommandCompletion("@region")
    @CommandPermission("%permissiontrackedit_regionspawn")
    public static void onRegionSpawn(Player player, TrackRegion region) {
        region.setSpawn(player.getLocation());
        Text.send(player, Success.SAVED);
    }

    @Subcommand("spawn")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrackedit_spawn")
    public static void onRegionSpawn(Player player, @Optional Track track) {
        Message response = TrackEditor.setSpawn(player, track);
        Text.send(player, response);
    }

    @Subcommand("location | loc")
    @CommandCompletion("@locationType <index>")
    @CommandPermission("%permissiontrackedit_location")
    public static void onLocation(Player player, TrackLocation.Type locationType, @Optional String index) {
        var response = TrackEditor.createOrUpdateLocation(player, locationType, index);
        player.sendMessage(response);
    }

    @Subcommand("region | rg")
    @CommandCompletion("@regionType <index> overload")
    @CommandPermission("%permissiontrackedit_region")
    public static void onRegion(Player player, TrackRegion.RegionType regionType, @Optional String index) {
        var response = TrackEditor.createOrUpdateRegion(player, regionType, index, false);
        player.sendMessage(response);
    }

    @Subcommand("overload checkpoint")
    @CommandCompletion("<index>")
    @CommandPermission("%permissiontrackedit_overload")
    public static void onOverload(Player player, String index) {
        var response = TrackEditor.createOrUpdateRegion(player, TrackRegion.RegionType.CHECKPOINT, index, true);
        player.sendMessage(response);
    }

    @Subcommand("item")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrackedit_item")
    public static void onItem(Player player, Track track) {
        var response = TrackEditor.setItem(player, track);
        Text.send(player, response);
    }

    @Subcommand("owner")
    @CommandCompletion("<player> @track")
    @CommandPermission("%permissiontrackedit_owner")
    public static void onOwner(Player player, String name, @Optional Track track) {
        var response = TrackEditor.setOwner(player, name, track);
        Text.send(player, response);
    }

    @Subcommand("contributors add")
    @CommandCompletion("<player> @track")
    @CommandPermission("%permissiontrackedit_contributors")
    public static void onAddContributor(Player player, String name, @Optional Track track) {
        var response = TrackEditor.addContributor(player, name, track);
        Text.send(player, response);
    }

    @Subcommand("contributors remove")
    @CommandCompletion("<player> @track")
    @CommandPermission("%permissiontrackedit_contributors")
    public static void onRemoveContributor(Player player, String name, @Optional Track track) {
        var response = TrackEditor.removeContributor(player, name, track);
        Text.send(player, response);
    }
}
