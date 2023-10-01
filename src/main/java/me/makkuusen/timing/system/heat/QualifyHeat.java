package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.theme.Theme;
import net.kyori.adventure.text.Component;

import java.time.Duration;

public class QualifyHeat {


    public static boolean passQualyLap(Driver driver) {
        if (driver.getHeat().getHeatState() != HeatState.RACING) {
            return false;
        }
        if (timeIsOver(driver)) {
            driver.finish();
            driver.getHeat().updatePositions();
            EventAnnouncements.sendFinishSound(driver);
            EventAnnouncements.sendFinishTitleQualification(driver);
            EventAnnouncements.broadcastFinishQualification(driver.getHeat(), driver);
            if (driver.getHeat().noDriversRunning()) {
                driver.getHeat().finishHeat();
            }
            return true;
        }

        driver.passLap();
        return true;
    }

    public static Component getBestLapDelta(Theme theme, Lap finishedLap, Lap personalBest) {
        if (finishedLap != null && personalBest != null) {
            if (personalBest.getLapTime() < finishedLap.getLapTime()) {
                return Component.text(" +" + ApiUtilities.formatAsPersonalGap(finishedLap.getLapTime() - personalBest.getLapTime())).color(theme.getError());
            } else if (personalBest.getLapTime() == finishedLap.getLapTime()){
                return Component.text(" -" + ApiUtilities.formatAsPersonalGap(personalBest.getLapTime() - finishedLap.getLapTime())).color(theme.getWarning());
            }else {
                return Component.text(" -" + ApiUtilities.formatAsPersonalGap(personalBest.getLapTime() - finishedLap.getLapTime())).color(theme.getSuccess());
            }
        }

        return Component.empty();
    }

    public static String getBestLapCheckpointDelta(Driver driver, int latestCheckpoint) {
        if (latestCheckpoint > 0) {
            if (driver.getBestLap().isPresent() && driver.getBestLap().get().getCheckpointTime(latestCheckpoint) != null) {
                var bestCheckpoint = Duration.between(driver.getBestLap().get().getLapStart(), driver.getBestLap().get().getCheckpointTime(latestCheckpoint)).toMillis();
                var currentCheckpoint = Duration.between(driver.getCurrentLap().getLapStart(), driver.getCurrentLap().getCheckpointTime(latestCheckpoint)).toMillis();
                if (ApiUtilities.getRoundedToTick(bestCheckpoint) < ApiUtilities.getRoundedToTick(currentCheckpoint)) {
                    return " " + "&e+" + ApiUtilities.formatAsPersonalGap(currentCheckpoint - bestCheckpoint);
                } else if (ApiUtilities.getRoundedToTick(bestCheckpoint) == ApiUtilities.getRoundedToTick(currentCheckpoint)) {
                    return " " + "&w-" + ApiUtilities.formatAsPersonalGap(currentCheckpoint - bestCheckpoint);
                }else {
                    return " " + "&s-" + ApiUtilities.formatAsPersonalGap(bestCheckpoint - currentCheckpoint);
                }
            }
        }

        return "";
    }

    private static boolean timeIsOver(Driver driver) {
        return Duration.between(driver.getStartTime(), TimingSystem.currentTime).toMillis() > driver.getHeat().getTimeLimit();
    }
}
