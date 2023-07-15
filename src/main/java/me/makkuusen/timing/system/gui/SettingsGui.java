package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.messages.Gui;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SettingsGui extends BaseGui {

    public SettingsGui(TPlayer tPlayer) {
        super(TimingSystem.getPlugin().getText(tPlayer.getPlayer(), Gui.SETTINGS_TITLE), 3);
        setButtons(tPlayer);
    }

    public static GuiButton getSoundButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.JUKEBOX).setName(TimingSystem.getPlugin().getText(tPlayer, Gui.TOGGLE_SOUND)).build());
        button.setAction(() -> {
            tPlayer.toggleSound();
            GuiCommon.playConfirm(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getVerboseButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.JUKEBOX).setName(TimingSystem.getPlugin().getText(tPlayer, Gui.TOGGLE_VERBOSE)).build());
        button.setAction(() -> {
            tPlayer.toggleVerbose();
            GuiCommon.playConfirm(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getTimeTrialButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.JUKEBOX).setName(TimingSystem.getPlugin().getText(tPlayer, Gui.TOGGLE_TIME_TRIAL)).build());
        button.setAction(() -> {
            tPlayer.toggleTimeTrial();
            GuiCommon.playConfirm(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getOverrideButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.JUKEBOX).setName(TimingSystem.getPlugin().getText(tPlayer, Gui.TOGGLE_OVERRIDE)).build());
        button.setAction(() -> {
            tPlayer.toggleOverride();
            GuiCommon.playConfirm(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }


    public static GuiButton getBoatMenuButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(tPlayer.getBoatMaterial()).setName(TimingSystem.getPlugin().getText(tPlayer, Gui.CHANGE_BOAT_TYPE)).build());
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
        var button = new GuiButton(new ItemBuilder(Material.valueOf(materialName)).setName(TimingSystem.getPlugin().getText(tPlayer, Gui.CHANGE_TEAM_COLOR)).build());
        button.setAction(() -> {
            new ColorSettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    private void setButtons(TPlayer tPlayer) {
        Player player = tPlayer.getPlayer();
        if (player != null && (player.isOp() || player.hasPermission("track.admin"))) {
            setItem(tPlayer.isOverride() ? GuiCommon.getStatusOnButton(tPlayer) : GuiCommon.getStatusOffButton(tPlayer), 0);
            setItem(getOverrideButton(tPlayer), 9);
        }

        setItem(tPlayer.isSound() ? GuiCommon.getStatusOnButton(tPlayer) : GuiCommon.getStatusOffButton(tPlayer), 1);
        setItem(getSoundButton(tPlayer), 10);
        setItem(tPlayer.isVerbose() ? GuiCommon.getStatusOnButton(tPlayer) : GuiCommon.getStatusOffButton(tPlayer), 2);
        setItem(getVerboseButton(tPlayer), 11);
        setItem(tPlayer.isTimeTrial() ? GuiCommon.getStatusOnButton(tPlayer) : GuiCommon.getStatusOffButton(tPlayer), 3);
        setItem(getTimeTrialButton(tPlayer), 12);

        setItem(getBoatMenuButton(tPlayer), 14);
        setItem(getColorMenuButton(tPlayer), 16);
    }
}
