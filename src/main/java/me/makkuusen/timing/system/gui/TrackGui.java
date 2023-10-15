package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.theme.Text;
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

    public TrackGui(TPlayer tPlayer) {
        super(tPlayer, Text.get(tPlayer.getPlayer(), Gui.TRACKS_TITLE));
    }

    public TrackGui(TPlayer tPlayer, Component title) {
        super(tPlayer, title);
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
                Text.send(player, Error.WORLD_NOT_LOADED);
                return;
            }
            player.teleport(track.getSpawnLocation());
            player.closeInventory();
        });
        return button;
    }

    private ItemStack setTrackLore(Player player, Track track, ItemStack toReturn) {
        List<Component> loreToSet = new ArrayList<>();
        loreToSet.add(Text.get(player, Gui.TOTAL_FINISHES, "%total%", String.valueOf(track.getTotalFinishes())));
        loreToSet.add(Text.get(player, Gui.TOTAL_ATTEMPTS, "%total%", String.valueOf(track.getTotalFinishes() + track.getTotalAttempts())));
        loreToSet.add(Text.get(player, Gui.TIME_SPENT, "%time%", ApiUtilities.formatAsTimeSpent(track.getTotalTimeSpent())));
        loreToSet.add(Text.get(player, Gui.CREATED_BY, "%player%", track.getOwner().getName()));
        loreToSet.add(Text.get(player, Gui.CREATED_AT, "%time%", ApiUtilities.niceDate(track.getDateCreated())));
        loreToSet.add(Text.get(player, Gui.WEIGHT, "%weight%", String.valueOf(track.getWeight())));

        Component tags = Component.empty();
        boolean notFirst = false;
        for (TrackTag tag : track.getTags()) {
            if (notFirst) {
                tags = tags.append(Component.text(", ").color(tPlayer.getTheme().getSecondary()));
            }
            tags = tags.append(Component.text(tag.getValue()).color(tag.getColor()));
            notFirst = true;
        }
        loreToSet.add(Text.get(player, Gui.TAGS).append(tags));
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
