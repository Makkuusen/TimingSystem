package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.sounds.PlaySound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Gui;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SettingsGui extends BaseGui {

    public SettingsGui(TPlayer tPlayer) {
        super(Text.get(tPlayer.getPlayer(), Gui.SETTINGS_TITLE), 3);
        setButtons(tPlayer);
    }

    public static GuiButton getSoundButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.JUKEBOX).setName(Text.get(tPlayer, Gui.TOGGLE_SOUND)).build());
        button.setAction(() -> {
            tPlayer.toggleSound();
            PlaySound.buttonClick(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getVerboseButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.GOAT_HORN).setName(Text.get(tPlayer, Gui.TOGGLE_VERBOSE)).build());
        button.setAction(() -> {
            tPlayer.toggleVerbose();
            PlaySound.buttonClick(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getTimeTrialButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.BARRIER).setName(Text.get(tPlayer, Gui.TOGGLE_TIME_TRIAL)).build());
        button.setAction(() -> {
            tPlayer.toggleTimeTrial();
            PlaySound.buttonClick(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getOverrideButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.IRON_DOOR).setName(Text.get(tPlayer, Gui.TOGGLE_OVERRIDE)).build());
        button.setAction(() -> {
            tPlayer.toggleOverride();
            PlaySound.buttonClick(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }


    public static GuiButton getBoatMenuButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(tPlayer.getBoatMaterial()).setName(Text.get(tPlayer, Gui.CHANGE_BOAT_TYPE)).build());
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
        var button = new GuiButton(new ItemBuilder(Material.valueOf(materialName)).setName(Text.get(tPlayer, Gui.CHANGE_TEAM_COLOR)).build());
        button.setAction(() -> {
            new ColorSettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    public static GuiButton getHeatLapsButton(TPlayer tPlayer) {
        var button = new GuiButton(new ItemBuilder(Material.BAMBOO_SIGN).setName(Text.get(tPlayer, Gui.TOGGLE_FINAL_LAPS)).build());
        button.setAction(() -> {
            tPlayer.toggleSendFinalLaps();
            PlaySound.buttonClick(tPlayer);
            new SettingsGui(tPlayer).show(tPlayer.getPlayer());
        });
        return button;
    }

    private void setButtons(TPlayer tPlayer) {
        Player player = tPlayer.getPlayer();
        if (player != null && (player.isOp() || player.hasPermission("timingsystem.packs.trackadmin"))) {
            setItem(tPlayer.isOverride() ? GuiCommon.getStatusOnButton(tPlayer) : GuiCommon.getStatusOffButton(tPlayer), 0);
            setItem(getOverrideButton(tPlayer), 9);
        }

        setItem(tPlayer.isSound() ? GuiCommon.getStatusOnButton(tPlayer) : GuiCommon.getStatusOffButton(tPlayer), 1);
        setItem(getSoundButton(tPlayer), 10);
        setItem(tPlayer.isVerbose() ? GuiCommon.getStatusOnButton(tPlayer) : GuiCommon.getStatusOffButton(tPlayer), 2);
        setItem(getVerboseButton(tPlayer), 11);
        setItem(tPlayer.isTimeTrial() ? GuiCommon.getStatusOnButton(tPlayer) : GuiCommon.getStatusOffButton(tPlayer), 3);
        setItem(getTimeTrialButton(tPlayer), 12);
        setItem(tPlayer.isSendFinalLaps() ? GuiCommon.getStatusOnButton(tPlayer) : GuiCommon.getStatusOffButton(tPlayer), 4);
        setItem(getHeatLapsButton(tPlayer), 13);

        setItem(getBoatMenuButton(tPlayer), 15);
        setItem(getColorMenuButton(tPlayer), 16);
    }
}
