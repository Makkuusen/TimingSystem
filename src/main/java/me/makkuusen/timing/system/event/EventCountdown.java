package me.makkuusen.timing.system.event;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.participant.Spectator;
import me.makkuusen.timing.system.sounds.PlaySound;
import me.makkuusen.timing.system.tplayer.TPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class EventCountdown {

    BossBar bossBar;
    private int countdownInSeconds;
    private Long countdownStarted;
    private BukkitTask countdownTask;

    private String label;
    private boolean isActive = false;

    Event event;

    public EventCountdown(Event event) {
        this.event = event;
    }

    public void startCountdown(int length, String label) {
        this.label = label;
        this.countdownInSeconds = length;
        countdownStarted = TimingSystem.currentTime.getEpochSecond();
        if (bossBar == null) {
            bossBar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        }
        countdownTask = Bukkit.getScheduler().runTaskTimer(TimingSystem.getPlugin(), () -> {
            updateBossBar();
            if (timeLeft() < 1) {
                stopCountdown();
            }
        }, 0, 20);
        isActive = true;
    }

    public void stopCountdown() {
        Bukkit.getScheduler().cancelTask(countdownTask.getTaskId());
        event.getSpectators().values().forEach(spectator -> {
            Player player = spectator.getTPlayer().getPlayer();
            if (player != null) {
                player.hideBossBar(bossBar);
            }
        });
        isActive = false;
    }

    private long timeLeft() {
        return countdownInSeconds - (TimingSystem.currentTime.getEpochSecond() - countdownStarted);
    }

    private float percentDone() {
        return (float) (TimingSystem.currentTime.getEpochSecond() - countdownStarted) / countdownInSeconds;
    }

    private BossBar.Color getColor() {

        if (timeLeft() < 30) {
            return BossBar.Color.RED;
        } else {
            return BossBar.Color.GREEN;
        }
    }

    private void updateBossBar() {

        String displayText;
        if (label == null) {
            displayText = "Time left: ";
        } else {
            displayText = label + ": ";
        }
        final Component time = Component.text(displayText + ApiUtilities.formatAsSeconds(timeLeft()));
        if (bossBar == null) {
            bossBar = BossBar.bossBar(time, 1, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        }
        bossBar.name(time);
        bossBar.progress(Math.max(1 - percentDone(), 0));
        if (bossBar.color() != getColor()) {
            for (Spectator spectator : event.getSpectators().values()) {
                PlaySound.countDownPling(spectator.getTPlayer());
            }
        } else {
            for (Spectator spectator : event.getSpectators().values()) {
                if (timeLeft() < 1) {
                    PlaySound.countDownPling(spectator.getTPlayer());
                }
            }
        }
        bossBar.color(getColor());

        event.getSpectators().values().forEach(spectator -> {
            Player player = spectator.getTPlayer().getPlayer();
            if (player != null) {
                player.showBossBar(bossBar);
            }
        });
    }

    public boolean isActive() {
        return isActive;
    }

    public void addSpectator(TPlayer tPlayer) {
        if (isActive) {
            Player player = tPlayer.getPlayer();
            if (player != null) {
                player.showBossBar(bossBar);
            }
        }
    }

    public void removeSpectator(TPlayer tPlayer) {
        if (isActive) {
            Player player = tPlayer.getPlayer();
            if (player != null) {
                player.hideBossBar(bossBar);
            }
        }
    }
}
