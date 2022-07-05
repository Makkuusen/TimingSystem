package me.makkuusen.timing.system;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Duration;
import java.time.Instant;

public class TimeTrial{

    public static TimingSystem plugin;
    private me.makkuusen.timing.system.TSPlayer TSPlayer;
    private Track track;
    private Instant startTime;
    private boolean[] checkpoints;
    private long bestFinish;


    public TimeTrial(Track track, TSPlayer player)
    {
        this.track = track;
        this.startTime = plugin.currentTime;
        this.checkpoints = new boolean[track.getCheckpoints().size()];
        this.bestFinish = getBestFinish(track.getBestFinish(player));
        this.TSPlayer = player;

    }

    private long getBestFinish(TimeTrialFinish timeTrialFinish)
    {
        if (timeTrialFinish == null)
        {
            return -1;
        }
        return timeTrialFinish.getTime();
    }

    public long getBestFinish()
    {
        return bestFinish;
    }

    public Track getTrack()
    {
        return track;
    }

    private void passCheckpoint(int checkpoint)
    {
        checkpoint -= 1;
        try
        {
            for (int i = 0; i < checkpoints.length; i++)
            {
                if (i == checkpoint)
                {
                    this.checkpoints[i] = true;
                }
                else if (!this.checkpoints[i])
                {
                    return;
                }
            }
        } catch (NullPointerException e) { }
    }

    private boolean hasPassedAllCheckpoints()
    {
        for (boolean b : checkpoints)
        {
            if (!b)
            {
                return false;
            }
        }
        return true;
    }

    public int getPassedCheckpoints()
    {
        int count = 0;
        for (int i = 0; i < checkpoints.length; i++)
        {
            if (this.checkpoints[i])
            {
                count++;
            }
        }

        return count;
    }

    public int getNextCheckpoint()
    {
        for (int i = 0; i < checkpoints.length; i++)
        {
            if (!this.checkpoints[i])
            {
                return i+1;
            }
        }
        return checkpoints.length;
    }

    public long getCurrentTime()
    {
        return Duration.between(startTime, Instant.now()).toMillis();
    }

    private long getTimeSinceStart(Instant time)
    {
        return Duration.between(startTime, time).toMillis();
    }

    public int getLatestCheckpoint()
    {
        for (int i = 0; i < checkpoints.length; i++)
        {
            if (!this.checkpoints[i])
            {
                return i;
            }
        }
        return checkpoints.length;
    }


    public String getCheckpointsString()
    {
        if (checkpoints.length > 0)
        {
            return " (" + getPassedCheckpoints() + "/" + checkpoints.length + ")";
        }
        return "";
    }

