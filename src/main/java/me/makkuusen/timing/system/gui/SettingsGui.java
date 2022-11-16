package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.TPlayer;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SettingsGui extends BaseGui {

    public SettingsGui(TPlayer tPlayer) {
        super("Cool Title", 3);
        setButtons(tPlayer);
    }

    private void setButtons(TPlayer tPlayer){
        Player player = tPlayer.getPlayer();
        if (player != null && (player.isOp() || player.hasPermission("track.admin"))) {
            setItem(tPlayer.isOverride() ? ButtonFactory.getStatusOnButton() : ButtonFactory.getStatusOffButton(), 0);
            setItem(getOverrideButton(tPlayer), 9);
        }

        setItem(tPlayer.isSound() ? ButtonFactory.getStatusOnButton() : ButtonFactory.getStatusOffButton(), 1);
        setItem(getSoundButton(tPlayer), 10);
        setItem(tPlayer.isVerbose() ? ButtonFactory.getStatusOnButton() : ButtonFactory.getStatusOffButton(), 2);
        setItem(getVerboseButton(tPlayer), 11);
        setItem(tPlayer.isTimeTrial() ? ButtonFactory.getStatusOnButton() : ButtonFactory.getStatusOffButton(), 3);
        setItem(getTimeTrialButton(tPlayer), 12);

        setItem(getBoatMenuButton(tPlayer),14);
        setItem(getColorMenuButton(tPlayer), 16);
    }

    public static GuiButton getSoundButton(TPlayer tPlayer) {
        var button = new GuiButton(ButtonFactory.sound);
        button.setAction(() -> {
            tPlayer.switchToggleSound();
            if (tPlayer.isSound()) {
                ButtonFactory.playConfirm(tPlayer.getPlayer());
            }
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getVerboseButton(TPlayer tPlayer) {
        var button = new GuiButton(ButtonFactory.verbose);
        button.setAction(() -> {
            tPlayer.toggleVerbose();
            if (tPlayer.isSound()) {
                ButtonFactory.playConfirm(tPlayer.getPlayer());
            }
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getTimeTrialButton(TPlayer tPlayer) {
        var button = new GuiButton(ButtonFactory.timeTrial);
        button.setAction(() -> {
            tPlayer.toggleTimeTrial();
            if (tPlayer.isSound()) {
                ButtonFactory.playConfirm(tPlayer.getPlayer());
            }
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getOverrideButton(TPlayer tPlayer) {
        var button = new GuiButton(ButtonFactory.override);
        button.setAction(() -> {
            tPlayer.toggleOverride();
            if (tPlayer.isSound()) {
                ButtonFactory.playConfirm(tPlayer.getPlayer());
            }
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getBoatMenuButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(tPlayer.getBoatMaterial()).setName("§eBoatType").build());
        button.setAction(() -> {

        });
        return button;
    }

    public static GuiButton getColorMenuButton(TPlayer tPlayer) {
        var dyeColor = DyeColor.getByColor(tPlayer.getBukkitColor());
        String materialName = "WHITE_DYE";
        if (dyeColor != null) {
            materialName = dyeColor.name() + "_DYE";
        }
        var button = new GuiButton(new ItemBuilder(Material.valueOf(materialName)).setName(tPlayer.getColorCode()  + "Team Color").build());
        button.setAction(() -> {

        });
        return button;
    }
}
