package me.makkuusen.timing.system.heat;

import co.aikar.idb.DbRow;
import co.aikar.taskchain.TaskChain;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;

import java.time.Duration;

@Setter
@Getter
public class QualifyHeat extends Heat {

    public QualifyHeat(DbRow data) {
        super(data);
    }

    @Override
    public void startHeat() {
        setHeatState(HeatState.RACING);
        updateScoreboard();
        setStartTime(TimingSystem.currentTime);
        TaskChain<?> chain = TimingSystem.newSharedChain("STARTING");
        for (Driver driver : getStartPositions()) {
            chain.sync(() -> {
                getGridManager().startPlayerFromGrid(driver.getTPlayer().getUniqueId());
                driver.setStartTime(TimingSystem.currentTime);
                EventDatabase.addPlayerToRunningHeat(driver);
                if (driver.getTPlayer().getPlayer() != null) {
                    driver.getTPlayer().getPlayer().playSound(driver.getTPlayer().getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1, 1);
                }
            });
            if (getStartDelay() != null && getStartDelay() > 0) {
                //Start delay in seconds times 20 ticks.
                chain.delay(getStartDelay() * 20);
            }
        }
        chain.execute();
    }

    @Override
    public boolean passLap(Driver driver) {
        if (getHeatState() != HeatState.RACING) {
            return false;
        }
        if (timeIsOver(driver)) {
            updatePositions();
            driver.finish();
            EventAnnouncements.sendFinishSound(driver);
            if (allDriversFinished()) {
                finishHeat();
            }
            return true;
        }

        driver.passLap();
        return true;
    }

    private boolean timeIsOver(Driver driver) {
        return Duration.between(driver.getStartTime(), TimingSystem.currentTime).toMillis() > getTimeLimit();
    }
}
