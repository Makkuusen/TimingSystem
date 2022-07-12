package me.makkuusen.timing.system.heat;

import lombok.Getter;
import me.makkuusen.timing.system.SimpleScoreboard;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.scoreboard.Scoreboard;

@Getter
public abstract class GenericScoreboard {
    private Heat heat;

    public GenericScoreboard(Heat heat) {
        this.heat = heat;
    }

    public Scoreboard getScoreboard()
    {

        if (getHeat().getHeatState() == HeatState.LOADED){
            return getStartingScoreboard(new SimpleScoreboard(getScoreboardName()));
        }
            return getRacingScoreboard(new SimpleScoreboard(getScoreboardName()));
    }

    abstract Scoreboard getRacingScoreboard(SimpleScoreboard scoreboard);

    Scoreboard getStartingScoreboard(SimpleScoreboard scoreboard){
        int score = -1;
        for (Driver driver : heat.getStartPositions()) {
            if (score == -9) {
                break;
            }
            scoreboard.add("§f          §8| §f" + driver.getTPlayer().getName(), score--);
        }
        scoreboard.build();
        return scoreboard.getScoreboard();

    }

    String getScoreboardName()
    {
        if (heat.getHeatState() == HeatState.RACING) {
            return "§7§l" + heat.getName() + " - " + heat.getEvent().getDisplayName();
        } else {
            return "§7§l(" + heat.getName() + ") - " + heat.getEvent().getDisplayName();
        }
    }
}
