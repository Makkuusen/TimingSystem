package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.LeaderboardManager;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.TimeTrialAttemptEvent;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackRegion;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeTrial {

    private final TPlayer tPlayer;
    private final Track track;
    private Instant startTime;
    private ArrayList<Instant> checkpoints;
    private boolean lagStart = false;
    private Instant lagStartTime = null;
    private boolean lagEnd = false;
    private TimeTrialFinish bestFinish;


    public TimeTrial(Track track, TPlayer player) {
        this.track = track;
        this.startTime = TimingSystem.currentTime;
        this.checkpoints = new ArrayList<>();
        this.bestFinish = track.getBestFinish(player);
        this.tPlayer = player;
    }


    public long getBestTime() {
        if (bestFinish == null) {
            return -1;
        }
        return bestFinish.getTime();
    }

    public TimeTrialFinish getBestFinish() {
        return bestFinish;
    }

    public Track getTrack() {
        return track;
    }

    private boolean hasNotPassedAllCheckpoints() {
        return checkpoints.size() != track.getRegions(TrackRegion.RegionType.CHECKPOINT).size();
    }

    public int getNextCheckpoint() {
        if (track.getRegions(TrackRegion.RegionType.CHECKPOINT).size() >= checkpoints.size()) {
            return checkpoints.size() + 1;
        }
        return checkpoints.size();
    }

    public void passNextCheckpoint(Instant timeStamp) {
        checkpoints.add(timeStamp);
    }

    public int getLatestCheckpoint() {
        return checkpoints.size();
    }

    public long getCheckpointTime(int checkpoint) {
        if (checkpoints.size() == 0 || checkpoint == 0) {
            return 0;
        }
        return getTimeSinceStart(checkpoints.get(checkpoint - 1));
    }

    public List<Long> getCheckpointTimes() {
        List<Long> checkpointTimes = new ArrayList<>();
        checkpoints.forEach(checkpoint -> checkpointTimes.add(ApiUtilities.getRoundedToTick(getTimeSinceStart(checkpoint))));
        return checkpointTimes;
    }

    public long getCurrentTime() {
        return Duration.between(startTime, Instant.now()).toMillis();
    }

    public long getTimeSinceStart(Instant time) {
        return Duration.between(startTime, time).toMillis();
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

    public void playerPassingNextCheckpoint() {
        passNextCheckpoint(TimingSystem.currentTime);
        long timeSinceStart = ApiUtilities.getRoundedToTick(getTimeSinceStart(TimingSystem.currentTime));
        if (tPlayer.isVerbose()) {

            Component delta = getBestLapDelta(tPlayer.getTheme(), getLatestCheckpoint());
            tPlayer.getPlayer().sendMessage(Text.get(tPlayer.getPlayer(), Info.TIME_TRIAL_CHECKPOINT, "%checkpoint%", String.valueOf(getLatestCheckpoint()), "%time%", ApiUtilities.formatAsTime(timeSinceStart)).append(delta));
        }
        ApiUtilities.msgConsole(tPlayer.getName() + " passed checkpoint " + getLatestCheckpoint() + " on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeSinceStart));
    }

    public void playerResetMap() {
        var timeTrial = TimeTrialController.timeTrials.get(tPlayer.getUniqueId());
        if (timeTrial == null) {
            return;
        }
        var time = ApiUtilities.getRoundedToTick(getTimeSinceStart(TimingSystem.currentTime));
        var attempt = getTrack().newTimeTrialAttempt(time, tPlayer.getUniqueId());
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

        if (validateFinish(player)) {
            long timeTrialTime = ApiUtilities.getRoundedToTick(getTimeSinceStart(endTime));
            saveAndAnnounceFinish(player, timeTrialTime);
            ApiUtilities.msgConsole(player.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrialTime));
        }
        TimeTrialController.timeTrials.remove(player.getUniqueId());
    }

    public void playerRestartMap() {
        Instant endTime = TimingSystem.currentTime;
        Player player = tPlayer.getPlayer();

        if (validateFinish(player)){
            long timeTrialTime = ApiUtilities.getRoundedToTick(getTimeSinceStart(endTime));
            saveAndAnnounceFinish(player, timeTrialTime);
            ApiUtilities.msgConsole(player.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrialTime));
        }

        if (!track.isOpen() && !tPlayer.isOverride()) {
            TimeTrialController.timeTrials.remove(player.getUniqueId());
        } else {
            resetTimeTrial();
        }
    }

    private boolean validateFinish(Player player) {
        if (hasNotPassedAllCheckpoints()) {
            Text.send(player, Error.MISSED_CHECKPOINTS);
            return false;
        }

        if (track.hasRegion(TrackRegion.RegionType.LAGSTART) && !lagStart) {
            Text.send(player, Error.LAG_DETECTED);
            return false;
        }

        if (track.hasRegion(TrackRegion.RegionType.LAGEND) && !lagEnd) {
            Text.send(player, Error.LAG_DETECTED);
            return false;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack()) {
            return false;
        }

        return true;
    }

    private void resetTimeTrial() {
        ApiUtilities.msgConsole(tPlayer.getName() + " started on " + track.getDisplayName());
        this.startTime = TimingSystem.currentTime;
        this.checkpoints = new ArrayList<>();
        this.lagStart = false;
        this.lagEnd = false;
        this.lagStartTime = null;
    }

    private void saveAndAnnounceFinish(Player player, long timeTrialTime) {

        Component finishMessage;
        TimeTrialFinish finish;
        if (track.getBestFinish(tPlayer) == null) {
            //First finish
            finish = newBestFinish(player, timeTrialTime, -1);
            finishMessage = Text.get(player, Info.TIME_TRIAL_FIRST_FINISH,"%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%pos%", String.valueOf(track.getPlayerTopListPosition(tPlayer)));
            finishMessage = tPlayer.getTheme().getCheckpointHovers(finish, finishMessage);
        } else if (timeTrialTime < track.getBestFinish(tPlayer).getTime()) {
            //New personal best
            var oldPos = track.getCachedPlayerPosition(tPlayer);
            var oldFinish = track.getBestFinish(tPlayer);
            finish = newBestFinish(player, timeTrialTime, oldFinish.getTime());
            finishMessage = Text.get(player, Info.TIME_TRIAL_NEW_RECORD, "%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%delta%", ApiUtilities.formatAsPersonalGap(oldFinish.getTime() - timeTrialTime), "%oldPos%", oldPos.toString(), "%pos%", track.getPlayerTopListPosition(tPlayer).toString());
            finishMessage = tPlayer.getTheme().getCheckpointHovers(finish, oldFinish, finishMessage);
        } else {
            //Finish no improvement
            finish = callTimeTrialFinishEvent(player, timeTrialTime, track.getBestFinish(tPlayer).getTime(), false);
            finishMessage = Text.get(player, Info.TIME_TRIAL_FINISH, "%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%delta%", ApiUtilities.formatAsPersonalGap(timeTrialTime - track.getBestFinish(tPlayer).getTime()));
            finishMessage = tPlayer.getTheme().getCheckpointHovers(finish, track.getBestFinish(tPlayer), finishMessage);

        }

        player.sendMessage(finishMessage);
    }

    private TimeTrialFinish newBestFinish(Player p, long mapTime, long oldTime) {
        var finish = callTimeTrialFinishEvent(p, mapTime, oldTime, true);
        this.bestFinish = track.getBestFinish(tPlayer);
        if (tPlayer.isSound()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
        }
        LeaderboardManager.updateFastestTimeLeaderboard(track);

        return finish;
    }


    private TimeTrialFinish callTimeTrialFinishEvent(Player player, long time, long oldBestTime, boolean newBestFinish) {
        var finish = track.newTimeTrialFinish(time, player.getUniqueId());
        Map<Integer, Long> checkpointTimes = new HashMap<>();
        for(int i = 1; i <= checkpoints.size(); i++) {
            checkpointTimes.put(i, getCheckpointTime(i));
        }
        if (newBestFinish) {
            //only store checkpoints on personal best
            finish.insertCheckpoints(checkpointTimes);
        } else {
            finish.updateCheckpointTimes(checkpointTimes);
        }

        TimeTrialFinishEvent eventTimeTrialFinish = new TimeTrialFinishEvent(player, finish, oldBestTime, newBestFinish);
        Bukkit.getServer().getPluginManager().callEvent(eventTimeTrialFinish);
        return finish;
    }

    public Component getBestLapDelta(Theme theme, int latestCheckpoint) {
        if (latestCheckpoint > 0) {
            if (getBestFinish() != null && getBestFinish().hasCheckpointTimes() && getBestFinish().getCheckpointTime(latestCheckpoint) != null) {
                if (getBestFinish().getDate() > getTrack().getDateChanged()) {
                    var bestCheckpoint = getBestFinish().getCheckpointTime(latestCheckpoint);
                    var currentCheckpoint = getCheckpointTime(latestCheckpoint);
                    if (ApiUtilities.getRoundedToTick(bestCheckpoint) <= ApiUtilities.getRoundedToTick(currentCheckpoint)) {
                        return Component.text(" +" + ApiUtilities.formatAsPersonalGap(currentCheckpoint - bestCheckpoint)).color(theme.getError());
                    } else {
                        return Component.text(" -" + ApiUtilities.formatAsPersonalGap(bestCheckpoint - currentCheckpoint)).color(theme.getSuccess());
                    }
                }
            }
        }
        return Component.empty();
    }


}
