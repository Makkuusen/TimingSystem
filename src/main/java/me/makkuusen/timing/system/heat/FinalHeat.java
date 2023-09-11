package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.api.events.PlayerFinishHeatEvent;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.Bukkit;


public class FinalHeat {

    public static boolean passLap(Driver driver) {
        if (driver.getHeat().getHeatState() != HeatState.RACING) {
            return false;
        }

        if (driver.getHeat().getTotalLaps() <= driver.getLaps().size() && driver.getHeat().getTotalPits() <= driver.getPits()) {
            finishDriver(driver);
            if (driver.getHeat().noDriversRunning()) {
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

        PlayerFinishHeatEvent event = new PlayerFinishHeatEvent(driver.getHeat(), driver);
        Bukkit.getServer().getPluginManager().callEvent(event);
    }
}
