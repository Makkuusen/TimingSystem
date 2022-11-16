package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.TPlayer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ButtonFactory {

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

    public static GuiButton getStatusOnButton() {
        var button = new GuiButton(on);
        button.setAction(() -> {

        });
        return button;
    }

    public static GuiButton getStatusOffButton() {
        var button = new GuiButton(off);
        button.setAction(() -> {

        });
        return button;
    }

    static void playConfirm(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1, 1);
    }
}
