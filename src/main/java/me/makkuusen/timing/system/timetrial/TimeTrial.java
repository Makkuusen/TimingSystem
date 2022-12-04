package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.LeaderboardManager;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Duration;
import java.time.Instant;

public class TimeTrial {

    public static TimingSystem plugin;
    private final TPlayer tPlayer;
    private final Track track;
    private Instant startTime;
    private boolean[] checkpoints;
    private long bestFinish;


    public TimeTrial(Track track, TPlayer player) {
        this.track = track;
        this.startTime = TimingSystem.currentTime;
        this.checkpoints = new boolean[track.getRegions(TrackRegion.RegionType.CHECKPOINT).size()];
        this.bestFinish = getBestFinish(track.getBestFinish(player));
        this.tPlayer = player;

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

    public long getTimeSinceStart(Instant time) {
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
        Player p = tPlayer.getPlayer();
        Integer oldPos = null;

        if(track.getBestFinish(tPlayer) != null) oldPos = track.getPlayerTopListPosition(tPlayer);

        if (!track.isOpen() && !tPlayer.isOverride()) {
            TimeTrialController.timeTrials.remove(p.getUniqueId());
            return;
        }

        if (!p.isInsideVehicle() && track.isBoatTrack()) {
            TimeTrialController.timeTrials.remove(p.getUniqueId());
            return;
        }

        if (!hasPassedAllCheckpoints()) {
            plugin.sendMessage(p, "messages.error.timer.missedCheckpoints");
            ApiUtilities.msgConsole(tPlayer.getName() + " started on " + track.getDisplayName());
            this.startTime = TimingSystem.currentTime;
            this.checkpoints = new boolean[track.getRegions(TrackRegion.RegionType.CHECKPOINT).size()];
            return;
        }

        long mapTime = ApiUtilities.getRoundedToTick(getTimeSinceStart(endTime));

        if (track.getBestFinish(tPlayer) == null) {
            newBestFinish(p, mapTime);
            p.sendMessage(plugin.getLocalizedMessage(p, "messages.timer.firstFinish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime)) + String.format(" (§e#- §6→ §e#%s§6)", track.getPlayerTopListPosition(tPlayer)));
        } else if (mapTime < track.getBestFinish(tPlayer).getTime()) {
            newBestFinish(p, mapTime);
            p.sendMessage(plugin.getLocalizedMessage(p, "messages.timer.newRecord", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(tPlayer).getTime())) + String.format(" (§e#%s §6→ §e#%s§6)", oldPos.toString(), track.getPlayerTopListPosition(tPlayer).toString()));
        } else {
            plugin.sendMessage(p, "messages.timer.finish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(tPlayer).getTime()));
            track.newTimeTrialFinish(mapTime, p.getUniqueId());
        }

        ApiUtilities.msgConsole(p.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(mapTime));

        ApiUtilities.msgConsole(tPlayer.getName() + " started on " + track.getDisplayName());
        this.startTime = TimingSystem.currentTime;
        this.checkpoints = new boolean[track.getRegions(TrackRegion.RegionType.CHECKPOINT).size()];
    }

    public void playerResetMap() {
        var timeTrial = TimeTrialController.timeTrials.get(tPlayer.getUniqueId());
        if (timeTrial == null) {
            return;
        }
        var time = ApiUtilities.getRoundedToTick(timeTrial.getTimeSinceStart(TimingSystem.currentTime));
        timeTrial.getTrack().newTimeTrialAttempt(time, tPlayer.getUniqueId());
        TimeTrialController.timeTrials.remove(tPlayer.getUniqueId());
        ApiUtilities.teleportPlayerAndSpawnBoat(tPlayer.getPlayer(), track.isBoatTrack(), track.getSpawnLocation());
        ApiUtilities.msgConsole(tPlayer.getName() + " has been reset on " + track.getDisplayName());

    }

    public void playerStartingMap() {
        Player player = tPlayer.getPlayer();

        if(!tPlayer.isTimeTrial()){
            return;
        }

        if (!track.isOpen() && !tPlayer.isOverride()) {
            return;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack()) {
            return;
        }

        if (track.isBoatTrack() && !(player.getVehicle() instanceof Boat)) {
            return;
        }
        TimeTrialController.timeTrials.put(tPlayer.getUniqueId(), this);
        ApiUtilities.msgConsole(tPlayer.getName() + " started on " + track.getDisplayName());
    }

    public void playerEndedMap() {
        Instant endTime = TimingSystem.currentTime;
        Player p = tPlayer.getPlayer();

        if (!hasPassedAllCheckpoints()) {
            plugin.sendMessage(p, "messages.error.timer.missedCheckpoints");
            TimeTrialController.timeTrials.remove(p.getUniqueId());
            return;
        }

        long mapTime = ApiUtilities.getRoundedToTick(getTimeSinceStart(endTime));

        if (track.getBestFinish(tPlayer) == null) {
            plugin.sendMessage(p, "messages.timer.firstFinish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime));
            newBestFinish(p, mapTime);
        } else if (mapTime < track.getBestFinish(tPlayer).getTime()) {
            plugin.sendMessage(p, "messages.timer.newRecord", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(tPlayer).getTime()));
            newBestFinish(p, mapTime);
        } else {
            plugin.sendMessage(p, "messages.timer.finish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(tPlayer).getTime()));
            track.newTimeTrialFinish(mapTime, p.getUniqueId());
        }

        TimeTrialController.timeTrials.remove(p.getUniqueId());
        ApiUtilities.msgConsole(p.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(mapTime));
    }

    public void playerPassingCheckpoint(int checkpoint) {
        passCheckpoint(checkpoint);
        long timeSinceStart = ApiUtilities.getRoundedToTick(getTimeSinceStart(TimingSystem.currentTime));
        if (tPlayer.isVerbose()) {
            plugin.sendMessage(tPlayer.getPlayer(), "messages.timer.checkpoint", "%checkpoint%", String.valueOf(checkpoint), "%time%", ApiUtilities.formatAsTime(timeSinceStart));
        }
        ApiUtilities.msgConsole(tPlayer.getName() + " passed checkpoint " + checkpoint + " on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeSinceStart));
    }

    private void newBestFinish(Player p, long mapTime){
        var finish = track.newTimeTrialFinish(mapTime, p.getUniqueId());
        TimeTrialFinishEvent eventTimeTrialFinish = new TimeTrialFinishEvent(p, finish, bestFinish);
        Bukkit.getServer().getPluginManager().callEvent(eventTimeTrialFinish);
        this.bestFinish = getBestFinish(track.getBestFinish(tPlayer));
        if (tPlayer.isSound()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
        }
        LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
    }
}
