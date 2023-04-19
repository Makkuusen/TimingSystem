package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Spectator;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Location;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SpectatorScoreboard {

    private Set<UUID> spectators = new HashSet<>();
    private Heat heat;

    public SpectatorScoreboard(Heat heat){
        this.heat = heat;
    }

    public void updateScoreboard() {
        for (Spectator spec : heat.getEvent().getSpectators().values()) {
            if (!spectators.contains(spec.getTPlayer().getUniqueId()) && !heat.getDrivers().containsKey(spec.getTPlayer().getUniqueId())) {
                if (spec.getTPlayer().getPlayer() != null) {
                    spec.getTPlayer().initScoreboard();
                    List<String> lines;
                    lines = normalScoreboard();
                    setTitle(spec.getTPlayer());
                    spec.getTPlayer().setScoreBoardLines(lines);
                    spectators.add(spec.getTPlayer().getUniqueId());
                }
            } else if (spectators.contains(spec.getTPlayer().getUniqueId()) && spec.getTPlayer().getPlayer() == null) {
                spectators.remove(spec.getTPlayer().getUniqueId());
                spec.getTPlayer().clearScoreboard();
            }
        }
    }

    public void setTitle(TPlayer tPlayer){
        String eventName;
        if (heat.getEvent().getDisplayName().length() > 8) {
            eventName = heat.getEvent().getDisplayName().substring(0, 8);
        } else {
            eventName = heat.getEvent().getDisplayName();
        }

        tPlayer.setScoreBoardTitle("&7&l" + heat.getName() + " | " + eventName);
    }

    public void removeScoreboards() {
        for (Spectator spec : heat.getEvent().getSpectators().values()) {
            spec.getTPlayer().clearScoreboard();
        }
    }

    public List<String> normalScoreboard(){
        List<String> lines = new ArrayList<>();
        Driver prevDriver = null;
        boolean compareToFirst = true;
        for (Driver driver : heat.getLivePositions()) {
            if (heat.getRound() instanceof QualificationRound) {
                lines.add(getDriverRowQualy(driver, prevDriver));
                if (compareToFirst) {
                    prevDriver = driver;
                    compareToFirst = false;
                }

            } else {
                lines.add(getDriverRowFinal(driver, prevDriver));
                prevDriver = driver;
            }
        }
        return lines;
    }

    private String getDriverRowFinal(Driver driver, Driver comparingDriver){
        if (driver.getLaps().size() < 1) {
            return ScoreboardUtils.getDriverLineRace(driver.getTPlayer().getName(), driver.getPosition(), false);
        }
        long timeDiff;

        if (driver.getTPlayer().getPlayer() == null) {
            return ScoreboardUtils.getDriverLineRaceOffline(driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), false);
        }
        Location playerLoc = driver.getTPlayer().getPlayer().getLocation();

        var inPitRegions = heat.getEvent().getTrack().getRegions(TrackRegion.RegionType.INPIT);
        for (TrackRegion trackRegion : inPitRegions) {
            if (trackRegion.contains(playerLoc)){
                return ScoreboardUtils.getDriverLineRaceInPit(driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), false);
            }
        }

        if (comparingDriver == null) {
            return ScoreboardUtils.getDriverLineRaceLaps(driver.getLaps().size(), driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), false);
        }
        if (driver.isFinished()) {
            timeDiff = Duration.between(comparingDriver.getEndTime(), driver.getEndTime()).toMillis();
            return ScoreboardUtils.getDriverLineRaceGap(timeDiff, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), false);
        }

        Instant timeStamp = driver.getTimeStamp(driver.getLaps().size(), driver.getCurrentLap().getLatestCheckpoint());
        Instant fasterTimeStamp = comparingDriver.getTimeStamp(driver.getLaps().size(), driver.getCurrentLap().getLatestCheckpoint());
        timeDiff = Duration.between(fasterTimeStamp, timeStamp).toMillis();
        if (timeDiff < 0) {
            return ScoreboardUtils.getDriverLineNegativeRaceGap(timeDiff*-1, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), false);
        }
        return ScoreboardUtils.getDriverLineRaceGap(timeDiff, driver.getTPlayer().getName(), driver.getPits(), driver.getPosition(), false);
    }

    private String getDriverRowQualy(Driver driver, Driver comparingDriver) {
        if (driver.getBestLap().isEmpty()) {
            return ScoreboardUtils.getDriverLine(driver.getTPlayer().getName(), driver.getPosition(), false);
        }

        if (comparingDriver == null) {
            return ScoreboardUtils.getDriverLineQualyTime(driver.getBestLap().get().getLapTime(), driver.getTPlayer().getName(), driver.getPosition(), false);
        }

        long timeDiff = driver.getBestLap().get().getLapTime() - comparingDriver.getBestLap().get().getLapTime();
        if (timeDiff < 0) {
            return ScoreboardUtils.getDriverLineNegativeQualyGap(timeDiff*-1, driver.getTPlayer().getName(), driver.getPosition(), false);
        }
        return ScoreboardUtils.getDriverLineQualyGap(timeDiff, driver.getTPlayer().getName(), driver.getPosition(), false);
    }
}
