package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.sounds.PlaySound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Gui;
import org.bukkit.Material;
import org.bukkit.entity.Boat;

public class BoatSettingsGui extends BaseGui {

    public BoatSettingsGui(TPlayer tPlayer) {
        super(Text.get(tPlayer.getPlayer(), Gui.SETTINGS_TITLE), 3);
        setButtons(tPlayer);
    }

    private void setButtons(TPlayer tPlayer) {
        int count = 0;
        for (Material boat : ApiUtilities.getBoatMaterials()) {
            setItem(getBoatTypeButton(tPlayer, boat), count);
            count++;
        }
        setItem(GuiCommon.getReturnToSettingsButton(tPlayer), 26);
    }

    private GuiButton getBoatTypeButton(TPlayer tPlayer, Material boatType) {
        var button = new GuiButton(new ItemBuilder(boatType).build());
        button.setAction(() -> {
            tPlayer.setBoat(ApiUtilities.getBoatType(boatType));
            tPlayer.setChestBoat(ApiUtilities.isChestBoat(boatType));
            if (tPlayer.getPlayer() instanceof Boat boat) {
                boat.setBoatType(tPlayer.getBoat());
            }
            PlaySound.buttonClick(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }


}
