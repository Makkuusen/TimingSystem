package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class PlayerTimer {
    static boolean update = false;


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

                        if (TimingSystem.configuration.isLasersItems()){

                            Player player = rd.getPlayer();

                            try {
                                Instant now = TimingSystem.getPlugin().currentTime;
                                var sectorTime = Duration.between(rd.getCurrentLap().getPassedCheckpointTime(rd.getLatestCheckpoint()), now).toMillis();
                                setItemName(player, 1, "§aSS " + ApiUtilities.formatAsTime(sectorTime) + " SS");
                            } catch (Exception e){}

                            try {
                                Instant now = TimingSystem.getPlugin().currentTime;
                                var lapTime = Duration.between(rd.getCurrentLap().getLapStart(), now).toMillis();
                                setItemName(player, 2, "§aLL " + ApiUtilities.formatAsTime(lapTime) + " LL");
                            } catch (Exception e){}

                            try {

                                StringBuilder lapstring = new StringBuilder();


                                lapstring.append("§5FLap: ");
                                if (rd.getBestLapTime() != -1){
                                    lapstring.append(ApiUtilities.formatAsTime(rd.getBestLapTime()));
                                } else {
                                    lapstring.append(ApiUtilities.formatAsTime(0));
                                }

                                lapstring.append(" §8| §eAvg:");
                                if (update) {
                                    lapstring.append(" ");
                                } else {
                                    lapstring.append("§o §r§e");
                                }
                                if (rd.getAverageLapTime() != -1) {
                                    lapstring.append(ApiUtilities.formatAsTime(rd.getAverageLapTime()));
                                } else {
                                    lapstring.append(ApiUtilities.formatAsTime(0));
                                }

                                lapstring.append(" §8| §cPrev: ");


                                if(rd.getPreviousLapTime() != -1) {
                                    lapstring.append(ApiUtilities.formatAsTime(rd.getPreviousLapTime()));
                                } else {
                                    lapstring.append(ApiUtilities.formatAsTime(0));
                                }

                                if (update) {
                                    lapstring.append("§l ");
                                } else {
                                    lapstring.append(" ");
                                }

                                update = !update;

                                setItemName(player, 3, lapstring.toString());
                            } catch (Exception e){}
                        }
                    }
                }
            }

        }, 5, 5);
    }

    private static void setItemName(Player player, int slot, String name){
        ItemStack item = player.getInventory().getItem(slot);
        if (item != null) {
            var meta = item.getItemMeta();
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        } else {
            ItemStack newItem;
            if (slot == 1) {
                newItem = new ItemBuilder(Material.DIAMOND_SWORD).setName(name).build();
            } else if (slot == 2) {
                newItem = new ItemBuilder(Material.COOKIE).setName(name).build();
            } else {
                newItem = new ItemBuilder(Material.CLOCK).setName(name).build();
            }
            player.getInventory().setItem(slot, newItem);
        }

    }

}
