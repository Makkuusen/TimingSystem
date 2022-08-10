package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;

import java.time.Duration;

public class QualifyHeat {


    public static boolean passQualyLap(Driver driver) {
        if (driver.getHeat().getHeatState() != HeatState.RACING) {
            return false;
        }
        if (timeIsOver(driver)) {
            driver.getHeat().updatePositions();
            driver.finish();
            EventAnnouncements.sendFinishSound(driver);
            EventAnnouncements.sendFinishTitleQualy(driver);
            EventAnnouncements.broadcastFinishQualy(driver.getHeat(), driver);
            if (driver.getHeat().noDriversRunning()) {
                driver.getHeat().finishHeat();
            }
            return true;
        }

        driver.passLap();
        return true;
    }

    private static boolean timeIsOver(Driver driver) {
        return Duration.between(driver.getStartTime(), TimingSystem.currentTime).toMillis() > driver.getHeat().getTimeLimit();
    }
}
