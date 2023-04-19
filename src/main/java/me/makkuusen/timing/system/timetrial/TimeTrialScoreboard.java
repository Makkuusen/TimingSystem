package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;

import java.util.ArrayList;
import java.util.List;

public class TimeTrialScoreboard {
    TPlayer tPlayer;
    TimeTrialSession timeTrialSession;

    public TimeTrialScoreboard(TPlayer tPlayer, TimeTrialSession timeTrialsession){
        this.tPlayer = tPlayer;
        tPlayer.initScoreboard();
        this.timeTrialSession = timeTrialsession;
        tPlayer.setScoreBoardTitle("&7&l" + timeTrialsession.track.getDisplayName());
    }

    public void removeScoreboard(){
        tPlayer.clearScoreboard();
    }

    public void setDriverLines(){
        setLines();
    }

    public void setLines() {
        List<String> lines = new ArrayList<>();

        long totalTime = 0;
        long totalFinishes = timeTrialSession.getTimeTrialFinishes().size();
        long totalAttempts = totalFinishes + timeTrialSession.getTimeTrialAttempts().size();
        long bestTime = -1;
        long slowestTime = -1;
        for (TimeTrialFinish tt : timeTrialSession.getTimeTrialFinishes()) {
            if (bestTime == -1) {
                bestTime = tt.getTime();
            }

            if (slowestTime == -1) {
                slowestTime = tt.getTime();
            }

            if (tt.getTime() < bestTime) {
                bestTime = tt.getTime();
            }

            if (tt.getTime() > slowestTime) {
                slowestTime = tt.getTime();
            }

            totalTime += tt.getTime();
        }

        long averageTime = -1;
        long percentage = 0;
        if (totalAttempts != 0) {
             percentage = totalFinishes * 100 / totalAttempts;
        }

        if (totalFinishes != 0) {
            averageTime = totalTime / totalFinishes;
        }

        String color = "§c";
        if (percentage > 67) {
            color = "§a";
        } else if (percentage > 33) {
            color = "§e";
        }

        lines.add("§7Finishes: " + color + (totalAttempts != 0 ? totalFinishes + "/" + totalAttempts + " (" + percentage + "%)" : "(none)"));
        lines.add("§7Avg time: §f" + (averageTime != -1 ? ApiUtilities.formatAsTimeNoRounding(averageTime) : "(none)"));
        lines.add("§7Best time: §a" + (bestTime != -1 ? ApiUtilities.formatAsTime(bestTime): "(none)"));
        lines.add("");
        int count = timeTrialSession.getTimeTrialFinishes().size();

        int limit = Math.max(0, count - 10);
        if (count != 0) {
            for (int i = count; i > limit; i--) {
                long time = timeTrialSession.getTimeTrialFinishes().get(i - 1).getTime();
                if (time == bestTime) {
                    lines.add("§7" + i + ". §a" + ApiUtilities.formatAsTime(time));
                } else if (time == slowestTime) {
                    lines.add("§7" + i + ". §c" + ApiUtilities.formatAsTime(time));
                } else {
                    lines.add("§7" + i + ". §f" + ApiUtilities.formatAsTime(time));
                }
            }
        }
        tPlayer.setScoreBoardLines(lines);
    }
}
