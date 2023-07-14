package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.text.Error;
import me.makkuusen.timing.system.text.Gui;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TrackGui extends TrackPageGui {

    public TrackGui(TPlayer tPlayer, int page) {
        super(tPlayer, TimingSystem.getPlugin().getText(tPlayer.getPlayer(), Gui.TRACKS_TITLE), page);
    }

    public TrackGui(TPlayer tPlayer, Component title, int page, TrackSort trackSort, TrackFilter filter, Track.TrackType trackType) {
        super(tPlayer, title, page, trackSort, filter, trackType);
    }

    public List<Track> getTracks() {
        return TrackDatabase.getTracks();
    }

    @Override
    public GuiButton getTrackButton(Player player, Track track) {
        var item = setTrackLore(track, track.getGuiItem(player.getUniqueId()));
        var button = new GuiButton(item);
        button.setAction(() -> {
            if (!track.getSpawnLocation().isWorldLoaded()) {
                TimingSystem.getPlugin().sendMessage(player, Error.WORLD_NOT_LOADED);
                return;
            }
            player.teleport(track.getSpawnLocation());
            player.closeInventory();
        });
        return button;
    }

    private ItemStack setTrackLore(Track track, ItemStack toReturn) {
        List<Component> loreToSet = new ArrayList<>();
        loreToSet.add(Component.text("§7Total Finishes: §e" + track.getTotalFinishes()));
        loreToSet.add(Component.text("§7Total Attempts: §e" + (track.getTotalFinishes() + track.getTotalAttempts())));
        loreToSet.add(Component.text("§7Time Spent: §e" + ApiUtilities.formatAsTimeSpent(track.getTotalTimeSpent())));
        loreToSet.add(Component.text("§7Created by: §e" + track.getOwner().getName()));
        loreToSet.add(Component.text("§7Created at: §e" + ApiUtilities.niceDate(track.getDateCreated())));
        loreToSet.add(Component.text("§7Weight: §e" + track.getWeight()));

        List<String> tagList = new ArrayList<>();
        for (TrackTag tag : track.getTags()) {
            tagList.add(tag.getValue());
        }
        String tags = String.join(", ", tagList);
        loreToSet.add(Component.text("§7Tags: §e" + tags));

        ItemMeta im = toReturn.getItemMeta();
        im.lore(loreToSet);
        toReturn.setItemMeta(im);
        return toReturn;
    }


}
