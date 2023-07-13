package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ButtonUtilities {

    public static ItemStack sound;
    public static ItemStack verbose;
    public static ItemStack timeTrial;
    public static ItemStack override;
    public static ItemStack on;
    public static ItemStack off;
    public static ItemStack goBack;
    public static ItemStack borderGlass;
    public static ItemStack elytra;
    public static ItemStack boat;
    public static ItemStack parkour;
    public static List<ItemStack> boatPages = new ArrayList<>();
    public static ItemStack parkourPage;
    public static ItemStack elytraPage;

    public static void init() {
        sound = new ItemBuilder(Material.JUKEBOX).setName("§b§eToggle: Sound").build();
        verbose = new ItemBuilder(Material.GOAT_HORN).setName("§b§eToggle: Verbose").build();
        timeTrial = new ItemBuilder(Material.BARRIER).setName("§b§eToggle: TimeTrial").build();
        override = new ItemBuilder(Material.IRON_DOOR).setName("§b§eToggle: Override").build();
        on = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName("§b§eON").build();
        off = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName("§b§eOFF").build();
        goBack = new ItemBuilder(Material.ARROW).setName("§b§eReturn").build();
        borderGlass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("§r").build();
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

    public static GuiButton getBorderGlassButton() {
        var button = new GuiButton(borderGlass);
        button.setAction(() -> {
        });
        return button;
    }

    public static GuiButton getReturnToSettingsButton(TPlayer tPlayer) {
        var button = new GuiButton(goBack);
        button.setAction(() -> new SettingsGui(tPlayer).show(tPlayer.getPlayer()));
        return button;
    }

    public static void playConfirm(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1, 1);
    }

    public static Component getFilterTitle(TrackTag tag, Theme theme) {
        if (tag == null) {
            return Component.text(" - ALL").color(theme.getPrimary()).decorate(TextDecoration.BOLD);
        } else {
            return Component.text(" - " + tag.getValue()).color(theme.getPrimary()).decorate(TextDecoration.BOLD);
        }
    }
}
