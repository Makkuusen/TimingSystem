package me.makkuusen.timing.system;

import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.participant.DriverState;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.timetrial.TimeTrial;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;

public class PlayerTimer {
    static boolean update = false;


    public static void initPlayerTimer() {

        Bukkit.getScheduler().scheduleSyncRepeatingTask(TimingSystem.getPlugin(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (TimeTrialController.timeTrials.containsKey(p.getUniqueId())) {
                    TimeTrial timeTrial = TimeTrialController.timeTrials.get(p.getUniqueId());
                    long mapTime = timeTrial.getCurrentTime();
                    if (timeTrial.getBestFinish() == -1) {
                        ApiUtilities.sendActionBar("§a" + ApiUtilities.formatAsTime(mapTime), p);
                    } else if (mapTime < timeTrial.getBestFinish()) {
                        ApiUtilities.sendActionBar("§a" + ApiUtilities.formatAsTime(mapTime), p);
                    } else {
                        ApiUtilities.sendActionBar("§c" + ApiUtilities.formatAsTime(mapTime), p);
                    }
                } else {
                    var maybeDriver = EventDatabase.getDriverFromRunningHeat(p.getUniqueId());
                    if (maybeDriver.isPresent()) {
                        var driver = maybeDriver.get();
                        if (driver.getHeat().getRound() instanceof FinalRound) {
                            if (!driver.isFinished()) {
                                String message = TimingSystem.getPlugin().getLocalizedMessage(
                                        p,
                                        "messages.actionbar.race",
                                        "%laps%", String.valueOf(driver.getLaps().size()),
                                        "%totalLaps%", String.valueOf(driver.getHeat().getTotalLaps()),
                                        "%pos%", String.valueOf(driver.getPosition()),
                                        "%pits%", String.valueOf(driver.getPits()),
                                        "%totalPits%", String.valueOf(driver.getHeat().getTotalPits())
                                );
                                ApiUtilities.sendActionBar(message, p);
                            }
                        } else if (driver.getHeat().getRound() instanceof QualificationRound) {
                            if (driver.getLaps().size() > 0 && driver.getState() == DriverState.RUNNING) {
                                long lapTime = Duration.between(driver.getCurrentLap().getLapStart(), TimingSystem.currentTime).toMillis();
                                long timeLeft = driver.getHeat().getTimeLimit() - Duration.between(driver.getStartTime(), TimingSystem.currentTime).toMillis();
                                if (timeLeft < 0) {
                                    ApiUtilities.sendActionBar("§a" + ApiUtilities.formatAsTime(lapTime) + "§r§8 |§f§l P" + driver.getPosition() + "§r§8 |§f§l §c-" +  ApiUtilities.formatAsHeatTimeCountDown(timeLeft*-1), p);
                                } else {
                                    ApiUtilities.sendActionBar("§a" + ApiUtilities.formatAsTime(lapTime) + "§r§8 |§f§l P" + driver.getPosition() + "§r§8 |§f§l §e" + ApiUtilities.formatAsHeatTimeCountDown(timeLeft), p);
                                }
                            } else if (driver.getState() == DriverState.LOADED || driver.getState() == DriverState.STARTING){
                                long timeLeft = driver.getHeat().getTimeLimit();
                                if (driver.getStartTime() != null) {
                                    timeLeft = driver.getHeat().getTimeLimit() - Duration.between(driver.getStartTime(), TimingSystem.currentTime).toMillis();
                                }
                                ApiUtilities.sendActionBar("§a00.000§r§8 |§f§l P" + driver.getPosition() + "§r§8 |§f§l §e" + ApiUtilities.formatAsHeatTimeCountDown(timeLeft), p);
                            }
                        }
                    } else {
                        var mightBeDriver = EventDatabase.getClosestDriverForSpectator(p);
                        if (mightBeDriver.isPresent()) {

                            var driver = mightBeDriver.get();
                            if (driver.getHeat().getRound() instanceof FinalRound) {
                                    if (!driver.isFinished()) {
                                        String message = TimingSystem.getPlugin().getLocalizedMessage(
                                                p,
                                                "messages.actionbar.raceSpectator",
                                                "%name%", driver.getTPlayer().getName(),
                                                "%laps%", String.valueOf(driver.getLaps().size()),
                                                "%totalLaps%", String.valueOf(driver.getHeat().getTotalLaps()),
                                                "%pos%", String.valueOf(driver.getPosition()),
                                                "%pits%", String.valueOf(driver.getPits()),
                                                "%totalPits%", String.valueOf(driver.getHeat().getTotalPits())
                                        );
                                        ApiUtilities.sendActionBar(message, p);

                                }
                            } else if (driver.getHeat().getRound() instanceof QualificationRound) {
                                if (driver.getLaps().size() > 0 && driver.getState() == DriverState.RUNNING) {
                                    long lapTime = Duration.between(driver.getCurrentLap().getLapStart(), TimingSystem.currentTime).toMillis();
                                    long timeLeft = driver.getHeat().getTimeLimit() - Duration.between(driver.getStartTime(), TimingSystem.currentTime).toMillis();
                                    if (timeLeft < 0) {
                                        ApiUtilities.sendActionBar("§f" + driver.getTPlayer().getName() + " > §a" + ApiUtilities.formatAsTime(lapTime) + "§r§8 |§f§l P" + driver.getPosition() + "§r§8 |§f§l §c-" +  ApiUtilities.formatAsHeatTimeCountDown(timeLeft*-1), p);
                                    } else {
                                        ApiUtilities.sendActionBar("§f" + driver.getTPlayer().getName() + " > §a" + ApiUtilities.formatAsTime(lapTime) + "§r§8 |§f§l P" + driver.getPosition() + "§r§8 |§f§l §e" + ApiUtilities.formatAsHeatTimeCountDown(timeLeft), p);
                                    }
                                } else if (driver.getState() == DriverState.LOADED || driver.getState() == DriverState.STARTING){
                                    long timeLeft = driver.getHeat().getTimeLimit();
                                    if (driver.getStartTime() != null) {
                                        timeLeft = driver.getHeat().getTimeLimit() - Duration.between(driver.getStartTime(), TimingSystem.currentTime).toMillis();
                                    }
                                    ApiUtilities.sendActionBar("§f" + driver.getTPlayer().getName() + " > §a00.000§r§8 |§f§l P" + driver.getPosition() + "§r§8 |§f§l §e" + ApiUtilities.formatAsHeatTimeCountDown(timeLeft), p);
                                }
                            }
                        }
                    }
                }
            }

        }, 5, 5);
    }

}
