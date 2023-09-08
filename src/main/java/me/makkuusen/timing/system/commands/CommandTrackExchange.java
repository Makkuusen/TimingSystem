package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackExchangeTrack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.IOException;

@CommandAlias("trackexchange|te")
@CommandPermission("track.admin")
public class CommandTrackExchange extends BaseCommand {

    @Subcommand("cut")
    @CommandCompletion("@track")
    public static void onCutTrack(Player player, Track track) {
        BlockArrayClipboard clipboard;
        try {
            Region r = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection();
            clipboard = new BlockArrayClipboard(r);
            Operations.complete(new ForwardExtentCopy(BukkitAdapter.adapt(player.getWorld()), r, clipboard, r.getMinimumPoint()));
        } catch (IncompleteRegionException e) {
            Text.send(player, Error.SELECTION);
            return;
        } catch (WorldEditException e) {
            Text.send(player, Error.GENERIC);
            return;
        }

        TrackDatabase.removeTrack(track);
        try {
            new TrackExchangeTrack(track, clipboard).writeToFile(player.getLocation());
        } catch (IOException e) {
            e.printStackTrace();
            Text.send(player, Error.GENERIC);
            return;
        }

        try(EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            editSession.setBlocks(clipboard.getRegion(), BukkitAdapter.adapt(Material.AIR.createBlockData()));
        } catch (MaxChangedBlocksException e) {
            Text.send(player, Error.GENERIC);
            return;
        }

        Text.send(player, Success.CREATED);
    }

    @Subcommand("copy")
    @CommandCompletion("@track")
    public static void onCopyTrack(Player player, Track track) {
        BlockArrayClipboard clipboard;
        try {
            Region r = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection();
            clipboard = new BlockArrayClipboard(r);
            Operations.complete(new ForwardExtentCopy(BukkitAdapter.adapt(player.getWorld()), r, clipboard, r.getMinimumPoint()));
        } catch (IncompleteRegionException e) {
            Text.send(player, Error.SELECTION);
            return;
        } catch (WorldEditException e) {
            Text.send(player, Error.GENERIC);
            return;
        }

        try {
            new TrackExchangeTrack(track, clipboard).writeToFile(player.getLocation());
        } catch (IOException e) {
            e.printStackTrace();
            Text.send(player, Error.GENERIC);
            return;
        }
        Text.send(player, Success.CREATED);
    }

    @Subcommand("paste")
    @CommandCompletion("trackfile")
    public static void onPasteTrack(Player player, String fileName) {
        if(fileName.matches(".+\\.zip")) fileName = fileName.substring(0, fileName.length() - 4);

        try(EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            TrackExchangeTrack trackExchangeTrack = new TrackExchangeTrack().readFile(player, fileName);

            Operations.complete(new ClipboardHolder(trackExchangeTrack.getClipboard()).createPaste(editSession).to(BlockVector3.at(player.getLocation().x() + trackExchangeTrack.getClipboardOffset().getX(), player.getLocation().y() + trackExchangeTrack.getClipboardOffset().getY(), player.getLocation().z() + trackExchangeTrack.getClipboardOffset().getZ())).copyEntities(true).build());
            Text.send(player, Success.CREATED_TRACK, "%track%", trackExchangeTrack.getTrack().getDisplayName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subcommand("pasteas")
    @CommandCompletion("oldname newname")
    public static void onPasteAs(Player player, String fileName, String newTrackName) {
        if(fileName.matches(".+\\.zip")) fileName = fileName.substring(0, fileName.length() - 4);

        int maxLength = 25;
        if (newTrackName.length() > maxLength) {
            Text.send(player, Error.LENGTH_EXCEEDED, "%length%", String.valueOf(maxLength));
            return;
        }

        if (!newTrackName.matches("[A-Za-z0-9 ]+")) {
            Text.send(player, Error.NAME_FORMAT);
            return;
        }

        if (ApiUtilities.checkTrackName(newTrackName)) {
            Text.send(player, Error.INVALID_TRACK_NAME);
            return;
        }

        if (!TrackDatabase.trackNameAvailable(newTrackName)) {
            Text.send(player, Error.TRACK_EXISTS);
            return;
        }

        try(EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            TrackExchangeTrack trackExchangeTrack = new TrackExchangeTrack(newTrackName).readFile(player, fileName);

            Operations.complete(new ClipboardHolder(trackExchangeTrack.getClipboard()).createPaste(editSession).to(BlockVector3.at(player.getLocation().x() + trackExchangeTrack.getClipboardOffset().getX(), player.getLocation().y() + trackExchangeTrack.getClipboardOffset().getY(), player.getLocation().z() + trackExchangeTrack.getClipboardOffset().getZ())).copyEntities(true).build());
            Text.send(player, Success.CREATED_TRACK, "%track%", trackExchangeTrack.getTrack().getDisplayName());
        } catch (Exception e) {
            e.printStackTrace();
            Text.send(player, Error.GENERIC);
        }
    }
}
