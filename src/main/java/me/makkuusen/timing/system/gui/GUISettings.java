package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TimingSystem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUISettings {

    public static ItemStack sound;
    public static ItemStack verbose;
    public static ItemStack timeTrial;
    public static ItemStack override;
    public static ItemStack on;
    public static ItemStack off;

    public static void init() {
        sound = new ItemBuilder(Material.JUKEBOX).setName("§b§eToggle: Sound").build();
        verbose = new ItemBuilder(Material.GOAT_HORN).setName("§b§eToggle: Verbose").build();
        timeTrial = new ItemBuilder(Material.BARRIER).setName("§b§eToggle: Timetrial").build();
        override = new ItemBuilder(Material.IRON_DOOR).setName("§b§eToggle: Override").build();
        on = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName("§b§eON").build();
        off = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName("§b§eOFF").build();
    }

    public static void openSettingsGui(Player p) {
        Inventory inv = createGui(p);
        p.openInventory(inv);
    }

    private static Inventory createGui(Player p) {

        Inventory inv = Bukkit.createInventory(null, InventoryType.BARREL, Component.text("§2§lSettings"));

        var tPlayer = Database.getPlayer(p.getUniqueId());
        // 1, 3, 5
        // 10 jukebox, 12 horn, 14 barrier, 16 boat
        inv.setItem(10, sound);
        inv.setItem(11, verbose);
        inv.setItem(12, timeTrial);
        if (p.isOp() || p.hasPermission("track.admin")) {
            inv.setItem(9, override);
            inv.setItem(0, tPlayer.isOverride() ? on : off);
        }
        var boat = new ItemBuilder(tPlayer.getBoatMaterial()).setName("§eBoatType").build();

        var dyeColor = DyeColor.getByColor(tPlayer.getBukkitColor());
        String materialName = "WHITE_DYE";
        if (dyeColor != null) {
            materialName = dyeColor.name() + "_DYE";
        }

        inv.setItem(14, boat);
        inv.setItem(16, new ItemBuilder(Material.valueOf(materialName)).setName(tPlayer.getColorCode() + "Team Color").build());
        inv.setItem(1, tPlayer.isSound() ? on : off);
        inv.setItem(2, tPlayer.isVerbose() ? on : off);
        inv.setItem(3, tPlayer.isTimeTrial() ? on : off);

        return inv;
    }

    public static void openSettingsBoatGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.BARREL, Component.text("§2§lBoat Settings"));

        int count = 0;
        for (Material boat : ApiUtilities.getBoatMaterials()) {
            inv.setItem(count, new ItemBuilder(boat).setName("§b§eBoat").build());
            count++;
        }
        inv.setItem(26, new ItemBuilder(Material.ARROW).setName("§b§eReturn").build());
        p.openInventory(inv);
    }

    public static void openSettingsColorGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.BARREL, Component.text("§2§lColor Settings"));
        int count = 0;
        for (DyeColor dyeColor : DyeColor.values()) {
            try {
                Material dye = Material.valueOf(dyeColor.name() + "_DYE");
                inv.setItem(count, new ItemBuilder(dye).setName("§eColor").build());
                count++;
            } catch (IllegalArgumentException exception) {

            }
        }
        inv.setItem(26, new ItemBuilder(Material.ARROW).setName("§b§eReturn").build());
        p.openInventory(inv);
    }
}
