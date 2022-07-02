package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class RaceController {

    static HashMap<UUID, TimeTrial> timeTrials = new HashMap<>();


    public static void initTimeTrials()
    {
        timeTrials = new HashMap<>();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(Race.getPlugin(), () -> {

            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (timeTrials.containsKey(p.getUniqueId()))
                {
                    TimeTrial timeTrial = timeTrials.get(p.getUniqueId());
                    long mapTime = timeTrial.getCurrentTime();
                    if (timeTrial.getBestFinish() == -1)
                    {
                        RaceUtilities.sendActionBar("§a" + RaceUtilities.formatAsTime(mapTime) + timeTrial.getCheckpointsString(), p);
                    }
                    else if (mapTime < timeTrial.getBestFinish())
                    {
                        RaceUtilities.sendActionBar("§a" + RaceUtilities.formatAsTime(mapTime) + timeTrial.getCheckpointsString(), p);
                    }
                    else
                    {
                        RaceUtilities.sendActionBar("§c" + RaceUtilities.formatAsTime(mapTime) + timeTrial.getCheckpointsString(), p);
                    }
                }
            }

        }, 5, 5);
    }

    public static void playerLeavingMap(UUID uuid)
    {
        if (!RaceController.timeTrials.containsKey(uuid))
        {
            return;
        }
        RaceController.timeTrials.remove(uuid);
    }

    public static void playerCancelMap(Player player)
    {
        if (!RaceController.timeTrials.containsKey(player.getUniqueId()))
        {
            return;
        }
        RaceUtilities.msgConsole(player.getName() + " has cancelled run on " + RaceController.timeTrials.get(player.getUniqueId()).getTrack().getName());
        RaceController.timeTrials.remove(player.getUniqueId());
    }
}
