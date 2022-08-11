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
    Heat heat;

    public DriverScoreboard(Player player, Driver driver){
        jScoreboard = new JPerPlayerMethodBasedScoreboard();
        this.driver = driver;
        heat = driver.getHeat();
        jScoreboard.setTitle(player, "&7&l" + heat.getName() + " | " + heat.getEvent().getDisplayName());
        jScoreboard.addPlayer(player);
    }

    public void removeScoreboard(){
        jScoreboard.destroy();
    }

    public void setDriverLines(Player player){
            setLines(player);
    }

    public void setLines(Player player) {
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
            return ScoreboardUtils.getDriverLineRace(driver.getTPlayer().getName(), driver.getPosition());
        }
        long timeDiff;

        if (driver.getTPlayer().getPlayer() == null) {
            return ScoreboardUtils.getDriverLineRaceOffline(driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
        }
        Location playerLoc = driver.getTPlayer().getPlayer().getLocation();

        var inPitRegions = heat.getEvent().getTrack().getRegions(TrackRegion.RegionType.INPIT);
        for (TrackRegion trackRegion : inPitRegions) {
            if (trackRegion.contains(playerLoc)){
                return ScoreboardUtils.getDriverLineRaceInPit(driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
            }
        }

        if (driver.getPosition() < comparingDriver.getPosition()) {

            if (comparingDriver.isFinished()) {
                timeDiff = Duration.between(driver.getEndTime(), comparingDriver.getEndTime()).toMillis();
                return ScoreboardUtils.getDriverLineNegativeRaceGap(timeDiff, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
            }

            if (comparingDriver.getLaps().size() > 0 && comparingDriver.getCurrentLap() != null) {
                Instant timeStamp = comparingDriver.getTimeStamp(comparingDriver.getLaps().size(), comparingDriver.getCurrentLap().getLatestCheckpoint());
                Instant fasterTimeStamp = driver.getTimeStamp(comparingDriver.getLaps().size(), comparingDriver.getCurrentLap().getLatestCheckpoint());
                timeDiff = Duration.between(fasterTimeStamp, timeStamp).toMillis();
                if (timeDiff < 0) {
                    return ScoreboardUtils.getDriverLineRaceGap(timeDiff*-1, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
                }
                return ScoreboardUtils.getDriverLineNegativeRaceGap(timeDiff, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
            }
            return ScoreboardUtils.getDriverLineRace(driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
        }

        if (driver.getPosition() > comparingDriver.getPosition()) {
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

        return ScoreboardUtils.getDriverLineRace(driver.getTPlayer().getName(), driver.getPits(), driver.getPosition());
    }

    private String getDriverRowQualy(Driver driver, Driver comparingDriver) {
        if (driver.getBestLap().isEmpty()) {
            return ScoreboardUtils.getDriverLine(driver.getTPlayer().getName(), driver.getPosition());
        }

        if (comparingDriver.getBestLap().isEmpty()) {
            return ScoreboardUtils.getDriverLineQualyTime(driver.getBestLap().get().getLapTime(), driver.getTPlayer().getName(), driver.getPosition());
        }

        if (comparingDriver.equals(driver)) {
            return ScoreboardUtils.getDriverLineQualyTime(driver.getBestLap().get().getLapTime(), driver.getTPlayer().getName(), driver.getPosition());
        }

        long timeDiff = driver.getBestLap().get().getLapTime() - comparingDriver.getBestLap().get().getLapTime();
        if (timeDiff < 0) {
            return ScoreboardUtils.getDriverLineNegativeQualyGap(timeDiff*-1, driver.getTPlayer().getName(), driver.getPosition());
        }
        return ScoreboardUtils.getDriverLineQualyGap(timeDiff, driver.getTPlayer().getName(), driver.getPosition());
    }
}


