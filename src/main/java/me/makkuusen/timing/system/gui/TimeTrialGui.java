package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.text.Error;
import me.makkuusen.timing.system.text.Gui;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

public class TimeTrialGui extends TrackPageGui {

    public TimeTrialGui(TPlayer tPlayer, int page) {
        super(tPlayer, TimingSystem.getPlugin().getText(tPlayer.getPlayer(), Gui.TRACKS_TITLE), page);
    }

    public TimeTrialGui(TPlayer tPlayer, Component title, int page, TrackSort trackSort, TrackFilter filter, Track.TrackType trackType) {
        super(tPlayer, title, page, trackSort, filter, trackType);
    }

    public List<Track> getTracks() {
        return TrackDatabase.getTracks();
    }

    @Override
    public GuiButton getTrackButton(Player player, Track track) {
        var button = new GuiButton(track.getGuiItem(player.getUniqueId()));
        button.setAction(() -> {
            if (!track.getSpawnLocation().isWorldLoaded()) {
                TimingSystem.getPlugin().sendMessage(player, Error.WORLD_NOT_LOADED);
                return;
            }
            ApiUtilities.teleportPlayerAndSpawnBoat(player, track, track.getSpawnLocation());
            player.closeInventory();
        });
        return button;
    }
}
