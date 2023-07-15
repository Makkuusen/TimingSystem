package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.messages.Gui;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

public class ColorSettingsGui extends BaseGui {

    public ColorSettingsGui(TPlayer tPlayer) {
        super(TimingSystem.getPlugin().getText(tPlayer.getPlayer(), Gui.SETTINGS_TITLE), 3);
        setButtons(tPlayer);
    }

    private void setButtons(TPlayer tPlayer) {
        int count = 0;
        for (DyeColor dyeColor : DyeColor.values()) {
            Material dye = Material.valueOf(dyeColor.name() + "_DYE");
            setItem(getDyeColorButton(tPlayer, dye), count);
            count++;
        }
        setItem(GuiCommon.getReturnToSettingsButton(tPlayer), 26);
    }

    private GuiButton getDyeColorButton(TPlayer tPlayer, Material dye) {
        var hexColor = ApiUtilities.getHexFromDyeColor(dye);
        var button = new GuiButton(new ItemBuilder(dye).setName(TimingSystem.getPlugin().getText(tPlayer, Gui.COLOR).color(TextColor.fromHexString(hexColor))).build());
        button.setAction(() -> {
            tPlayer.setHexColor(hexColor);
            GuiCommon.playConfirm(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }
}
