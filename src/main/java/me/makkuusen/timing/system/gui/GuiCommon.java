package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.messages.Gui;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.inventory.ItemStack;

public class GuiCommon {
    public static ItemStack borderGlass;

    public static void init() {
        borderGlass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("§r").build();
    }

    public static GuiButton getStatusOnButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName(TimingSystem.getPlugin().getText(tPlayer, Gui.ON)).build());
        button.setAction(() -> {

        });
        return button;
    }

    public static GuiButton getStatusOffButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName(TimingSystem.getPlugin().getText(tPlayer, Gui.OFF)).build());
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
        var button = new GuiButton(new ItemBuilder(Material.ARROW).setName(TimingSystem.getPlugin().getText(tPlayer, Gui.RETURN)).build());
        button.setAction(() -> new SettingsGui(tPlayer).show(tPlayer.getPlayer()));
        return button;
    }

    public static void playConfirm(TPlayer tPlayer) {
        if (tPlayer.isSound()) {
            tPlayer.getPlayer().playSound(tPlayer.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.2F, 1);
        }
    }
}