    public void playerRestartMap()
    {
        Instant endTime = TimingSystem.getPlugin().currentTime;
        Player p = TSPlayer.getPlayer();

        if (!hasPassedAllCheckpoints())
        {
            plugin.sendMessage(p, "messages.error.timer.missedCheckpoints");
            return;
        }

        long mapTime = getTimeSinceStart(endTime);
        mapTime = Math.round(mapTime/50) * 50;

        if (track.getBestFinish(TSPlayer) == null)
        {
            plugin.sendMessage(p, "messages.timer.firstFinish", "%map%", track.getName(), "%time%", ApiUtilities.formatAsTime(mapTime));
            track.newRaceFinish(mapTime, p.getUniqueId());
            this.bestFinish = getBestFinish(track.getBestFinish(TSPlayer));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER,1,1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        }
        else if (mapTime < track.getBestFinish(TSPlayer).getTime())
        {
            plugin.sendMessage(p, "messages.timer.newRecord", "%map%", track.getName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(TSPlayer).getTime()));
            track.newRaceFinish(mapTime, p.getUniqueId());
            this.bestFinish = getBestFinish(track.getBestFinish(TSPlayer));
            p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,SoundCategory.MASTER,1,1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        }
        else
        {
            plugin.sendMessage(p, "messages.timer.finish", "%map%", track.getName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(TSPlayer).getTime()));
            track.newRaceFinish(mapTime, p.getUniqueId());
        }

        ApiUtilities.msgConsole(p.getName() + " finished " + track.getName() + " with a time of " + ApiUtilities.formatAsTime(mapTime));

        Player player = TSPlayer.getPlayer();

        if (!track.isOpen() && !TimingSystem.getPlugin().override.contains(TSPlayer.getUniqueId()))
        {
            return;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack())
        {
            return;
        }

        ApiUtilities.msgConsole(TSPlayer.getName() + " started on " + track.getName());
        this.startTime = plugin.currentTime;
        this.checkpoints = new boolean[track.getCheckpoints().size()];
    }

    public void playerResetMap()
    {
        if (track.hasOption('c'))
        {
            int lastCheckpoint = getLatestCheckpoint();
            if (lastCheckpoint != 0)
            {
                var checkpoint = track.getCheckpoints().get(lastCheckpoint);
                TSPlayer.getPlayer().teleport(checkpoint.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                return;
            }
        }
        TSPlayer.getPlayer().teleport(track.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        TimeTrialController.timeTrials.remove(TSPlayer.getUniqueId());
        ApiUtilities.msgConsole(TSPlayer.getName() + " has been reset on " + track.getName());
    }

    public void playerStartingMap()
    {
        Player player = TSPlayer.getPlayer();

        if (!track.isOpen() && !TimingSystem.getPlugin().override.contains(TSPlayer.getUniqueId()))
        {
            return;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack())
        {
            return;
        }
        TimeTrialController.timeTrials.put(TSPlayer.getUniqueId(), this);
        ApiUtilities.msgConsole(TSPlayer.getName() + " started on " + track.getName());
    }

    public void playerEndedMap()
    {
        Instant endTime = TimingSystem.getPlugin().currentTime;
        Player p = TSPlayer.getPlayer();

        if (!hasPassedAllCheckpoints())
        {
            plugin.sendMessage(p, "messages.error.timer.missedCheckpoints");
            TimeTrialController.timeTrials.remove(p.getUniqueId());
            return;
        }

        long mapTime = getTimeSinceStart(endTime);
        mapTime = Math.round(mapTime/50) * 50;

        if (track.getBestFinish(TSPlayer) == null)
        {
            plugin.sendMessage(p, "messages.timer.firstFinish", "%map%", track.getName(), "%time%", ApiUtilities.formatAsTime(mapTime));
            track.newRaceFinish(mapTime, p.getUniqueId());
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER,1,1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        }
        else if (mapTime < track.getBestFinish(TSPlayer).getTime())
        {
            plugin.sendMessage(p, "messages.timer.newRecord", "%map%", track.getName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(TSPlayer).getTime()));
            track.newRaceFinish(mapTime, p.getUniqueId());
            p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,SoundCategory.MASTER,1,1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        }
        else
        {
            plugin.sendMessage(p, "messages.timer.finish", "%map%", track.getName(), "%time%", ApiUtilities.formatAsTime(mapTime), "%oldTime%", ApiUtilities.formatAsTime(track.getBestFinish(TSPlayer).getTime()));
            track.newRaceFinish(mapTime, p.getUniqueId());
        }

        TimeTrialController.timeTrials.remove(p.getUniqueId());
        ApiUtilities.msgConsole(p.getName() + " finished " + track.getName() + " with a time of " + ApiUtilities.formatAsTime(mapTime));
    }

    public void playerPassingCheckpoint(int checkpoint)
    {
        passCheckpoint(checkpoint);
        long timeSinceStart = getTimeSinceStart(Instant.now());
        if (TimingSystem.getPlugin().verbose.contains(TSPlayer.getUniqueId()))
        {
            plugin.sendMessage(TSPlayer.getPlayer(),"messages.timer.checkpoint", "%checkpoint%", String.valueOf(checkpoint), "%time%", ApiUtilities.formatAsTime(timeSinceStart));
        }
        ApiUtilities.msgConsole(TSPlayer.getName() + " passed checkpoint " + checkpoint + " on " + track.getName() + " with a time of " + ApiUtilities.formatAsTime(timeSinceStart));
    }
}
