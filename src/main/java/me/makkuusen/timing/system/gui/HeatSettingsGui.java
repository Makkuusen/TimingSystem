package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.sounds.PlaySound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Gui;
import org.bukkit.Material;

public class HeatSettingsGui extends BaseGui {

    public HeatSettingsGui(TPlayer tPlayer) {
        super(Text.get(tPlayer, Gui.SETTINGS_TITLE), 3);
        setButtons(tPlayer);
    }

    private GuiButton getFinalButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.SOUL_LANTERN).setName(Text.get(tPlayer, Gui.TOGGLE_FINAL_LAPS)).build());
        button.setAction(() -> {
            tPlayer.toggleSendFinalLaps();
            PlaySound.buttonClick(tPlayer);
            new HeatSettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    private void setButtons(TPlayer tPlayer) {
        setItem(tPlayer.isSendFinalLaps() ? GuiCommon.getStatusOnButton(tPlayer) : GuiCommon.getStatusOffButton(tPlayer), 0);
        setItem(getFinalButton(tPlayer), 9);

        setItem(GuiCommon.getReturnToSettingsButton(tPlayer), 26);
    }

}
