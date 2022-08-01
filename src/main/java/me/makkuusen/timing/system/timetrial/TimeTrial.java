package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.LeaderboardManager;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Duration;
import java.time.Instant;

public class TimeTrial {

    public static TimingSystem plugin;
    private final me.makkuusen.timing.system.TPlayer TPlayer;
    private final Track track;
    private Instant startTime;
    private boolean[] checkpoints;
    private long bestFinish;


    public TimeTrial(Track track, TPlayer player) {
        this.track = track;
        this.startTime = TimingSystem.currentTime;
        this.checkpoints = new boolean[track.getCheckpoints().size()];
        this.bestFinish = getBestFinish(track.getBestFinish(player));
        this.TPlayer = player;

    }

    private long getBestFinish(TimeTrialFinish timeTrialFinish) {
        if (timeTrialFinish == null) {
            return -1;
        }
        return timeTrialFinish.getTime();
    }

    public long getBestFinish() {
        return bestFinish;
    }

    public Track getTrack() {
        return track;
    }

    private void passCheckpoint(int checkpoint) {
        checkpoint -= 1;
        try {
            for (int i = 0; i < checkpoints.length; i++) {
                if (i == checkpoint) {
                    this.checkpoints[i] = true;
                } else if (!this.checkpoints[i]) {
                    return;
                }
            }
        } catch (NullPointerException e) {
        }
    }

    private boolean hasPassedAllCheckpoints() {
        for (boolean b : checkpoints) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public int getPassedCheckpoints() {
        int count = 0;
        for (int i = 0; i < checkpoints.length; i++) {
            if (this.checkpoints[i]) {
                count++;
            }
        }

        return count;
    }

    public int getNextCheckpoint() {
        for (int i = 0; i < checkpoints.length; i++) {
            if (!this.checkpoints[i]) {
                return i + 1;
            }
        }
        return checkpoints.length;
    }

    public long getCurrentTime() {
        return Duration.between(startTime, Instant.now()).toMillis();
    }

    private long getTimeSinceStart(Instant time) {
        return Duration.between(startTime, time).toMillis();
    }

    public int getLatestCheckpoint() {
        for (int i = 0; i < checkpoints.length; i++) {
            if (!this.checkpoints[i]) {
                return i;
            }
        }
        return checkpoints.length;
    }


    public String getCheckpointsString() {
        if (checkpoints.length > 0) {
            return " (" + getPassedCheckpoints() + "/" + checkpoints.length + ")";
        }
        return "";
    }

    public void playerRestartMap() {
        Instant endTime = TimingSystem.currentTime;
        Player p = TPlayer.getPlayer();

        if (!hasPassedAllCheckpoints()) {
            plugin.sendMessage(p, "messages.error.timer.missedCheckpoints");
            return;
        }

        long mapTime = getTimeSinceStart(endTime);
        mapTime = Math.round(mapTime / 50) * 50;

        if (track.getBestFinish(TPlayer) == null) {
            plugin.sendMessage(p, "messages.timer.firstFinish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime));
            track.newTimeTrialFinish(mapTime, p.getUniqueId());
            this.bestFinish = getBestFinish(track.getBestFinish(TPlayer));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        } else if (mapTime < track.getBestFinish(TPlayer).getTime()) {
            plugin.sendMessage(p, "messages.timer.newRecord", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(TPlayer).getTime()));
            track.newTimeTrialFinish(mapTime, p.getUniqueId());
            this.bestFinish = getBestFinish(track.getBestFinish(TPlayer));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        } else {
            plugin.sendMessage(p, "messages.timer.finish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(TPlayer).getTime()));
            track.newTimeTrialFinish(mapTime, p.getUniqueId());
        }

        ApiUtilities.msgConsole(p.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(mapTime));

        Player player = TPlayer.getPlayer();

        if (!track.isOpen() && !TimingSystem.getPlugin().override.contains(TPlayer.getUniqueId())) {
            return;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack()) {
            return;
        }

        ApiUtilities.msgConsole(TPlayer.getName() + " started on " + track.getDisplayName());
        this.startTime = TimingSystem.currentTime;
        this.checkpoints = new boolean[track.getCheckpoints().size()];
    }

    public void playerResetMap() {
        if (track.hasOption('c')) {
            int lastCheckpoint = getLatestCheckpoint();
            if (lastCheckpoint != 0) {
                var checkpoint = track.getCheckpoints().get(lastCheckpoint);
                TPlayer.getPlayer().teleport(checkpoint.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                if (track.getType() == Track.TrackType.BOAT) {
                    Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
                        Boat boat = ApiUtilities.spawnBoat(checkpoint.getSpawnLocation());
                        boat.addPassenger(TPlayer.getPlayer());
                        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
                            boat.setWoodType(Database.getPlayer(TPlayer.getUniqueId()).getBoat());
                        }, 1);
                    }, 1);
                }
                return;
            }
        }
        TPlayer.getPlayer().teleport(track.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        TimeTrialController.timeTrials.remove(TPlayer.getUniqueId());
        ApiUtilities.msgConsole(TPlayer.getName() + " has been reset on " + track.getDisplayName());
    }

    public void playerStartingMap() {
        Player player = TPlayer.getPlayer();

        if (!track.isOpen() && !TimingSystem.getPlugin().override.contains(TPlayer.getUniqueId())) {
            return;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack()) {
            return;
        }
        TimeTrialController.timeTrials.put(TPlayer.getUniqueId(), this);
        ApiUtilities.msgConsole(TPlayer.getName() + " started on " + track.getDisplayName());
    }

    public void playerEndedMap() {
        Instant endTime = TimingSystem.currentTime;
        Player p = TPlayer.getPlayer();

        if (!hasPassedAllCheckpoints()) {
            plugin.sendMessage(p, "messages.error.timer.missedCheckpoints");
            TimeTrialController.timeTrials.remove(p.getUniqueId());
            return;
        }

        long mapTime = getTimeSinceStart(endTime);
        mapTime = Math.round(mapTime / 50) * 50;

        if (track.getBestFinish(TPlayer) == null) {
            plugin.sendMessage(p, "messages.timer.firstFinish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime));
            track.newTimeTrialFinish(mapTime, p.getUniqueId());
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        } else if (mapTime < track.getBestFinish(TPlayer).getTime()) {
            plugin.sendMessage(p, "messages.timer.newRecord", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(TPlayer).getTime()));
            track.newTimeTrialFinish(mapTime, p.getUniqueId());
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        } else {
            plugin.sendMessage(p, "messages.timer.finish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(TPlayer).getTime()));
            track.newTimeTrialFinish(mapTime, p.getUniqueId());
        }

        TimeTrialController.timeTrials.remove(p.getUniqueId());
        ApiUtilities.msgConsole(p.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(mapTime));
    }

    public void playerPassingCheckpoint(int checkpoint) {
        passCheckpoint(checkpoint);
        long timeSinceStart = getTimeSinceStart(TimingSystem.currentTime);
        timeSinceStart = Math.round(timeSinceStart / 50) * 50;
        if (TimingSystem.getPlugin().verbose.contains(TPlayer.getUniqueId())) {
            plugin.sendMessage(TPlayer.getPlayer(), "messages.timer.checkpoint", "%checkpoint%", String.valueOf(checkpoint), "%time%", ApiUtilities.formatAsTime(timeSinceStart));
        }
        ApiUtilities.msgConsole(TPlayer.getName() + " passed checkpoint " + checkpoint + " on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeSinceStart));
    }
}
