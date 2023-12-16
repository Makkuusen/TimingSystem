package me.makkuusen.timing.system.sounds;

import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;

public class PlaySound {
    public static void buttonClick(TPlayer tPlayer) {
        if (tPlayer.getSettings().isSound()) {
            tPlayer.getPlayer().playSound(tPlayer.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.2F, 1);
        }
    }

    public static void pageTurn(TPlayer tPlayer) {
        if (tPlayer.getSettings().isSound()) {
            tPlayer.getPlayer().playSound(tPlayer.getPlayer().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 0.2F, 1);
        }
    }

    public static void boatUtilsEffect(TPlayer tPlayer) {
        if (tPlayer.getSettings().isSound()) {
            tPlayer.getPlayer().playSound(tPlayer.getPlayer().getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 1, 1);
        }
    }

    public static void countDownPling(TPlayer tPlayer) {
        if (tPlayer.getSettings().isSound() && tPlayer.getPlayer() != null) {
            tPlayer.getPlayer().playSound(tPlayer.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 1F, 1);
        }
    }
}
