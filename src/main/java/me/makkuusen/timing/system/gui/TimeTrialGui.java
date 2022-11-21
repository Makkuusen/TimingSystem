package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TimeTrialGui extends TrackPageGui{

    public TimeTrialGui(TPlayer tPlayer, int page) {
        super(tPlayer,"§2§lTracks", 6, page);
    }

    public TimeTrialGui(TPlayer tPlayer, int page, TrackSort trackSort) {
        super(tPlayer,"§2§lTracks", 6, page, trackSort);
    }

    public List<Track> getTracks(int page, TrackSort trackSort) {
        List<Track> tracks;
        if (page == ELYTRAPAGE) {
            tracks = TrackDatabase.getOpenTracks().stream().filter(Track::isElytraTrack).collect(Collectors.toList());
            sortTracks(tracks, trackSort);
        } else if (page == PARKOURPAGE) {
            tracks = TrackDatabase.getOpenTracks().stream().filter(Track::isParkourTrack).collect(Collectors.toList());
            sortTracks(tracks, trackSort);
        } else {
            List<Track> tempTracks = TrackDatabase.getOpenTracks().stream().filter(Track::isBoatTrack).collect(Collectors.toList());
            sortTracks(tempTracks, trackSort);
            int start = 36 * page;
            tracks = new ArrayList<>();
            for (int i = start; i < Math.min(start + 36, tempTracks.size()); i++) {
                tracks.add(tempTracks.get(i));
            }
        }
        return tracks;
    }

    @Override
    public GuiButton getSortingButton(ItemStack item, TPlayer tPlayer, int page, TrackSort sort) {
        var button = new GuiButton(item);
        button.setAction(() -> {
            new TimeTrialGui(tPlayer, page, sort).show(tPlayer.getPlayer());
        });
        return button;
    }

    @Override
    public GuiButton getPageButton(ItemStack item, TPlayer tPlayer, int page){
        var button = new GuiButton(item);
        button.setAction(() -> {
            new TimeTrialGui(tPlayer, page, trackSort).show(tPlayer.getPlayer());
        });
        return button;
    }

    @Override
    public GuiButton getTrackButton(Player player, Track track){
        var button = new GuiButton(track.getGuiItem(player.getUniqueId()));
        button.setAction(() -> {
            if (!track.getSpawnLocation().isWorldLoaded()) {
                player.sendMessage("§cWorld is not loaded!");
                return;
            }
            player.teleport(track.getSpawnLocation());
            player.closeInventory();
        });
        return button;
    }
}
