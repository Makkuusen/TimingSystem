package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class PlayerTimer {

    public static void initPlayerTimer()
    {

        Bukkit.getScheduler().scheduleSyncRepeatingTask(TimingSystem.getPlugin(), () -> {

            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (TimeTrialsController.timeTrials.containsKey(p.getUniqueId()))
                {
                    TimeTrial timeTrial = TimeTrialsController.timeTrials.get(p.getUniqueId());
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
                else
                {
                    for (Race race : RaceController.races.values()) {
                        if (race.isRunning)
                        {
                            if (race.raceDrivers.containsKey(p.getUniqueId()))
                            {
                                RaceDriver rd = race.raceDrivers.get(p.getUniqueId());
                                if (rd.isFinished()) {
                                    RaceUtilities.sendActionBar("§a" + RaceUtilities.formatAsTime(race.getEndTime(rd)) + rd.getLapsString(race.getTotalLaps()), p);
                                }
                                else
                                {
                                    RaceUtilities.sendActionBar("§a" + RaceUtilities.formatAsTime(race.getCurrentTime()) + rd.getLapsString(race.getTotalLaps()), p);
                                }

                            }
                        }
                    }
                }
            }

        }, 5, 5);
    }

}
