package me.makkuusen.timing.system.event;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.QualifyHeat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.participant.Spectator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

public class EventAnnouncements {

    private static void broadcastAnnouncement(Heat heat, String key, String... replacements) {

        for (Participant p : heat.getParticipants()) {
            if (p.getTPlayer().getPlayer() != null) {
                TimingSystem.getPlugin().sendMessage(p.getTPlayer().getPlayer(), key, replacements);
            }
        }
    }

    public static void broadcastFinish(Heat heat, Driver driver, long time) {
        broadcastAnnouncement(heat, "messages.announcements.finish", "%player%", driver.getTPlayer().getName(), "%position%", String.valueOf(driver.getPosition()), "%time%", ApiUtilities.formatAsTime(time));
    }

    public static void broadcastPit(Heat heat, Driver driver, int pit) {
        broadcastAnnouncement(heat, "messages.announcements.pitstop", "%player%", driver.getTPlayer().getName(), "%pit%", String.valueOf(pit));
    }

    public static void broadcastFastestLap(Heat heat, Driver driver, long time) {
        broadcastAnnouncement(heat, "messages.announcements.fastestLap", "%player%", driver.getTPlayer().getName(), "%time%", ApiUtilities.formatAsTime(time));

    }
    public static void broadcastQualificationResults(Event event, List<Driver> drivers){
        for (Spectator s : event.getSpectators().values()) {
            if (s.getTPlayer().getPlayer() != null) {
                s.getTPlayer().getPlayer().sendMessage("§7Qualifying results for §f" + event.getDisplayName());
                int pos = 1;
                for (Driver d : drivers) {
                    s.getTPlayer().getPlayer().sendMessage("§7" + pos++ + ". §f" + d.getTPlayer().getName() + "§7 - §f"  + (d.getBestLap().isPresent() ? ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) : "-"));
                }
            }
        }
    }

    public static void broadcastFinalResults(Event event, List<Driver> drivers){
        for (Spectator s : event.getSpectators().values()) {
            if (s.getTPlayer().getPlayer() != null) {
                s.getTPlayer().getPlayer().sendMessage("§7Final results for §f" + event.getDisplayName());
                int pos = 1;
                for (Driver d : drivers) {
                    if (d.isFinished()) {
                        s.getTPlayer().getPlayer().sendMessage("§7" + pos++ + ". §f" + d.getTPlayer().getName() + "§7 - §f" + d.getLaps().size() + " §7laps in §f" + ApiUtilities.formatAsTime(d.getFinishTime()));
                    } else {
                        s.getTPlayer().getPlayer().sendMessage("§7" + pos++ + ". §f" + d.getTPlayer().getName());
                    }
                }
            }
        }
    }

    public static void broadcastHeatResult(List<Driver> drivers, Heat heat) {
        for (Spectator s : heat.getEvent().getSpectators().values()) {
            if (s.getTPlayer().getPlayer() != null) {
                s.getTPlayer().getPlayer().sendMessage("§7Results for §f" + heat.getName());
                int pos = 1;
                for (Driver d : drivers) {
                    if (heat instanceof QualifyHeat) {
                        s.getTPlayer().getPlayer().sendMessage("§7" +  pos++ + ". §f" + d.getTPlayer().getName() + "§7 - §f" + (d.getBestLap().isPresent() ? ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) : "-"));
                    } else  {
                        s.getTPlayer().getPlayer().sendMessage("§7" +  pos++ + ". §f" + d.getTPlayer().getName() + "§7 - §f" + d.getLaps().size() + " §7laps in §f" + ApiUtilities.formatAsTime(d.getFinishTime()));
                    }
                }
            }
        }
    }

    public static void broadcastCountdown(Heat heat, Integer count){
        for (Participant participant : heat.getParticipants()) {
            Player player = participant.getTPlayer().getPlayer();
            if (player != null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1, 1);
                Component mainTitle = Component.text(count, NamedTextColor.WHITE);
                Title.Times times = Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(100));
                Title title = Title.title(mainTitle, Component.empty(), times);
                player.showTitle(title);
            }
        }
    }

    public static void sendLapTime(Driver driver, long time) {
        if (driver.getTPlayer().getPlayer() == null) {
            return;
        }
        TimingSystem.getPlugin().sendMessage(driver.getTPlayer().getPlayer(), "messages.announcements.lap", "%time%", ApiUtilities.formatAsTime(time));
    }

    public static void sendStartSound(Heat heat) {
        for (Participant participant : heat.getParticipants()) {
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
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
    }

    public static void sendFinishTitle(Driver driver) {
        if (driver.getTPlayer().getPlayer() == null) {
            return;
        }
        Player player = driver.getTPlayer().getPlayer();
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "title " + player.getName() + " title {\"text\":\"§6-- §eP" + driver.getPosition() + " §6--\"}");
    }
}
