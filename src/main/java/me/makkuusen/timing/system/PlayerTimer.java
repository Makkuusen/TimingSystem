package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerTimer {

    public static void initPlayerTimer()
    {

        Bukkit.getScheduler().scheduleSyncRepeatingTask(TimingSystem.getPlugin(), () -> {

            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (TimeTrialController.timeTrials.containsKey(p.getUniqueId()))
                {
                    TimeTrial timeTrial = TimeTrialController.timeTrials.get(p.getUniqueId());
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
                        if (!race.getRaceState().equals(RaceState.RACING)) { continue; }

                        if (!race.hasRaceDriver(p.getUniqueId())) { continue; }

                        RaceDriver rd = race.getRaceDrivers().get(p.getUniqueId());
                        if (rd.isFinished()) { continue; }


                        String message = TimingSystem.getPlugin().getLocalizedMessage(
                                rd.getPlayer(),
                                "messages.actionbar.race",
                                "%laps%" , String.valueOf(rd.getLaps()),
                                "%totalLaps%", String.valueOf(race.getTotalLaps()),
                                "%pos%", String.valueOf(rd.getPosition()),
                                "%pits%", String.valueOf(rd.getPits()),
                                "%totalPits%", String.valueOf(race.getTotalPits())
                        );
                        ApiUtilities.sendActionBar(message, rd.getPlayer());
                    }
                }
            }

        }, 5, 5);
    }

}
