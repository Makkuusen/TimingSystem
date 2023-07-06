package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.LeaderboardManager;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.TimeTrialAttemptEvent;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;

public class TimeTrial {

    public static TimingSystem plugin;
    private final TPlayer tPlayer;
    private final Track track;
    private Instant startTime;
    private boolean[] checkpoints;
    private boolean lagStart = false;
    private Instant lagStartTime = null;
    private boolean lagEnd = false;
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
        } catch (NullPointerException ignored) {
        }
    }

    private boolean hasNotPassedAllCheckpoints() {
        for (boolean b : checkpoints) {
            if (!b) {
                return true;
            }
        }
        return false;
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

    public boolean isLagStart() {
        return lagStart;
    }

    public void setLagStartTrue() {
        this.lagStart = true;
        lagStartTime = TimingSystem.currentTime;
    }

    public Instant getLagStart() {
        return lagStartTime;
    }

    public boolean isLagEnd() {
        return lagEnd;
    }

    public void setLagEnd(boolean lagEnd) {
        this.lagEnd = lagEnd;
    }

    public void playerRestartMap() {
        Instant endTime = TimingSystem.currentTime;
        Player player = tPlayer.getPlayer();

        if (!track.isOpen() && !tPlayer.isOverride()) {
            TimeTrialController.timeTrials.remove(player.getUniqueId());
            return;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack()) {
            TimeTrialController.timeTrials.remove(player.getUniqueId());
            return;
        }


        if (track.hasRegion(TrackRegion.RegionType.LAGSTART) && !lagStart) {
            player.sendMessage("§cTimingSystem detected some lag and unfortunately your time has been invalidated.");
            TimeTrialController.timeTrials.remove(player.getUniqueId());
            return;
        }

        if (track.hasRegion(TrackRegion.RegionType.LAGEND) && !lagEnd) {
            player.sendMessage("§cTimingSystem detected some lag and unfortunately your time has been invalidated.");
            TimeTrialController.timeTrials.remove(player.getUniqueId());
            return;
        }


        if (hasNotPassedAllCheckpoints()) {
            plugin.sendMessage(player, "messages.error.timer.missedCheckpoints");
            ApiUtilities.msgConsole(tPlayer.getName() + " started on " + track.getDisplayName());
            this.startTime = TimingSystem.currentTime;
            this.checkpoints = new boolean[track.getRegions(TrackRegion.RegionType.CHECKPOINT).size()];
            return;
        }

        long mapTime = ApiUtilities.getRoundedToTick(getTimeSinceStart(endTime));

        if (track.getBestFinish(tPlayer) == null) {
            newBestFinish(player, mapTime, -1);
            player.sendMessage(plugin.getLocalizedMessage(player, "messages.timer.firstFinish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime)) + String.format(" (§e#%s§6)", track.getPlayerTopListPosition(tPlayer)));
        } else if (mapTime < track.getBestFinish(tPlayer).getTime()) {
            var oldPos = track.getPlayerTopListPosition(tPlayer);
            var oldTime = track.getBestFinish(tPlayer).getTime();
            newBestFinish(player, mapTime, oldTime);
            player.sendMessage(plugin.getLocalizedMessage(player, "messages.timer.newRecord", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(oldTime)) + String.format(" (§e#%s §6-> §e#%s§6)", oldPos.toString(), track.getPlayerTopListPosition(tPlayer).toString()));
        } else {
            callTimeTrialFinishEvent(player, mapTime, track.getBestFinish(tPlayer).getTime(), false);
            plugin.sendMessage(player, "messages.timer.finish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(tPlayer).getTime()));
        }

        ApiUtilities.msgConsole(player.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(mapTime));

        ApiUtilities.msgConsole(tPlayer.getName() + " started on " + track.getDisplayName());
        this.startTime = TimingSystem.currentTime;
        this.checkpoints = new boolean[track.getRegions(TrackRegion.RegionType.CHECKPOINT).size()];
        this.lagStart = false;
        this.lagEnd = false;
        this.lagStartTime = null;
    }

    public void playerResetMap() {
        var timeTrial = TimeTrialController.timeTrials.get(tPlayer.getUniqueId());
        if (timeTrial == null) {
            return;
        }
        var time = ApiUtilities.getRoundedToTick(timeTrial.getTimeSinceStart(TimingSystem.currentTime));
        var attempt = timeTrial.getTrack().newTimeTrialAttempt(time, tPlayer.getUniqueId());
        var eventTimeTrialAttempt = new TimeTrialAttemptEvent(tPlayer.getPlayer(), attempt);
        Bukkit.getServer().getPluginManager().callEvent(eventTimeTrialAttempt);
        TimeTrialController.timeTrials.remove(tPlayer.getUniqueId());
        ApiUtilities.teleportPlayerAndSpawnBoat(tPlayer.getPlayer(), track, track.getSpawnLocation());
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

        if (hasNotPassedAllCheckpoints()) {
            plugin.sendMessage(p, "messages.error.timer.missedCheckpoints");
            TimeTrialController.timeTrials.remove(p.getUniqueId());
            return;
        }

        if (track.hasRegion(TrackRegion.RegionType.LAGSTART) && !lagStart) {
            p.sendMessage("§cTimingSystem detected some lag and unfortunately your time has been invalidated.");
            TimeTrialController.timeTrials.remove(p.getUniqueId());
            return;
        }

        if (track.hasRegion(TrackRegion.RegionType.LAGEND) && !lagEnd) {
            p.sendMessage("§cTimingSystem detected some lag and unfortunately your time has been invalidated.");
            TimeTrialController.timeTrials.remove(p.getUniqueId());
            return;
        }

        long mapTime = ApiUtilities.getRoundedToTick(getTimeSinceStart(endTime));

        if (track.getBestFinish(tPlayer) == null) {
            newBestFinish(p, mapTime, -1);
            p.sendMessage(plugin.getLocalizedMessage(p, "messages.timer.firstFinish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime)) + String.format(" (§e#%s§6)", track.getPlayerTopListPosition(tPlayer)));
        } else if (mapTime < track.getBestFinish(tPlayer).getTime()) {
            var oldPos = track.getPlayerTopListPosition(tPlayer);
            var oldtime = track.getBestFinish(tPlayer).getTime();
            newBestFinish(p, mapTime, oldtime);
            p.sendMessage(plugin.getLocalizedMessage(p, "messages.timer.newRecord", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(oldtime)) + String.format(" (§e#%s §6-> §e#%s§6)", oldPos.toString(), track.getPlayerTopListPosition(tPlayer).toString()));
        } else {
            callTimeTrialFinishEvent(p, mapTime, track.getBestFinish(tPlayer).getTime(), false);
            plugin.sendMessage(p, "messages.timer.finish", "%map%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(tPlayer).getTime()));
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

    public void playerPassingLagStart() {
        Player player = tPlayer.getPlayer();
        if (tPlayer.isVerbose() && (player.isOp() || player.hasPermission("track.admin"))) {
            player.sendMessage("§2You passed §alagstart §2in §a" + ApiUtilities.formatAsTime(ApiUtilities.getRoundedToTick(getTimeSinceStart(TimingSystem.currentTime))));
        }
    }

    public void playerPassingLagEnd() {
        Player player = tPlayer.getPlayer();
        if (tPlayer.isVerbose() && (player.isOp() || player.hasPermission("track.admin"))) {
            player.sendMessage("§2You passed §alagend §2in §a" + ApiUtilities.formatAsTime(ApiUtilities.getRoundedToTick(getTimeSinceStart(TimingSystem.currentTime))));
        }
    }

    private void newBestFinish(Player p, long mapTime, long oldTime){
        callTimeTrialFinishEvent(p, mapTime, oldTime, true);
        this.bestFinish = getBestFinish(track.getBestFinish(tPlayer));
        if (tPlayer.isSound()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
        }
        LeaderboardManager.updateFastestTimeLeaderboard(track);
    }


    private void callTimeTrialFinishEvent(Player player, long time, long oldBestTime, boolean newBestFinish){
        var finish = track.newTimeTrialFinish(time, player.getUniqueId());
        TimeTrialFinishEvent eventTimeTrialFinish = new TimeTrialFinishEvent(player, finish, oldBestTime, newBestFinish);
        Bukkit.getServer().getPluginManager().callEvent(eventTimeTrialFinish);
    }
}
