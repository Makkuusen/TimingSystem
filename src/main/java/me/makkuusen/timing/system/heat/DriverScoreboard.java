package me.makkuusen.timing.system.heat;

import dev.jcsoftware.jscoreboards.JPerPlayerMethodBasedScoreboard;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DriverScoreboard {
    JPerPlayerMethodBasedScoreboard jScoreboard;
    Driver driver;
    boolean compact;
    Heat heat;

    public DriverScoreboard(Player player, Driver driver){
        jScoreboard = new JPerPlayerMethodBasedScoreboard();
        this.driver = driver;
        heat = driver.getHeat();
        setTitle(player);
        jScoreboard.addPlayer(player);
    }

    public void setTitle(Player player){
        String eventName;
        if (heat.getEvent().getDisplayName().length() > 8) {
            eventName = heat.getEvent().getDisplayName().substring(0, 8);
        } else {
            eventName = heat.getEvent().getDisplayName();
        }
        jScoreboard.setTitle(player, "&7&l" + heat.getName() + " | " + eventName);
    }

    public void removeScoreboard(){
        jScoreboard.destroy();
    }

    public void setDriverLines(Player player){
            setLines(player);
    }

    public void setLines(Player player) {
        if (compact != driver.getTPlayer().getCompactScoreboard()) {
            compact = driver.getTPlayer().getCompactScoreboard();
            setTitle(player);
        }

        List<String> lines;
        int pos = driver.getPosition();
        if (pos > 12 && heat.getLivePositions().size() > 15){
            lines = individualScoreboard();
        } else {
            lines = normalScoreboard();
        }
        jScoreboard.setLines(player, lines);
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
            } else if (count > last) {
                break;
            }
            if (heat.getRound() instanceof QualificationRound) {
                lines.add(getDriverRowQualy(driver, this.driver));

            } else {
                lines.add(getDriverRowFinal(driver,this.driver));
            }
        }
        return lines;
    }
    public List<String> normalScoreboard(){
        List<String> lines = new ArrayList<>();
        int count = 0;
        int last = 15;
        for (Driver driver : heat.getLivePositions()) {
            count++;
            if (count > last) {
                break;
            }
            if (heat.getRound() instanceof QualificationRound) {
                lines.add(getDriverRowQualy(driver, this.driver));

            } else {
                lines.add(getDriverRowFinal(driver,this.driver));
            }
        }
        return lines;
    }

    private String getDriverRowFinal(Driver driver, Driver comparingDriver){
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

    private String getDriverRowQualy(Driver driver, Driver comparingDriver) {
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


