package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.SimpleScoreboard;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.FinalDriver;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;
import java.time.Instant;

public class FinalScoreboard extends GenericScoreboard {

    public FinalScoreboard(FinalHeat heat) {
        super(heat);
    }

    @Override
    public Scoreboard getRacingScoreboard(SimpleScoreboard scoreboard) {

        int score = -1;
        FinalDriver previousDriver = null;
        for (Driver driver : getHeat().getLivePositions()) {
            if (driver instanceof FinalDriver finalDriver) {
                if (score == -16) {
                    break;
                }
                if (previousDriver != null) {
                    if (driver.getLaps().size() < 1) {
                        scoreboard.add("§f          §8| §f" + driver.getTPlayer().getName() + " §r§8(§f" + finalDriver.getPits() + "§8)", score--);
                    } else {
                        long timeDiff = 0;
                        if (driver.isFinished()) {
                            timeDiff = Duration.between(previousDriver.getEndTime(), finalDriver.getEndTime()).toMillis();
                            scoreboard.add("§f +" + ApiUtilities.formatAsRacingGap(timeDiff) + " §8| §f" + driver.getTPlayer().getName() + " §r§8(§f" + finalDriver.getPits() + "§8)", score--);
                        } else {
                            Instant timeStamp = finalDriver.getTimeStamp(driver.getLaps().size(), driver.getCurrentLap().getLatestCheckpoint());
                            Instant previousDriverTimeStamp = previousDriver.getTimeStamp(driver.getLaps().size(), driver.getCurrentLap().getLatestCheckpoint());

                            // If leader has done too many laps and the comparision is weird.
                            if (previousDriverTimeStamp == null) {
                                scoreboard.add("§f          §8| §f" + driver.getTPlayer().getName() + " §r§8(§f" + finalDriver.getPits() + "§8)", score--);
                            } else {
                                timeDiff = Duration.between(previousDriverTimeStamp, timeStamp).toMillis();
                                if (timeDiff < 0) {
                                    scoreboard.add("§f +" + ApiUtilities.formatAsRacingGap(0) + " §8| §f" + driver.getTPlayer().getName() + " §r§8(§f" + finalDriver.getPits() + "§8)", score--);
                                } else {
                                    scoreboard.add("§f +" + ApiUtilities.formatAsRacingGap(timeDiff) + " §8| §f" + driver.getTPlayer().getName() + " §r§8(§f" + finalDriver.getPits() + "§8)", score--);
                                }
                            }
                        }
                    }

                } else {
                    scoreboard.add("§8 Lap: §f" + driver.getLaps().size() + " §8| §f" + driver.getTPlayer().getName() + " §8(§f" + finalDriver.getPits() + "§8)", score--);

                }
                previousDriver = finalDriver;
            }
        }
        scoreboard.build();
        return scoreboard.getScoreboard();
    }
}
