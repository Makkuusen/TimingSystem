package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackTag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TimeTrialGui extends TrackPageGui{

    public TimeTrialGui(TPlayer tPlayer, int page) {
        super(tPlayer,"§2§lTracks - ALL", 6, page);
    }

    public TimeTrialGui(TPlayer tPlayer, String title, int page, TrackSort trackSort, TrackTag filter) {
        super(tPlayer, title, 6, page, trackSort, filter);
    }

    public List<Track> getTracks(int page, TrackSort trackSort) {

        var filteredTracks = TrackDatabase.getOpenTracks().stream().filter(track -> track.hasTag(filter)).filter(Track::isWeightAboveZero);

        List<Track> tracks;
        if (page == ELYTRAPAGE) {
            tracks = filteredTracks.filter(Track::isElytraTrack).collect(Collectors.toList());
            sortTracks(tracks, trackSort);
        } else if (page == PARKOURPAGE) {
            tracks = filteredTracks.filter(Track::isParkourTrack).collect(Collectors.toList());
            sortTracks(tracks, trackSort);
        } else {
            List<Track> tempTracks = filteredTracks.filter(Track::isBoatTrack).collect(Collectors.toList());
            tempTracks.stream();
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
    public GuiButton getSortingButton(ItemStack item, TPlayer tPlayer, int page, TrackSort sort, TrackTag tag) {
        var button = new GuiButton(item);
        button.setAction(() -> {
            String title = "§2§lTracks " + ButtonUtilities.getFilterTitel(filter);
            if (tPlayer.isSound()) {
                ButtonUtilities.playConfirm(tPlayer.getPlayer());
            }
            new TimeTrialGui(tPlayer, title, page, sort, tag).show(tPlayer.getPlayer());
        });
        return button;
    }

    @Override
    public GuiButton getFilterButton(ItemStack item, TPlayer tPlayer, int page, TrackSort sort, TrackTag tag) {
        var button = new GuiButton(item);
        button.setAction(() -> {
            String title = "§2§lTracks " + ButtonUtilities.getFilterTitel(tag);
            if (tPlayer.isSound()) {
                ButtonUtilities.playConfirm(tPlayer.getPlayer());
            }
            new TimeTrialGui(tPlayer, title, page, sort, tag).show(tPlayer.getPlayer());
        });
        return button;
    }


    @Override
    public GuiButton getPageButton(ItemStack item, TPlayer tPlayer, int page){
        var button = new GuiButton(item);
        button.setAction(() -> {
            String title = "§2§lTracks " + ButtonUtilities.getFilterTitel(filter);
            new TimeTrialGui(tPlayer, title, page, trackSort, filter).show(tPlayer.getPlayer());
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
            ApiUtilities.teleportPlayerAndSpawnBoat(player, track, track.getSpawnLocation());
            player.closeInventory();
        });
        return button;
    }
}
