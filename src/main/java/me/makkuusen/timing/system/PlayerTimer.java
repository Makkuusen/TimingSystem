package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
                        ApiUtilities.sendActionBar("§a" + ApiUtilities.formatAsTime(mapTime) + timeTrial.getCheckpointsString(), p);
                    }
                    else if (mapTime < timeTrial.getBestFinish())
                    {
                        ApiUtilities.sendActionBar("§a" + ApiUtilities.formatAsTime(mapTime) + timeTrial.getCheckpointsString(), p);
                    }
                    else
                    {
                        ApiUtilities.sendActionBar("§c" + ApiUtilities.formatAsTime(mapTime) + timeTrial.getCheckpointsString(), p);
                    }
                }
                else
                {
                    for (Race race : RaceController.races.values()) {
                        if (race.isRunning())
                        {
                            if (race.hasRaceDriver(p.getUniqueId()))
                            {
                                RaceDriver rd = race.getRaceDrivers().get(p.getUniqueId());
                                if (rd.isFinished()) {
                                    ApiUtilities.sendActionBar("§a" + ApiUtilities.formatAsTime(race.getEndTime(rd)) + rd.getLapsString(race.getTotalLaps()), p);
                                }
                                else
                                {
                                    ApiUtilities.sendActionBar("§a" + ApiUtilities.formatAsTime(race.getCurrentTime()) + rd.getLapsString(race.getTotalLaps()), p);
                                }

                            }
                        }
                    }
                }
            }

        }, 5, 5);
    }

}
