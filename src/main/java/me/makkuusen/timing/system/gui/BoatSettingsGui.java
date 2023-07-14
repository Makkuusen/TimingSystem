package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.messages.Gui;
import org.bukkit.Material;
import org.bukkit.entity.Boat;

public class BoatSettingsGui extends BaseGui {

    public BoatSettingsGui(TPlayer tPlayer) {
        super(TimingSystem.getPlugin().getText(tPlayer.getPlayer(), Gui.SETTINGS_TITLE), 3);
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
        var button = new GuiButton(new ItemBuilder(boatType).setName("§eBoat").build());
        button.setAction(() -> {
            tPlayer.setBoat(ApiUtilities.getBoatType(boatType));
            tPlayer.setChestBoat(ApiUtilities.isChestBoat(boatType));
            if (tPlayer.getPlayer() instanceof Boat boat) {
                boat.setBoatType(tPlayer.getBoat());
            }
            GuiCommon.playConfirm(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }


}
