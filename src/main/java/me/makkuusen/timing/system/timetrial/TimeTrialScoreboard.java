package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.ScoreBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
        tPlayer.setScoreBoardTitle(Component.text(timeTrialsession.track.getDisplayName()).color(tPlayer.getTheme().getPrimary()).decorate(TextDecoration.BOLD));
    }

    public void removeScoreboard() {
        tPlayer.clearScoreboard();
    }

    public void setDriverLines() {
        setLines();
    }

    public void setLines() {
        List<Component> lines = new ArrayList<>();

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

        TextColor color = tPlayer.getTheme().getError();
        if (percentage > 67) {
            color = tPlayer.getTheme().getSuccess();
        } else if (percentage > 33) {
            color = tPlayer.getTheme().getWarning();
        }

        lines.add(Component.text(PlainTextComponentSerializer.plainText().serialize(Text.get(tPlayer, ScoreBoard.FINISHES))).color(tPlayer.getTheme().getSecondary()).append(Component.text((totalAttempts != 0 ? totalFinishes + "/" + totalAttempts + " (" + percentage + "%)" : "(-)"))).color(color));
        lines.add(Component.text(PlainTextComponentSerializer.plainText().serialize(Text.get(tPlayer, ScoreBoard.AVERAGE_TIME))).color(tPlayer.getTheme().getSecondary()).append(Component.text((averageTime != -1 ? ApiUtilities.formatAsTimeNoRounding(averageTime) : "(-)"))).color(tPlayer.getTheme().getWarning()));
        lines.add(Component.text(PlainTextComponentSerializer.plainText().serialize(Text.get(tPlayer, ScoreBoard.BEST_TIME))).color(tPlayer.getTheme().getSecondary()).append(Component.text((bestTime != -1 ? ApiUtilities.formatAsTime(bestTime) : "(-)"))).color(tPlayer.getTheme().getSuccess()));
        lines.add(Component.empty());
        int count = timeTrialSession.getTimeTrialFinishes().size();

        int limit = Math.max(0, count - 10);
        if (count != 0) {
            for (int i = count; i > limit; i--) {
                long time = timeTrialSession.getTimeTrialFinishes().get(i - 1).getTime();
                if (time == bestTime) {
                    lines.add(Component.text(i + ". ").color(tPlayer.getTheme().getPrimary()).append(Component.text(ApiUtilities.formatAsTime(time)).color(tPlayer.getTheme().getSuccess())));
                } else if (time == slowestTime) {
                    lines.add(Component.text(i + ". ").color(tPlayer.getTheme().getPrimary()).append(Component.text(ApiUtilities.formatAsTime(time)).color(tPlayer.getTheme().getError())));
                } else {
                    lines.add(Component.text(i + ". ").color(tPlayer.getTheme().getPrimary()).append(Component.text(ApiUtilities.formatAsTime(time)).color(tPlayer.getTheme().getSecondary())));
                }
            }
        }
        tPlayer.setScoreBoardLines(lines);
    }
}
