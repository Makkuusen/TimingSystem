package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.SimpleScoreboard;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.scoreboard.Scoreboard;

public class QualyScoreboard extends GenericScoreboard {


    public QualyScoreboard(QualyHeat heat) {
        super(heat);
    }

    @Override
    Scoreboard getRacingScoreboard(SimpleScoreboard scoreboard){
        int score = -1;
        Driver previousDriver = null;
        for (Driver driver : getHeat().getLivePositions()) {
            if (score == -9) {
                break;
            }
            if (previousDriver != null) {
                if (driver.getLaps().size() < 1) {
                    scoreboard.add("§f          §8| §f" + driver.getTPlayer().getName(), score--);
                } else {
                    long timeDiff = 0;
                    if (driver.getBestLap().isPresent() && previousDriver.getBestLap().isPresent()) {
                        timeDiff = driver.getBestLap().get().getLapTime() - previousDriver.getBestLap().get().getLapTime();
                        scoreboard.add("§f +" + ApiUtilities.formatAsQualyGap(timeDiff) + " §8| §f" + driver.getTPlayer().getName(), score--);
                    } else {
                        scoreboard.add("§f          §8| §f" + driver.getTPlayer().getName(), score--);
                    }
                }
            } else {
                if (driver.getBestLap().isPresent()) {
                    scoreboard.add("§f " + ApiUtilities.formatAsTime(driver.getBestLap().get().getLapTime()) + " §8| §f" + driver.getTPlayer().getName(), score--);
                } else {
                    scoreboard.add("§f          §8| §f" + driver.getTPlayer().getName(), score--);
                }

            }
            previousDriver = driver;

        }
        scoreboard.build();
        return scoreboard.getScoreboard();
    }
}
