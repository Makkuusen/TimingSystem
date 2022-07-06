package me.makkuusen.timing.system;

import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class RaceScoreboard {

    List<RaceDriver> livePositioning;
    Track track;

    public RaceScoreboard(List<RaceDriver> livePositioning, Track track) {
        this.livePositioning = livePositioning;
        this.track = track;
    }

    public Scoreboard getScoreboard()
    {
        SimpleScoreboard scoreboard = new SimpleScoreboard("§e§l" + getScoreboardName());

        int count = 0;
        int score = -1;
        RaceDriver previousDriver = null;
        for (RaceDriver rd : livePositioning){
            if (score == -9){
                break;
            }
            if (previousDriver != null) {
                if (rd.getLaps() < 1){
                    scoreboard.add("§f        §b| §f" + rd.getTSPlayer().getName(), score--);
                } else {
                    Instant timeStamp = rd.getTimeStamp(rd.getLaps(), rd.getLatestCheckpoint());
                    Instant previousDriverTimeStamp = previousDriver.getTimeStamp(rd.getLaps(), rd.getLatestCheckpoint());
                    long timeDiff = Duration.between(previousDriverTimeStamp, timeStamp).toMillis();
                    scoreboard.add("§f+" + ApiUtilities.formatAsTime(timeDiff) + " §b| §f" + rd.getTSPlayer().getName(), score--);
                }

            } else {
                scoreboard.add("§f        §4| §f" + rd.getTSPlayer().getName(), score--);
            }

            count++;
            previousDriver = rd;
        }
        scoreboard.build();

        return scoreboard.getScoreboard();
    }

    String getScoreboardName()
    {
        int spacesCount = ((20 - track.getName().length()) / 2) - 1;

        StringBuilder spaces = new StringBuilder();

        for (int i = 0; i < spacesCount; i++)
        {
            spaces.append(" ");
        }

        return spaces + track.getName() + spaces;
    }
}
