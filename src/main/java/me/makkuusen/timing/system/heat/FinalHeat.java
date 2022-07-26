package me.makkuusen.timing.system.heat;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.FinalDriver;

@Getter
@Setter
public class FinalHeat extends Heat {

    private int totalLaps;
    private int totalPits;

    public FinalHeat(DbRow data) {
        super(data);
        totalLaps = data.getInt("totalLaps");
        totalPits = data.getInt("totalPitstops");
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
        if (totalLaps <= fDriver.getLaps().size() && totalPits <= fDriver.getPits())
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

    public void setTotalLaps(int totalLaps){
        this.totalLaps = totalLaps;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `totalLaps` = " + totalLaps + " WHERE `id` = " + getId() + ";");
    }

    public void setTotalPits(int totalPits) {
        this.totalPits = totalPits;
        DB.executeUpdateAsync("UPDATE `ts_heats` SET `totalPitstops` = " + totalPits + " WHERE `id` = " + getId() + ";");
    }
}
