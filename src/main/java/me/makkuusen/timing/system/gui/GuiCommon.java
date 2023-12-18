package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Gui;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class GuiCommon {
    public static ItemStack borderGlass;

    public static void init() {
        borderGlass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("§r").build();
    }

    public static GuiButton getStatusOnButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName(Text.get(tPlayer, Gui.ON)).build());
        button.setAction(() -> {

        });
        return button;
    }

    public static GuiButton getStatusOffButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName(Text.get(tPlayer, Gui.OFF)).build());
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
        var button = new GuiButton(new ItemBuilder(Material.ARROW).setName(Text.get(tPlayer, Gui.RETURN)).build());
        button.setAction(() -> new SettingsGui(tPlayer).show(tPlayer.getPlayer()));
        return button;
    }

}
