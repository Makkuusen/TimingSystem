package me.makkuusen.timing.system.track;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@Getter
public class TrackTag {
    int id;
    String value;
    TextColor color;
    ItemStack item;
    int weight;

    public TrackTag(String value, TextColor color, ItemStack item, int weight) {
        this.value = value.toUpperCase();
        this.color = color;
        this.item = item;
        this.weight = weight;
    }

    public TrackTag(DbRow dbRow) {
        this.id = dbRow.getInt("id");
        this.value = dbRow.getString("tag");
        this.color = TextColor.fromHexString(dbRow.getString("color"));
        this.item = ApiUtilities.stringToItem(dbRow.getString("item"));
        this.weight = dbRow.getInt("weight");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TrackTag trackTag) {
            return trackTag.getValue().equalsIgnoreCase(value);
        }

        if (o instanceof String tackTagString) {
            return tackTagString.equalsIgnoreCase(value);
        }

        return false;
    }

    public ItemStack getItem(Player player) {
        ItemStack toReturn;
        if (item == null) {
            toReturn = new ItemBuilder(Material.ANVIL).build();
        } else {
            toReturn = item.clone();
        }
        ItemMeta im = toReturn.getItemMeta();
        im.displayName(Component.text(value).color(color));
        if (player.hasPermission("timingsystem.packs.trackadmin")) {
            im.lore(List.of(Component.text(getWeight()).color(NamedTextColor.DARK_GRAY)));
        }
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        im.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        im.addItemFlags(ItemFlag.HIDE_DYE);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        toReturn.setItemMeta(im);

        return toReturn;
    }

    public void setItem(ItemStack item) {
        this.item = item;
        DB.executeUpdateAsync("UPDATE `ts_tags` SET `item` = " + Database.sqlString(ApiUtilities.itemToString(item)) + " WHERE `tag` = '" + value + "';");
    }

    public void setColor(TextColor color) {
        this.color = color;
        DB.executeUpdateAsync("UPDATE `ts_tags` SET `color` = '" + color.asHexString() + "' WHERE `tag` = '" + value + "';");
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
        DB.executeUpdateAsync("UPDATE `ts_tags` SET `weight` = " + weight + " WHERE `tag` = '" + value + "';");
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }


}
