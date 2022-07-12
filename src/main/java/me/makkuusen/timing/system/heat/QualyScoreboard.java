package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.SimpleScoreboard;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.scoreboard.Scoreboard;

public class QualyScoreboard {

    QualyHeat qualyHeat;

    public QualyScoreboard(QualyHeat qualyHeat) {
        this.qualyHeat = qualyHeat;
    }

    public Scoreboard getScoreboard()
    {
        SimpleScoreboard scoreboard = new SimpleScoreboard(getScoreboardName());

        int count = 0;
        int score = -1;
        Driver previousDriver = null;
        for (Driver driver : qualyHeat.getPositions()){
            if (score == -9) {
                break;
            }
            if (previousDriver != null) {
                if (driver.getLaps().size() < 1) {
                    scoreboard.add("§f          §8| §f" + driver.getTPlayer().getName(), score--);
                } else {
                    long timeDiff = 0;
                    if (driver.getBestLap().isPresent() && previousDriver.getBestLap().isPresent()){
                        timeDiff = driver.getBestLap().get().getLapTime() - previousDriver.getBestLap().get().getLapTime() ;
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
            count++;
            previousDriver = driver;
        }
        scoreboard.build();

        return scoreboard.getScoreboard();
    }

    String getScoreboardName()
    {
        int spacesCount = ((20 - qualyHeat.getName().length()) / 2) - 1;

        StringBuilder spaces = new StringBuilder();

        for (int i = 0; i < spacesCount; i++)
        {
            spaces.append(" ");
        }

        return "§7§l" + qualyHeat.getName() + " - " + qualyHeat.getEvent().getDisplayName();
    }
}
