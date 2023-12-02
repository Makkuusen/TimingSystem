package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackExchangeTrack;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

@CommandAlias("trackexchange|te")
public class CommandTrackExchange extends BaseCommand {

    @Subcommand("cut")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrackexchange_cut")
    public static void onCutTrack(Player player, Track track) {
        TimingSystem.newChain().async(() -> {
            Text.send(player, Info.TRACK_EXCHANGE_SAVING, "%trackfile%", track.getCommandName().toLowerCase() + ".zip");
            try {
                Clipboard playerClip = TrackExchangeTrack.makeSchematicFile(player);
                TrackExchangeTrack te = new TrackExchangeTrack(track, playerClip);
                te.writeTrackToFile(player.getLocation());

                clearTrackBlocks(player, playerClip);

            } catch (FileAlreadyExistsException e) {
                Text.send(player, Error.TRACK_EXISTS);
            } catch (IOException e) {
                e.printStackTrace();
                Text.send(player, Error.TRACK_EXCHANGE_SAVE_FAILED, "%reason%", "File Creation Failed");
            }
        }).execute(finished -> {
            TrackDatabase.removeTrack(track);
            Text.send(player, Success.CREATED);
        });
    }

    @Subcommand("copy")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrackexchange_copy")
    public static void onCopyTrack(Player player, Track track) {
        TimingSystem.newChain().async(() -> {
            Text.send(player, Info.TRACK_EXCHANGE_SAVING, "%trackfile%", track.getCommandName().toLowerCase() + ".zip");
            try {
                Clipboard clip = TrackExchangeTrack.makeSchematicFile(player);
                TrackExchangeTrack te = new TrackExchangeTrack(track, clip);
                te.writeTrackToFile(player.getLocation());
            } catch (IOException e) {
                e.printStackTrace();
                Text.send(player, Error.TRACK_EXCHANGE_SAVE_FAILED, "%reason%", "File Creation Failed");
            }
        }).execute(finished -> Text.send(player, Success.CREATED));
    }

    @Subcommand("paste")
    @CommandCompletion("<filename> [newname]")
    @CommandPermission("%permissiontrackexchange_paste")
    public static void onPasteTrack(Player player, String fileName, @Optional String newName) {
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

            if (TrackDatabase.trackNameNotAvailable(newName)) {
                Text.send(player, Error.TRACK_EXISTS);
                return;
            }
        }

        if(fileName.matches(".+\\.zip")) fileName = fileName.substring(0, fileName.length() - 4);

        String fName = fileName;
        TimingSystem.newChain().async(() -> {
            String name = newName == null ? fName : newName;
            Text.send(player, Info.TRACK_EXCHANGE_PASTING, "%trackfile%", fName + ".zip", "%track%", name);
            TrackExchangeTrack trackExchangeTrack;
            try {
                trackExchangeTrack = TrackExchangeTrack.readFile(player, fName, name);

                if(trackExchangeTrack == null) {
                    Text.send(player, Error.TRACK_EXCHANGE_PASTE_FAILED, "%reason%", "Track failed to be created");
                    return;
                }

                trackExchangeTrack.pasteTrackSchematic(player);

                Text.send(player, Success.CREATED_TRACK, "%track%", trackExchangeTrack.getTrack().getDisplayName());
            } catch (IOException e) {
                e.printStackTrace();
                Text.send(player, Error.TRACK_EXCHANGE_PASTE_FAILED, "%reason%", "Track file could not be read");
            }
        }).execute();
    }

    private static void clearTrackBlocks(Player player, Clipboard clipboard) {
        if(clipboard == null) {
            player.sendMessage(Component.text("No selection detected; saving track without cut.").color(Theme.getTheme(player).getWarning()));
            return;
        }

        TimingSystem.newChain().async(() -> {
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
                editSession.setBlocks(clipboard.getRegion(), BukkitAdapter.adapt(Material.AIR.createBlockData()));
            } catch (MaxChangedBlocksException e) {
                Text.send(player, Error.GENERIC);
            }
        }).execute();
    }
}
