package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.text.Gui;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SettingsGui extends BaseGui {

    public SettingsGui(TPlayer tPlayer) {
        super(TimingSystem.getPlugin().getText(tPlayer.getPlayer(), Gui.SETTINGS_TITLE), 3);
        setButtons(tPlayer);
    }

    public static GuiButton getSoundButton(TPlayer tPlayer) {
        var button = new GuiButton(GuiCommon.sound);
        button.setAction(() -> {
            tPlayer.toggleSound();
            GuiCommon.playConfirm(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getVerboseButton(TPlayer tPlayer) {
        var button = new GuiButton(GuiCommon.verbose);
        button.setAction(() -> {
            tPlayer.toggleVerbose();
            GuiCommon.playConfirm(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getTimeTrialButton(TPlayer tPlayer) {
        var button = new GuiButton(GuiCommon.timeTrial);
        button.setAction(() -> {
            tPlayer.toggleTimeTrial();
            GuiCommon.playConfirm(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getOverrideButton(TPlayer tPlayer) {
        var button = new GuiButton(GuiCommon.override);
        button.setAction(() -> {
            tPlayer.toggleOverride();
            GuiCommon.playConfirm(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getBoatMenuButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(tPlayer.getBoatMaterial()).setName("§eBoatType").build());
        button.setAction(() -> {
            new BoatSettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getColorMenuButton(TPlayer tPlayer) {
        var dyeColor = DyeColor.getByColor(tPlayer.getBukkitColor());
        String materialName = "WHITE_DYE";
        if (dyeColor != null) {
            materialName = dyeColor.name() + "_DYE";
        }
        var button = new GuiButton(new ItemBuilder(Material.valueOf(materialName)).setName(tPlayer.getColorCode() + "Team Color").build());
        button.setAction(() -> {
            new ColorSettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    private void setButtons(TPlayer tPlayer) {
        Player player = tPlayer.getPlayer();
        if (player != null && (player.isOp() || player.hasPermission("track.admin"))) {
            setItem(tPlayer.isOverride() ? GuiCommon.getStatusOnButton() : GuiCommon.getStatusOffButton(), 0);
            setItem(getOverrideButton(tPlayer), 9);
        }

        setItem(tPlayer.isSound() ? GuiCommon.getStatusOnButton() : GuiCommon.getStatusOffButton(), 1);
        setItem(getSoundButton(tPlayer), 10);
        setItem(tPlayer.isVerbose() ? GuiCommon.getStatusOnButton() : GuiCommon.getStatusOffButton(), 2);
        setItem(getVerboseButton(tPlayer), 11);
        setItem(tPlayer.isTimeTrial() ? GuiCommon.getStatusOnButton() : GuiCommon.getStatusOffButton(), 3);
        setItem(getTimeTrialButton(tPlayer), 12);

        setItem(getBoatMenuButton(tPlayer), 14);
        setItem(getColorMenuButton(tPlayer), 16);
    }
}
