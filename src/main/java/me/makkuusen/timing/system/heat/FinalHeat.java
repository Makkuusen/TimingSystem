package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;


public class FinalHeat {

    public static boolean passLap(Driver driver){
        if (driver.getHeat().getHeatState() != HeatState.RACING) {
            return false;
        }

        if (driver.getHeat().getTotalLaps() <= driver.getLaps().size() && driver.getHeat().getTotalPits() <= driver.getPits())
        {
            finishDriver(driver);
            if (driver.getHeat().noDriversRunning()){
                driver.getHeat().finishHeat();
            }
            return true;
        }
        driver.passLap();
        return true;
    }

    private static void finishDriver(Driver driver) {
        driver.finish();
        driver.getHeat().updatePositions();
        EventAnnouncements.sendFinishSound(driver);
        EventAnnouncements.sendFinishTitle(driver);
        EventAnnouncements.broadcastFinish(driver.getHeat(), driver, driver.getFinishTime());
    }
}
