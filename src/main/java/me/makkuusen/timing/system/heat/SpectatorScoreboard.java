package me.makkuusen.timing.system.heat;

import dev.jcsoftware.jscoreboards.JGlobalMethodBasedScoreboard;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.FinalDriver;
import me.makkuusen.timing.system.participant.QualyDriver;
import me.makkuusen.timing.system.participant.Spectator;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SpectatorScoreboard {

    private Set<UUID> spectators = new HashSet<>();
    private JGlobalMethodBasedScoreboard jScoreboard;
    private Heat heat;

    public SpectatorScoreboard(Heat heat){
        jScoreboard = new JGlobalMethodBasedScoreboard();
        this.heat = heat;
        jScoreboard.setTitle("&7&l" + heat.getName() + " - " + heat.getEvent().getDisplayName());
    }

    public void removeScoreboard(){
        jScoreboard.destroy();
    }

    public void updateScoreboard() {
        for (Spectator spec : heat.getEvent().getSpectators().values()) {
            if (!spectators.contains(spec.getTPlayer().getUniqueId()) && !heat.getDrivers().containsKey(spec.getTPlayer().getUniqueId())) {
                if (spec.getTPlayer().getPlayer() != null) {
                    jScoreboard.addPlayer(spec.getTPlayer().getPlayer());
                    spectators.add(spec.getTPlayer().getUniqueId());
                }
            } else if (spectators.contains(spec.getTPlayer().getUniqueId()) && spec.getTPlayer().getPlayer() == null) {
                spectators.remove(spec.getTPlayer().getUniqueId());
            }
        }
        List<String> lines;
        lines = normalScoreboard();
        jScoreboard.setLines(lines);
    }

    public List<String> normalScoreboard(){
        List<String> lines = new ArrayList<>();
        int count = 0;
        int last = 15;
        Driver prevDriver = null;
        for (Driver driver : heat.getLivePositions()) {
            count++;
            if (count > last) {
                break;
            }
            if (heat instanceof QualifyHeat) {
                lines.add(getDriverRow((QualyDriver) driver, (QualyDriver) prevDriver));
                prevDriver = driver;

            } else if (heat instanceof FinalHeat) {
                lines.add(getDriverRow((FinalDriver) driver, (FinalDriver) prevDriver));
                prevDriver = driver;
            }
        }
        return lines;
    }

    private String getDriverRow(FinalDriver driver, FinalDriver comparingDriver){
        if (driver.getLaps().size() < 1) {
            return ScoreboardUtils.getDriverLineRace(driver.getTPlayer().getName(), driver.getPosition());
        }
        long timeDiff;

        if (comparingDriver == null) {
            return ScoreboardUtils.getDriverLineRaceLaps(driver.getLaps().size(), driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
        }
        if (driver.isFinished()) {
            timeDiff = Duration.between(comparingDriver.getEndTime(), driver.getEndTime()).toMillis();
            return ScoreboardUtils.getDriverLineRaceGap(timeDiff, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
        }

        Instant timeStamp = driver.getTimeStamp(driver.getLaps().size(), driver.getCurrentLap().getLatestCheckpoint());
        Instant fasterTimeStamp = comparingDriver.getTimeStamp(driver.getLaps().size(), driver.getCurrentLap().getLatestCheckpoint());
        timeDiff = Duration.between(fasterTimeStamp, timeStamp).toMillis();
        if (timeDiff < 0) {
            return ScoreboardUtils.getDriverLineNegativeRaceGap(timeDiff*-1, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
        }
        return ScoreboardUtils.getDriverLineRaceGap(timeDiff, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
    }

    private String getDriverRow(QualyDriver driver, QualyDriver comparingDriver) {
        if (driver.getBestLap().isEmpty()) {
            return ScoreboardUtils.getDriverLine(driver.getTPlayer().getName(), driver.getPosition());
        }

        if (comparingDriver == null) {
            return ScoreboardUtils.getDriverLineQualyTime(driver.getBestLap().get().getLapTime(), driver.getTPlayer().getName(), driver.getPosition());
        }

        long timeDiff = driver.getBestLap().get().getLapTime() - comparingDriver.getBestLap().get().getLapTime();
        if (timeDiff < 0) {
            return ScoreboardUtils.getDriverLineNegativeQualyGap(timeDiff*-1, driver.getTPlayer().getName(), driver.getPosition());
        }
        return ScoreboardUtils.getDriverLineQualyGap(timeDiff, driver.getTPlayer().getName(), driver.getPosition());
    }
}
