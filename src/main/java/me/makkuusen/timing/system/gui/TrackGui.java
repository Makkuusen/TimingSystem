package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Gui;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
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
        var item = setTrackLore(player, track, track.getGuiItem(player.getUniqueId()));
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

    private ItemStack setTrackLore(Player player, Track track, ItemStack toReturn) {
        List<Component> loreToSet = new ArrayList<>();
        loreToSet.add(plugin.getText(player, Gui.TOTAL_FINISHES, "%total%", String.valueOf(track.getTotalFinishes())));
        loreToSet.add(plugin.getText(player, Gui.TOTAL_ATTEMPTS, "%total%", String.valueOf(track.getTotalFinishes() + track.getTotalAttempts())));
        loreToSet.add(plugin.getText(player, Gui.TIME_SPENT, "%time%", ApiUtilities.formatAsTimeSpent(track.getTotalTimeSpent())));
        loreToSet.add(plugin.getText(player, Gui.CREATED_BY, "%player%", track.getOwner().getName()));
        loreToSet.add(plugin.getText(player, Gui.CREATED_AT, "%time%", ApiUtilities.niceDate(track.getDateCreated())));
        loreToSet.add(plugin.getText(player, Gui.WEIGHT, "%weight%", String.valueOf(track.getWeight())));

        List<String> tagList = new ArrayList<>();
        for (TrackTag tag : track.getTags()) {
            tagList.add(tag.getValue());
        }
        String tags = String.join(", ", tagList);
        loreToSet.add(plugin.getText(player, Gui.TAGS, "%tags%", tags));

        ItemMeta im = toReturn.getItemMeta();
        im.lore(loreToSet);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        im.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        im.addItemFlags(ItemFlag.HIDE_DYE);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        toReturn.setItemMeta(im);
        return toReturn;
    }


}
