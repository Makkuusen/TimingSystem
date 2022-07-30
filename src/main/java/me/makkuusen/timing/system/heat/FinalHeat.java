package me.makkuusen.timing.system.heat;

import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.FinalDriver;

@Getter
@Setter
public class FinalHeat extends Heat {



    public FinalHeat(DbRow data) {
        super(data);

    }

    public String getName(){
        return "F" + getHeatNumber();
    }

    @Override
    public boolean passLap(Driver driver){
        if (getHeatState() != HeatState.RACING) {
            return false;
        }
        if (!(driver instanceof FinalDriver))
        {
            return false;
        }
        FinalDriver fDriver = (FinalDriver) driver;
        if (getTotalLaps() <= fDriver.getLaps().size() && getTotalPits() <= fDriver.getPits())
        {
            finishDriver(fDriver);
            if (allDriversFinished()){
                finishHeat();
            }
            return true;
        }
        driver.passLap();
        return true;
    }

    private void finishDriver(FinalDriver driver) {
        driver.finish();
        updatePositions();
        EventAnnouncements.sendFinishSound(driver);
        EventAnnouncements.sendFinishTitle(driver);
        EventAnnouncements.broadcastFinish(this, driver, driver.getFinishTime());
    }
}
