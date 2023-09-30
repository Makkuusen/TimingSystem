package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.permissions.PermissionTrackExchange;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.Warning;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackExchangeTrack;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

@CommandAlias("trackexchange|te")
public class CommandTrackExchange extends BaseCommand {

    @Subcommand("cut")
    @CommandCompletion("@track")
    public static void onCutTrack(Player player, Track track) {
        if(!player.hasPermission(PermissionTrackExchange.CUT.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        BlockArrayClipboard clipboard = null;
        try {
            Region r = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection();
            clipboard = new BlockArrayClipboard(r);
            Operations.complete(new ForwardExtentCopy(BukkitAdapter.adapt(player.getWorld()), r, clipboard, r.getMinimumPoint()));
        } catch (IncompleteRegionException e) {
            player.sendMessage(Component.text("No WorldEdit selection detected, saving track without schematic").color(Theme.getTheme(player).getWarning()));
        } catch (WorldEditException e) {
            Text.send(player, Error.GENERIC);
            return;
        }

        TrackDatabase.removeTrack(track);
        try {
            new TrackExchangeTrack(track, clipboard).writeToFile(player.getLocation());
        } catch (FileAlreadyExistsException e) {
            Text.send(player, Error.TRACK_EXISTS);
            return;
        } catch (IOException e) {
            e.printStackTrace();
            Text.send(player, Error.GENERIC);
            return;
        }

        if(clipboard != null) {
            try(EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
                editSession.setBlocks(clipboard.getRegion(), BukkitAdapter.adapt(Material.AIR.createBlockData()));
            } catch (MaxChangedBlocksException e) {
                Text.send(player, Error.GENERIC);
                return;
            }
        } else {
            player.sendMessage(Component.text("No selection detected; saving track without cut").color(Theme.getTheme(player).getWarning()));
        }

        Text.send(player, Success.CREATED);
    }

    @Subcommand("copy")
    @CommandCompletion("@track")
    public static void onCopyTrack(Player player, Track track) {
        if(!player.hasPermission(PermissionTrackExchange.COPY.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        BlockArrayClipboard clipboard = null;
        try {
            Region r = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection();
            clipboard = new BlockArrayClipboard(r);
            Operations.complete(new ForwardExtentCopy(BukkitAdapter.adapt(player.getWorld()), r, clipboard, r.getMinimumPoint()));
        } catch (IncompleteRegionException e) {
            player.sendMessage(Component.text("No WorldEdit selection detected; saving track without schematic").color(Theme.getTheme(player).getWarning()));
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
    @CommandCompletion("trackfile <newname>")
    public static void onPasteTrack(Player player, String fileName, @Optional String newName) {
        if(!player.hasPermission(PermissionTrackExchange.PASTE.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }

        if(newName != null) {
            int maxLength = 25;
            if (newName.length() > maxLength) {
                Text.send(player, Error.LENGTH_EXCEEDED, "%length%", String.valueOf(maxLength));
                return;
            }

            if (!newName.matches("[A-Za-z0-9 ]+")) {
                Text.send(player, Error.NAME_FORMAT);
                return;
            }

            if (ApiUtilities.checkTrackName(newName)) {
                Text.send(player, Error.INVALID_TRACK_NAME);
                return;
            }

            if (!TrackDatabase.trackNameAvailable(newName)) {
                Text.send(player, Error.TRACK_EXISTS);
                return;
            }
        }

        if(fileName.matches(".+\\.zip")) fileName = fileName.substring(0, fileName.length() - 4);

        try(EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            TrackExchangeTrack trackExchangeTrack;
            if(newName == null) trackExchangeTrack = new TrackExchangeTrack().readFile(player, fileName);
            else trackExchangeTrack = new TrackExchangeTrack(newName).readFile(player, fileName);

            if(trackExchangeTrack == null) {
                Text.send(player, Error.GENERIC);
                return;
            }

            if(!trackExchangeTrack.getVersion().equals(TrackExchangeTrack.CURRENT_VERSION)) {
                Text.send(player, Warning.DANGEROUS_COMMAND, "%command%", "/trackexchange pasteoutdated");
                return;
            }

            if(trackExchangeTrack.getClipboard() != null) {
                Operations.complete(new ClipboardHolder(trackExchangeTrack.getClipboard()).createPaste(editSession).to(BlockVector3.at(player.getLocation().x() + trackExchangeTrack.getClipboardOffset().getX(), player.getLocation().y() + trackExchangeTrack.getClipboardOffset().getY(), player.getLocation().z() + trackExchangeTrack.getClipboardOffset().getZ())).copyEntities(true).build());
                player.sendMessage(Component.text("Loading without schematic").color(Theme.getTheme(player).getWarning()));
            }
            Text.send(player, Success.CREATED_TRACK, "%track%", trackExchangeTrack.getTrack().getDisplayName());
        } catch (FileNotFoundException e) {
            Text.send(player, Error.TRACKS_NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            Text.send(player, Error.GENERIC);
        }
    }

    @Subcommand("pasteoutdated")
    @CommandCompletion("trackfile <newname>")
    public static void onPasteOutdated(Player player, String fileName, @Optional String newName) {
        if(!player.hasPermission(PermissionTrackExchange.PASTEOUTDATED.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        if(newName != null) {
            int maxLength = 25;
            if (newName.length() > maxLength) {
                Text.send(player, Error.LENGTH_EXCEEDED, "%length%", String.valueOf(maxLength));
                return;
            }

            if (!newName.matches("[A-Za-z0-9 ]+")) {
                Text.send(player, Error.NAME_FORMAT);
                return;
            }

            if (ApiUtilities.checkTrackName(newName)) {
                Text.send(player, Error.INVALID_TRACK_NAME);
                return;
            }

            if (!TrackDatabase.trackNameAvailable(newName)) {
                Text.send(player, Error.TRACK_EXISTS);
                return;
            }
        }

        if(fileName.matches(".+\\.zip")) fileName = fileName.substring(0, fileName.length() - 4);

        try(EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            TrackExchangeTrack trackExchangeTrack;
            if(newName == null) trackExchangeTrack = new TrackExchangeTrack().readFile(player, fileName);
            else trackExchangeTrack = new TrackExchangeTrack(newName).readFile(player, fileName);

            if(trackExchangeTrack == null) {
                Text.send(player, Error.GENERIC);
                return;
            }

            if(trackExchangeTrack.getClipboard() != null) {
                Operations.complete(new ClipboardHolder(trackExchangeTrack.getClipboard()).createPaste(editSession).to(BlockVector3.at(player.getLocation().x() + trackExchangeTrack.getClipboardOffset().getX(), player.getLocation().y() + trackExchangeTrack.getClipboardOffset().getY(), player.getLocation().z() + trackExchangeTrack.getClipboardOffset().getZ())).copyEntities(true).build());
                player.sendMessage(Component.text("Loading without schematic").color(Theme.getTheme(player).getWarning()));
            }
            Text.send(player, Success.CREATED_TRACK, "%track%", trackExchangeTrack.getTrack().getDisplayName());
        } catch (FileNotFoundException e) {
            Text.send(player, Error.TRACKS_NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            Text.send(player, Error.GENERIC);
        }
    }
}
