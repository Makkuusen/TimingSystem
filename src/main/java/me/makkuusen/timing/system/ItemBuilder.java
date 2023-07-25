package me.makkuusen.timing.system;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta itemMeta;
    private LeatherArmorMeta leatherMeta;

    /**
     * Creates a new {@link ItemBuilder} object
     *
     * @param m The material the item should be.
     */
    public ItemBuilder(Material m) {
        item = new ItemStack(m);
        itemMeta = item.getItemMeta();
        if (m.toString().toLowerCase().contains("leather")) {
            leatherMeta = (LeatherArmorMeta) item.getItemMeta();
        }
    }

    public ItemBuilder setName(String name) {
        if (item.getType().toString().toLowerCase().contains("leather")) {
            leatherMeta.displayName(Component.text(name));
        } else {
            itemMeta.displayName(Component.text(name));
        }
        return this;
    }

    public ItemBuilder setName(Component name) {
        if (item.getType().toString().toLowerCase().contains("leather")) {
            leatherMeta.displayName(name);
        } else {
            itemMeta.displayName(name);
        }
        return this;
    }

    public ItemBuilder setCustomModelData(int value) {
        if (item.getType().toString().toLowerCase().contains("leather")) {
            leatherMeta.setCustomModelData(value);
        } else {
            itemMeta.setCustomModelData(value);
        }
        return this;
    }


    public ItemStack build() {

        if (item.getType().toString().toLowerCase().contains("leather")) {
            item.setItemMeta(leatherMeta);
        } else {
            item.setItemMeta(itemMeta);
        }

        return item;
    }

}
