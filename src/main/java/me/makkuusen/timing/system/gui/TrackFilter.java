package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrackFilter {

    private Set<TrackTag> tags;
    private boolean anyMatch = true;
    public TrackFilter() {
        tags = new HashSet<>();
    }

    public boolean isAnyMatch() {
        return anyMatch;
    }

    public void setAnyMatch(boolean anymatch) {
        this.anyMatch = anymatch;
    }

    public void addTag(TrackTag tag) {
        tags.add(tag);
    }

    public void removeTag(TrackTag tag) {
        tags.remove(tag);
    }

    public void setTags(Set<TrackTag> tags) {
        this.tags = tags;
    }

    public Set<TrackTag> getTags() {
        return tags;
    }

    public ItemStack getItem(){
        var item = new ItemBuilder(Material.HOPPER).setName("§eFilter by: ").build();
        List<Component> loreToSet = new ArrayList<>();

        List<String> tagList = new ArrayList<>();
        for (TrackTag tag : getTags()) {
            tagList.add(tag.getValue());
        }
        String tags = String.join(", ", tagList);
        loreToSet.add(Component.text("§e" + tags));

        ItemMeta im = item.getItemMeta();
        im.lore(loreToSet);
        item.setItemMeta(im);
        return item;
    }
}
