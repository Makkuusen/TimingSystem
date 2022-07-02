package me.makkuusen.timing.system;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Duration;
import java.time.Instant;

public class TimeTrial{

    static Race plugin;
    private RPlayer rPlayer;
    private RaceTrack track;
    private Instant startTime;
    private boolean[] checkpoints;
    private long bestFinish;


    public TimeTrial(RaceTrack track, RPlayer player)
    {
        this.track = track;
        this.startTime = plugin.currentTime;
        this.checkpoints = new boolean[track.getCheckpoints().size()];
        this.bestFinish = getBestFinish(track.getBestFinish(player));
        this.rPlayer = player;

    }

    private long getBestFinish(RaceFinish raceFinish)
    {
        if (raceFinish == null)
        {
            return -1;
        }
        return raceFinish.getTime();
    }

    public long getBestFinish()
    {
        return bestFinish;
    }

    public RaceTrack getTrack()
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
        Instant endTime = Race.getPlugin().currentTime;
        Player p = rPlayer.getPlayer();

        if (!hasPassedAllCheckpoints())
        {
            plugin.sendMessage(p, "messages.error.timer.missedCheckpoints");
            return;
        }

        long mapTime = getTimeSinceStart(endTime);
        mapTime = Math.round(mapTime/50) * 50;

        if (track.getBestFinish(rPlayer) == null)
        {
            plugin.sendMessage(p, "messages.timer.firstFinish", "%map%", track.getName(), "%time%", RaceUtilities.formatAsTime(mapTime));
            track.newRaceFinish(mapTime, p.getUniqueId());
            this.bestFinish = getBestFinish(track.getBestFinish(rPlayer));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER,1,1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        }
        else if (mapTime < track.getBestFinish(rPlayer).getTime())
        {
            plugin.sendMessage(p, "messages.timer.newRecord", "%map%", track.getName(), "%time%", RaceUtilities.formatAsTime(mapTime), "%oldTime%", RaceUtilities.formatAsTime(track.getBestFinish(rPlayer).getTime()));
            track.newRaceFinish(mapTime, p.getUniqueId());
            this.bestFinish = getBestFinish(track.getBestFinish(rPlayer));
            p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,SoundCategory.MASTER,1,1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        }
        else
        {
            plugin.sendMessage(p, "messages.timer.finish", "%map%", track.getName(), "%time%", RaceUtilities.formatAsTime(mapTime), "%oldTime%", RaceUtilities.formatAsTime(track.getBestFinish(rPlayer).getTime()));
            track.newRaceFinish(mapTime, p.getUniqueId());
        }

        RaceUtilities.msgConsole(p.getName() + " finished " + track.getName() + " with a time of " + RaceUtilities.formatAsTime(mapTime));

        Player player = rPlayer.getPlayer();

        if (!track.isOpen() && !Race.getPlugin().override.contains(rPlayer.getUniqueId()))
        {
            return;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack())
        {
            return;
        }

        RaceUtilities.msgConsole(rPlayer.getName() + " started on " + track.getName());
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
                rPlayer.getPlayer().teleport(checkpoint.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                return;
            }
        }
        rPlayer.getPlayer().teleport(track.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        TimeTrialsController.timeTrials.remove(rPlayer.getUniqueId());
        RaceUtilities.msgConsole(rPlayer.getName() + " has been reset on " + track.getName());
    }

    public void playerStartingMap()
    {
        Player player = rPlayer.getPlayer();

        if (!track.isOpen() && !Race.getPlugin().override.contains(rPlayer.getUniqueId()))
        {
            return;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack())
        {
            return;
        }
        TimeTrialsController.timeTrials.put(rPlayer.getUniqueId(), this);
        RaceUtilities.msgConsole(rPlayer.getName() + " started on " + track.getName());
    }

    public void playerEndedMap()
    {
        Instant endTime = Race.getPlugin().currentTime;
        Player p = rPlayer.getPlayer();

        if (!hasPassedAllCheckpoints())
        {
            plugin.sendMessage(p, "messages.error.timer.missedCheckpoints");
            TimeTrialsController.timeTrials.remove(p.getUniqueId());
            return;
        }

        long mapTime = getTimeSinceStart(endTime);
        mapTime = Math.round(mapTime/50) * 50;

        if (track.getBestFinish(rPlayer) == null)
        {
            plugin.sendMessage(p, "messages.timer.firstFinish", "%map%", track.getName(), "%time%", RaceUtilities.formatAsTime(mapTime));
            track.newRaceFinish(mapTime, p.getUniqueId());
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER,1,1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        }
        else if (mapTime < track.getBestFinish(rPlayer).getTime())
        {
            plugin.sendMessage(p, "messages.timer.newRecord", "%map%", track.getName(), "%time%", RaceUtilities.formatAsTime(mapTime), "%oldTime%", RaceUtilities.formatAsTime(track.getBestFinish(rPlayer).getTime()));
            track.newRaceFinish(mapTime, p.getUniqueId());
            p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,SoundCategory.MASTER,1,1);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
        }
        else
        {
            plugin.sendMessage(p, "messages.timer.finish", "%map%", track.getName(), "%time%", RaceUtilities.formatAsTime(mapTime), "%oldTime%", RaceUtilities.formatAsTime(track.getBestFinish(rPlayer).getTime()));
            track.newRaceFinish(mapTime, p.getUniqueId());
        }

        TimeTrialsController.timeTrials.remove(p.getUniqueId());
        RaceUtilities.msgConsole(p.getName() + " finished " + track.getName() + " with a time of " + RaceUtilities.formatAsTime(mapTime));
    }

    public void playerPassingCheckpoint(int checkpoint)
    {
        passCheckpoint(checkpoint);
        long timeSinceStart = getTimeSinceStart(Instant.now());
        if (Race.getPlugin().verbose.contains(rPlayer.getUniqueId()))
        {
            plugin.sendMessage(rPlayer.getPlayer(),"messages.timer.checkpoint", "%checkpoint%", String.valueOf(checkpoint), "%time%", RaceUtilities.formatAsTime(timeSinceStart));
        }
        RaceUtilities.msgConsole(rPlayer.getName() + " passed checkpoint " + checkpoint + " on " + track.getName() + " with a time of " + RaceUtilities.formatAsTime(timeSinceStart));
    }
}
