package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class EventAnnouncements {

    private static void sendAnnouncement (Heat heat, String key, String... replacements) {

        for (Participant p : heat.getParticipants())
        {
            if (p.getTPlayer().getPlayer() != null)
            {
                TimingSystem.getPlugin().sendMessage(p.getTPlayer().getPlayer(), key, replacements);
            }
        }
    }

    public static void broadcastFinish (Heat heat, Driver driver, long time)
    {
        sendAnnouncement(heat, "messages.announcements.finish", "%player%", driver.getTPlayer().getName(), "%position%", String.valueOf(driver.getPosition()), "%time%", ApiUtilities.formatAsTime(time));
    }

    public static void sendStartSound(Heat heat) {
        for (Participant participant : heat.getParticipants())
        {
            Player player = participant.getTPlayer().getPlayer();
            if (player != null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1, 1);
            }
        }
    }

    public static void sendFinishSound(Driver raceDriver) {
        if (raceDriver.getTPlayer().getPlayer() == null) {
            return;
        }
        Player player = raceDriver.getTPlayer().getPlayer();
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER,1,1);
    }

    public static void sendFinishTitle(Driver driver) {
        if (driver.getTPlayer().getPlayer() == null) {
            return;
        }
        Player player = driver.getTPlayer().getPlayer();
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "title " + player.getName() + " title {\"text\":\"§6-- §eP" + driver.getPosition() + " §6--\"}");
    }
}
