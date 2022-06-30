package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class PlayerTimer
{

    private static HashMap<UUID, PlayerRunData> playersInMaps = new HashMap<>();

    public static void init()
    {
        playersInMaps = new HashMap<>();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(Race.getPlugin(), () -> {

			for (Player p : Bukkit.getOnlinePlayers())
			{
				if (PlayerTimer.isPlayerInMap(p))
				{
					PlayerRunData runData = playersInMaps.get(p.getUniqueId());
					long mapTime = runData.getCurrentTime();
					if (runData.getBestFinish() == -1)
					{
						RaceUtilities.sendActionBar("§a" + RaceUtilities.formatAsTime(mapTime) + runData.getCheckpointsString(), p);
					}
					else if (mapTime < runData.getBestFinish())
					{
						RaceUtilities.sendActionBar("§a" + RaceUtilities.formatAsTime(mapTime) + runData.getCheckpointsString(), p);
					}
					else
					{
						RaceUtilities.sendActionBar("§c" + RaceUtilities.formatAsTime(mapTime) + runData.getCheckpointsString(), p);
					}
				}
			}

		}, 5, 5);
    }


    public static void playerLeavingMap(Player p)
    {
        if (!playersInMaps.containsKey(p.getUniqueId()))
        {
            return;
        }
        playersInMaps.remove(p.getUniqueId());
    }

    public static boolean isPlayerInMap(Player p)
    {
        return playersInMaps.containsKey(p.getUniqueId());
    }

    public static void playerStartingMap(Player p, RaceTrack raceTrack)
    {
        if (raceTrack == null)
        {
            return;
        }

        if(!p.getGameMode().equals(GameMode.SURVIVAL)){
            return;
        }

        if (!raceTrack.isOpen() && !Race.getPlugin().override.contains(p.getUniqueId()))
        {
            return;
        }

		if (!p.isInsideVehicle() && raceTrack.isBoatTrack())
		{
			return;
		}
        RPlayer rPlayer = ApiDatabase.getPlayer(p.getUniqueId());
        playersInMaps.put(rPlayer.getUniqueId(), new PlayerRunData(raceTrack, rPlayer));
        RaceUtilities.msgConsole(rPlayer.getName() + " started on " + raceTrack.getName());
    }

    public static void playerPassingCheckpoint(Player p, RaceTrack m, int checkpoint)
    {
        if (!playersInMaps.containsKey(p.getUniqueId()))
        {
            return;
        }

        if (!playersInMaps.get(p.getUniqueId()).getTrack().equals(m))
        {
            return;
        }

        PlayerRunData prd = playersInMaps.get(p.getUniqueId());
        if (m.hasOption('u'))
        {
            prd.passUnorderedCheckpoint(checkpoint);
        }
        else
        {
            prd.passCheckpoint(checkpoint);
        }
        long timeSinceStart = prd.getTimeSinceStart(Instant.now());
        if (Race.getPlugin().verbose.contains(p.getUniqueId()))
        {
            p.sendMessage("§2Du passerade §akontrollpunkt " + checkpoint + "§2 på §a" + RaceUtilities.formatAsTime(timeSinceStart) + "§2.");
        }
        RaceUtilities.msgConsole(p.getName() + " passed checkpoint " + checkpoint + " on " + m.getName() + " with a time of " + RaceUtilities.formatAsTime(timeSinceStart));
    }

    public static void playerResetMap(Player p, RaceTrack raceTrack)
    {
        if (!playersInMaps.containsKey(p.getUniqueId()))
        {
            return;
        }

        if (!playersInMaps.get(p.getUniqueId()).getTrack().equals(raceTrack))
        {
            return;
        }

        if (raceTrack.hasOption('c'))
        {
            var run = playersInMaps.get(p.getUniqueId());
            int lastCheckpoint = run.getLatestCheckpoint();
            if (lastCheckpoint != 0)
            {
                var checkpoint = raceTrack.getCheckpoints().get(lastCheckpoint);
                p.teleport(checkpoint.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                return;
            }
        }
        p.teleport(raceTrack.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        playersInMaps.remove(p.getUniqueId());
        RaceUtilities.msgConsole(p.getName() + " has been reset on " + raceTrack.getName());
    }

    public static void playerCancelMap(Player p)
    {
        if (!playersInMaps.containsKey(p.getUniqueId()))
        {
            return;
        }
        RaceUtilities.msgConsole(p.getName() + " has cancelled run on " + playersInMaps.get(p.getUniqueId()).getTrack().getName());
        playersInMaps.remove(p.getUniqueId());
    }

    public static void playerEndedMap(Player p, RaceTrack raceTrack)
    {
        Instant endTime = Instant.now();

        RPlayer rPlayer = ApiDatabase.getPlayer(p.getUniqueId());

        if (!playersInMaps.containsKey(p.getUniqueId()))
        {
            return;
        }

        if (!playersInMaps.get(p.getUniqueId()).getTrack().equals(raceTrack))
        {
            return;
        }

        if (!playersInMaps.get(p.getUniqueId()).hasPassedAllCheckpoints())
        {
            p.sendMessage("§cDu passerade inte alla kontrollpunkter och tiden var ogiltig!");
            playersInMaps.remove(p.getUniqueId());
            return;
        }

        long mapTime = playersInMaps.get(p.getUniqueId()).getTimeSinceStart(endTime);

        if (raceTrack.getBestFinish(rPlayer) == null)
        {
            p.sendMessage("§eNytt rekord!§6 Du klarade §e" + raceTrack.getName() + "§6 på §e" + RaceUtilities.formatAsTime(mapTime) + "§6");
            raceTrack.newRaceFinish(mapTime, p.getUniqueId());
            p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,SoundCategory.MASTER,1,1);
            LeaderboardManager.updateFastestTimeLeaderboard(raceTrack.getId());
        }
        else if (mapTime < raceTrack.getBestFinish(rPlayer).getTime())
        {
            p.sendMessage("§eNytt rekord!§6 Du klarade §e" + raceTrack.getName() + "§6 på §e" + RaceUtilities.formatAsTime(mapTime) + "§6. Tidigare rekord var §e" + RaceUtilities.formatAsTime(raceTrack.getBestFinish(rPlayer).getTime()) + "§6.");
            raceTrack.newRaceFinish(mapTime, p.getUniqueId());
            p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,SoundCategory.MASTER,1,1);
            LeaderboardManager.updateFastestTimeLeaderboard(raceTrack.getId());
        }
        else
        {
            p.sendMessage("§2Du klarade §a" + raceTrack.getName() + "§2 på §a" + RaceUtilities.formatAsTime(mapTime) + "§2. " + "Ditt rekord är §a" + RaceUtilities.formatAsTime(raceTrack.getBestFinish(rPlayer).getTime()) + "§2.");
            raceTrack.newRaceFinish(mapTime, p.getUniqueId());
        }

        playersInMaps.remove(p.getUniqueId());
        RaceUtilities.msgConsole(p.getName() + " finished " + raceTrack.getName() + " with a time of " + RaceUtilities.formatAsTime(mapTime));
    }

    public static RaceTrack getTrackPlayerIsIn(Player player)
    {
        if (playersInMaps.get(player.getUniqueId()) == null)
        {
            return null;
        }
        return playersInMaps.get(player.getUniqueId()).getTrack();
    }
}
