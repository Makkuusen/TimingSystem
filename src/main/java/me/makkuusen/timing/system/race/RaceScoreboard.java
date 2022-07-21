package me.makkuusen.timing.system.race;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.SimpleScoreboard;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;
import java.time.Instant;

public class RaceScoreboard {

    Race race;

    public RaceScoreboard(Race race) {
        this.race = race;
    }

    public Scoreboard getScoreboard() {
        SimpleScoreboard scoreboard = new SimpleScoreboard(getScoreboardName());

        int count = 0;
        int score = -1;
        RaceDriver previousDriver = null;
        for (RaceDriver rd : race.livePositioning) {
            if (score == -9) {
                break;
            }
            if (previousDriver != null) {
                if (rd.getLaps() < 1) {
                    scoreboard.add("§f          §8| §f" + rd.getTSPlayer().getName() + " §r§8(§f" + rd.getPits() + "§8)", score--);
                } else {
                    Instant timeStamp = rd.getTimeStamp(rd.getLaps(), rd.getLatestCheckpoint());
                    Instant previousDriverTimeStamp = previousDriver.getTimeStamp(rd.getLaps(), rd.getLatestCheckpoint());
                    // If leader has done too many laps and the comparision is weird.
                    if (previousDriverTimeStamp == null) {
                        scoreboard.add("§f          §8| §f" + rd.getTSPlayer().getName() + " §r§8(§f" + rd.getPits() + "§8)", score--);
                    }
                    long timeDiff = Duration.between(previousDriverTimeStamp, timeStamp).toMillis();
                    if (timeDiff < 0) {
                        scoreboard.add("§f +" + ApiUtilities.formatAsRacingGap(0) + " §8| §f" + rd.getTSPlayer().getName() + " §r§8(§f" + rd.getPits() + "§8)", score--);
                    } else {
                        scoreboard.add("§f +" + ApiUtilities.formatAsRacingGap(timeDiff) + " §8| §f" + rd.getTSPlayer().getName() + " §r§8(§f" + rd.getPits() + "§8)", score--);
                    }
                }

            } else {
                scoreboard.add("§8 Lap: §f" + rd.getLaps() + " §8| §f" + rd.getTSPlayer().getName() + " §8(§f" + rd.getPits() + "§8)", score--);

            }
            count++;
            previousDriver = rd;
        }
        scoreboard.build();

        return scoreboard.getScoreboard();
    }

    String getScoreboardName() {
        int spacesCount = ((20 - race.getTrack().getName().length()) / 2) - 1;

        StringBuilder spaces = new StringBuilder();

        for (int i = 0; i < spacesCount; i++) {
            spaces.append(" ");
        }

        return spaces + "§7§l" + race.getTrack().getName() + spaces;
    }
}
