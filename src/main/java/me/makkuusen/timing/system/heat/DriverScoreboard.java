package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.round.QualificationRound;
import org.bukkit.Location;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DriverScoreboard {
    TPlayer tPlayer;
    Driver driver;
    Heat heat;

    public DriverScoreboard(TPlayer tPlayer, Driver driver){
        this.tPlayer = tPlayer;
        this.driver = driver;
        heat = driver.getHeat();
        setTitle();
    }

    public void setTitle(){
        String eventName;
        if (tPlayer.getCompactScoreboard() && heat.getEvent().getDisplayName().length() > 8) {
            eventName = heat.getEvent().getDisplayName().substring(0, 8);
        } else {
            eventName = heat.getEvent().getDisplayName();
        }

        tPlayer.setScoreBoardTitle("&7&l" + heat.getName() + " | " + eventName);
    }

    public void removeScoreboard(){
        tPlayer.clearScoreboard();
    }

    public void setDriverLines(){
        setTitle();
        setLines();
    }

    public void setLines() {
        List<String> lines;
        int pos = driver.getPosition();
        if (pos > 12 && heat.getLivePositions().size() > 15){
            lines = individualScoreboard();
        } else {
            lines = normalScoreboard();
        }
        tPlayer.setScoreBoardLines(lines);
    }

    public List<String> individualScoreboard(){
        List<String> lines = new ArrayList<>();
        int count = 0;
        int last = Math.min(driver.getPosition() + 7, heat.getDrivers().size());
        int first = last - 14;
        for (Driver driver : heat.getLivePositions()) {
            count++;
            if (count < first){
                continue;
            }
            if (heat.getRound() instanceof QualificationRound) {
                lines.add(getDriverRowQualification(driver, this.driver, tPlayer.getCompactScoreboard()));

            } else {
                lines.add(getDriverRowFinal(driver,this.driver, tPlayer.getCompactScoreboard()));
            }
        }
        return lines;
    }
    public List<String> normalScoreboard(){
        List<String> lines = new ArrayList<>();
        for (Driver driver : heat.getLivePositions()) {
            if (heat.getRound() instanceof QualificationRound) {
                lines.add(getDriverRowQualification(driver, this.driver, tPlayer.getCompactScoreboard()));

            } else {
                lines.add(getDriverRowFinal(driver,this.driver, tPlayer.getCompactScoreboard()));
            }
        }
        return lines;
    }

    private String getDriverRowFinal(Driver driver, Driver comparingDriver, boolean compact){
        if (driver.getLaps().size() < 1) {
            return ScoreboardUtils.getDriverLineRace(driver.getTPlayer().getName(), driver.getPosition(), compact);
        }
        long timeDiff;

        if (driver.getTPlayer().getPlayer() == null) {
            return ScoreboardUtils.getDriverLineRaceOffline(driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), compact);
        }
        Location playerLoc = driver.getTPlayer().getPlayer().getLocation();

        if (driver.isInPit(playerLoc)) {
            return ScoreboardUtils.getDriverLineRaceInPit(driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), compact);
        }

        if (driver.getPosition() < comparingDriver.getPosition()) {

            if (comparingDriver.isFinished()) {
                timeDiff = Duration.between(driver.getEndTime(), comparingDriver.getEndTime()).toMillis();
                return ScoreboardUtils.getDriverLineNegativeRaceGap(timeDiff, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), compact);
            }

            if (comparingDriver.getLaps().size() > 0 && comparingDriver.getCurrentLap() != null) {
                Instant timeStamp = comparingDriver.getTimeStamp(comparingDriver.getLaps().size(), comparingDriver.getCurrentLap().getLatestCheckpoint());
                Instant fasterTimeStamp = driver.getTimeStamp(comparingDriver.getLaps().size(), comparingDriver.getCurrentLap().getLatestCheckpoint());
                timeDiff = Duration.between(fasterTimeStamp, timeStamp).toMillis();
                if (timeDiff < 0) {
                    return ScoreboardUtils.getDriverLineRaceGap(timeDiff*-1, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), compact);
                }
                return ScoreboardUtils.getDriverLineNegativeRaceGap(timeDiff, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), compact);
            }
            return ScoreboardUtils.getDriverLineRace(driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), compact);
        }

        if (driver.getPosition() > comparingDriver.getPosition()) {
            if (driver.isFinished()) {
                timeDiff = Duration.between(comparingDriver.getEndTime(), driver.getEndTime()).toMillis();
                return ScoreboardUtils.getDriverLineRaceGap(timeDiff, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), compact);
            }

            Instant timeStamp = driver.getTimeStamp(driver.getLaps().size(), driver.getCurrentLap().getLatestCheckpoint());
            Instant fasterTimeStamp = comparingDriver.getTimeStamp(driver.getLaps().size(), driver.getCurrentLap().getLatestCheckpoint());
            timeDiff = Duration.between(fasterTimeStamp, timeStamp).toMillis();
            if (timeDiff < 0) {
                return ScoreboardUtils.getDriverLineNegativeRaceGap(timeDiff*-1, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), compact);
            }
            return ScoreboardUtils.getDriverLineRaceGap(timeDiff, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), compact);
        }

        return ScoreboardUtils.getDriverLineRace(driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), compact);
    }

    private String getDriverRowQualification(Driver driver, Driver comparingDriver, boolean compact) {
        if (driver.getBestLap().isEmpty()) {
            return ScoreboardUtils.getDriverLine(driver.getTPlayer().getName(), driver.getPosition(), compact);
        }

        if (comparingDriver.getBestLap().isEmpty()) {
            return ScoreboardUtils.getDriverLineQualyTime(driver.getBestLap().get().getLapTime(), driver.getTPlayer().getName(), driver.getPosition(), compact);
        }

        if (comparingDriver.equals(driver)) {
            return ScoreboardUtils.getDriverLineQualyTime(driver.getBestLap().get().getLapTime(), driver.getTPlayer().getName(), driver.getPosition(), compact);
        }

        long timeDiff = driver.getBestLap().get().getLapTime() - comparingDriver.getBestLap().get().getLapTime();
        if (timeDiff < 0) {
            return ScoreboardUtils.getDriverLineNegativeQualyGap(timeDiff*-1, driver.getTPlayer().getName(), driver.getPosition(), compact);
        }
        return ScoreboardUtils.getDriverLineQualyGap(timeDiff, driver.getTPlayer().getName(), driver.getPosition(), compact);
    }
}


