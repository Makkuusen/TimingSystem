package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.messages.ScoreBoard;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public class TimeTrialScoreboard {
    TPlayer tPlayer;
    TimeTrialSession timeTrialSession;

    public TimeTrialScoreboard(TPlayer tPlayer, TimeTrialSession timeTrialsession) {
        this.tPlayer = tPlayer;
        tPlayer.initScoreboard();
        this.timeTrialSession = timeTrialsession;
        tPlayer.setScoreBoardTitle(getColor(tPlayer.getTheme().getPrimary()) + "&l" + timeTrialsession.track.getDisplayName());
    }

    public void removeScoreboard() {
        tPlayer.clearScoreboard();
    }

    public void setDriverLines() {
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

        String color = getColor(tPlayer.getTheme().getError());
        if (percentage > 67) {
            color = getColor(tPlayer.getTheme().getSuccess());
        } else if (percentage > 33) {
            color = getColor(tPlayer.getTheme().getWarning());
        }

        lines.add(getColor(tPlayer.getTheme().getSecondary()) + PlainTextComponentSerializer.plainText().serialize(TimingSystem.getPlugin().getText(tPlayer, ScoreBoard.FINISHES)) + color + (totalAttempts != 0 ? totalFinishes + "/" + totalAttempts + " (" + percentage + "%)" : "(-)"));
        lines.add(getColor(tPlayer.getTheme().getSecondary()) + PlainTextComponentSerializer.plainText().serialize(TimingSystem.getPlugin().getText(tPlayer, ScoreBoard.AVERAGE_TIME)) + getColor(tPlayer.getTheme().getWarning()) + (averageTime != -1 ? ApiUtilities.formatAsTimeNoRounding(averageTime) : "(-)"));
        lines.add(getColor(tPlayer.getTheme().getSecondary()) + PlainTextComponentSerializer.plainText().serialize(TimingSystem.getPlugin().getText(tPlayer, ScoreBoard.BEST_TIME)) + getColor(tPlayer.getTheme().getSuccess()) + (bestTime != -1 ? ApiUtilities.formatAsTime(bestTime) : "(-)"));
        lines.add("");
        int count = timeTrialSession.getTimeTrialFinishes().size();

        int limit = Math.max(0, count - 10);
        if (count != 0) {
            for (int i = count; i > limit; i--) {
                long time = timeTrialSession.getTimeTrialFinishes().get(i - 1).getTime();
                if (time == bestTime) {
                    lines.add(getColor(tPlayer.getTheme().getPrimary()) + i + ". " + getColor(tPlayer.getTheme().getSuccess()) + ApiUtilities.formatAsTime(time));
                } else if (time == slowestTime) {
                    lines.add(getColor(tPlayer.getTheme().getPrimary()) + i + ". " + getColor(tPlayer.getTheme().getError()) + ApiUtilities.formatAsTime(time));
                } else {
                    lines.add(getColor(tPlayer.getTheme().getPrimary()) + i + ". " + getColor(tPlayer.getTheme().getSecondary()) + ApiUtilities.formatAsTime(time));
                }
            }
        }
        tPlayer.setScoreBoardLines(lines);
    }

    private static String getColor(TextColor color) {
        return String.valueOf(net.md_5.bungee.api.ChatColor.of(color.asHexString()));
    }
}
