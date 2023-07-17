package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.LeaderboardManager;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.TimeTrialAttemptEvent;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
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
            Text.send(player, Error.LAG_DETECTED);
            TimeTrialController.timeTrials.remove(player.getUniqueId());
            return;
        }

        if (track.hasRegion(TrackRegion.RegionType.LAGEND) && !lagEnd) {
            Text.send(player, Error.LAG_DETECTED);
            TimeTrialController.timeTrials.remove(player.getUniqueId());
            return;
        }


        if (hasNotPassedAllCheckpoints()) {
            Text.send(player, Error.MISSED_CHECKPOINTS);
            ApiUtilities.msgConsole(tPlayer.getName() + " started on " + track.getDisplayName());
            this.startTime = TimingSystem.currentTime;
            this.checkpoints = new boolean[track.getRegions(TrackRegion.RegionType.CHECKPOINT).size()];
            return;
        }

        long timeTrialTime = ApiUtilities.getRoundedToTick(getTimeSinceStart(endTime));

        if (track.getBestFinish(tPlayer) == null) {
            newBestFinish(player, timeTrialTime, -1);
            Text.send(player, Info.TIME_TRIAL_FIRST_FINISH,"%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%pos%", String.valueOf(track.getPlayerTopListPosition(tPlayer)));
        } else if (timeTrialTime < track.getBestFinish(tPlayer).getTime()) {
            var oldPos = track.getPlayerTopListPosition(tPlayer);
            var oldTime = track.getBestFinish(tPlayer).getTime();
            newBestFinish(player, timeTrialTime, oldTime);
            Text.send(player, Info.TIME_TRIAL_NEW_RECORD, "%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%oldTime%", ApiUtilities.formatAsTime(oldTime), "%oldPos%", oldPos.toString(), "%pos%", track.getPlayerTopListPosition(tPlayer).toString());
        } else {
            callTimeTrialFinishEvent(player, timeTrialTime, track.getBestFinish(tPlayer).getTime(), false);
            Text.send(player, Info.TIME_TRIAL_FINISH, "%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%oldTime%", ApiUtilities.formatAsPersonalGap(timeTrialTime - track.getBestFinish(tPlayer).getTime()));
        }

        ApiUtilities.msgConsole(player.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrialTime));

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

        if (!tPlayer.isTimeTrial()) {
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
        Player player = tPlayer.getPlayer();

        if (hasNotPassedAllCheckpoints()) {
            Text.send(player, Error.MISSED_CHECKPOINTS);
            TimeTrialController.timeTrials.remove(player.getUniqueId());
            return;
        }

        if (track.hasRegion(TrackRegion.RegionType.LAGSTART) && !lagStart) {
            Text.send(player, Error.LAG_DETECTED);
            TimeTrialController.timeTrials.remove(player.getUniqueId());
            return;
        }

        if (track.hasRegion(TrackRegion.RegionType.LAGEND) && !lagEnd) {
            Text.send(player, Error.LAG_DETECTED);
            TimeTrialController.timeTrials.remove(player.getUniqueId());
            return;
        }

        long timeTrialTime = ApiUtilities.getRoundedToTick(getTimeSinceStart(endTime));

        if (track.getBestFinish(tPlayer) == null) {
            newBestFinish(player, timeTrialTime, -1);
            Text.send(player, Info.TIME_TRIAL_FIRST_FINISH,"%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%pos%", String.valueOf(track.getPlayerTopListPosition(tPlayer)));
        } else if (timeTrialTime < track.getBestFinish(tPlayer).getTime()) {
            var oldPos = track.getPlayerTopListPosition(tPlayer);
            var oldTime = track.getBestFinish(tPlayer).getTime();
            newBestFinish(player, timeTrialTime, oldTime);
            Text.send(player, Info.TIME_TRIAL_NEW_RECORD, "%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%oldTime%", ApiUtilities.formatAsTime(oldTime), "%oldPos%", oldPos.toString(), "%pos%", track.getPlayerTopListPosition(tPlayer).toString());
        } else {
            callTimeTrialFinishEvent(player, timeTrialTime, track.getBestFinish(tPlayer).getTime(), false);
            Text.send(player, Info.TIME_TRIAL_FINISH, "%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%oldTime%", ApiUtilities.formatAsPersonalGap(timeTrialTime - track.getBestFinish(tPlayer).getTime()));
        }

        TimeTrialController.timeTrials.remove(player.getUniqueId());
        ApiUtilities.msgConsole(player.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrialTime));
    }

    public void playerPassingCheckpoint(int checkpoint) {
        passCheckpoint(checkpoint);
        long timeSinceStart = ApiUtilities.getRoundedToTick(getTimeSinceStart(TimingSystem.currentTime));
        if (tPlayer.isVerbose()) {
            Text.send(tPlayer.getPlayer(), Info.TIME_TRIAL_CHECKPOINT, "%checkpoint%", String.valueOf(checkpoint), "%time%", ApiUtilities.formatAsTime(timeSinceStart));
        }
        ApiUtilities.msgConsole(tPlayer.getName() + " passed checkpoint " + checkpoint + " on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeSinceStart));
    }

    public void playerPassingLagStart() {
        Player player = tPlayer.getPlayer();
        if (tPlayer.isVerbose() && (player.isOp() || player.hasPermission("track.admin"))) {
            Text.send(player, Info.TIME_TRIAL_LAG_START, "%time%", ApiUtilities.formatAsTime(ApiUtilities.getRoundedToTick(getTimeSinceStart(TimingSystem.currentTime))));
        }
    }

    public void playerPassingLagEnd() {
        Player player = tPlayer.getPlayer();
        if (tPlayer.isVerbose() && (player.isOp() || player.hasPermission("track.admin"))) {
            Text.send(player, Info.TIME_TRIAL_LAG_END, "%time%", ApiUtilities.formatAsTime(ApiUtilities.getRoundedToTick(getTimeSinceStart(TimingSystem.currentTime))));
        }
    }

    private void newBestFinish(Player p, long mapTime, long oldTime) {
        callTimeTrialFinishEvent(p, mapTime, oldTime, true);
        this.bestFinish = getBestFinish(track.getBestFinish(tPlayer));
        if (tPlayer.isSound()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
        }
        LeaderboardManager.updateFastestTimeLeaderboard(track);
    }


    private void callTimeTrialFinishEvent(Player player, long time, long oldBestTime, boolean newBestFinish) {
        var finish = track.newTimeTrialFinish(time, player.getUniqueId());
        TimeTrialFinishEvent eventTimeTrialFinish = new TimeTrialFinishEvent(player, finish, oldBestTime, newBestFinish);
        Bukkit.getServer().getPluginManager().callEvent(eventTimeTrialFinish);
    }
}
