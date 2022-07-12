package me.makkuusen.timing.system.heat;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.FinalDriver;

@Getter
@Setter
public class FinalHeat extends Heat {

    private int totalLaps;
    private int totalPits;

    public FinalHeat(Event event, String name, int totalLaps, int totalPits){
        super(event, name);
        this.totalLaps = totalLaps;
        this.totalPits = totalPits;
        setScoreboard(new FinalScoreboard(this));
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
        EventAnnouncements.sendFinishSound(driver);
        EventAnnouncements.sendFinishTitle(driver);
        EventAnnouncements.broadcastFinish(this, driver, driver.getFinishTime());
    }
}
