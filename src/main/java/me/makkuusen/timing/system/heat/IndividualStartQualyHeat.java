package me.makkuusen.timing.system.heat;

import co.aikar.idb.DbRow;
import co.aikar.taskchain.TaskChain;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.DriverState;
import me.makkuusen.timing.system.round.Round;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IndividualStartQualyHeat extends Heat {

    public IndividualStartQualyHeat(DbRow data, Round round) {
        super(data, round);
    }


    public boolean passLap(Driver driver) {
        if (driver.getHeat().getHeatState() != HeatState.RACING) {
            return false;
        }

        driver.finish();
        driver.getHeat().updatePositions();
        EventAnnouncements.sendFinishSound(driver);
        EventAnnouncements.sendFinishTitleQualy(driver);
        EventAnnouncements.broadcastFinishQualy(driver.getHeat(), driver);

        return true;
    }

    public boolean initHeat(){
        if (getEvent().getEventSchedule().getCurrentRound() != getRound().getRoundIndex()) {
            return false;
        }
        if (getHeatState() != HeatState.SETUP) {
            return false;
        }
        setLivePositions(new ArrayList<>());
        setHeatState(HeatState.RACING);
        setStartTime(TimingSystem.currentTime);
        setScoreboard(new SpectatorScoreboard(this));
        updateScoreboard();
        return true;
    }

    public void startDriver(UUID uuid) {
        var driver = getDrivers().get(uuid);
        getLivePositions().add(driver);
        EventDatabase.addPlayerToRunningHeat(driver);
        Location grid = getEvent().getTrack().getSpawnLocation();
        if (grid != null) {
            getGridManager().teleportPlayerToGrid(driver.getTPlayer().getPlayer(), grid);
        }
        driver.setState(DriverState.LOADED);
        startWithCountdown(driver);
    }

    private void startWithCountdown(Driver driver){
        TaskChain<?> chain = TimingSystem.newChain();

        for (int i = 5; i > 0; i--) {
            int finalI = i;
            chain.sync(() -> {
                if (driver.getTPlayer().getPlayer() != null) {
                    var player = driver.getTPlayer().getPlayer();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1, 1);
                    Component mainTitle = Component.text(finalI, NamedTextColor.WHITE);
                    Title.Times times = Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(100));
                    Title title = Title.title(mainTitle, Component.empty(), times);
                    player.showTitle(title);
                }
            }).delay(20);
        }

        chain.execute((finished) -> {
            getGridManager().startPlayerFromGrid(driver.getTPlayer().getUniqueId());
            driver.setStartTime(TimingSystem.currentTime);
            driver.setState(DriverState.STARTING);
            if (driver.getTPlayer().getPlayer() != null) {
                driver.getTPlayer().getPlayer().playSound(driver.getTPlayer().getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1, 1);
            }
        });
    }

    @Override
    public boolean resetHeat() {
        if (getHeatState() == HeatState.FINISHED) {
            return false;
        }
        setHeatState(HeatState.SETUP);
        setStartTime(null);
        setEndTime(null);
        setFastestLapUUID(null);
        setLivePositions(new ArrayList<>());
        getGridManager().clearArmorstands();
        List<Driver> drivers = new ArrayList<>();
        drivers.addAll(getDrivers().values());
        drivers.stream().forEach(driver -> {
            driver.reset();
            EventDatabase.removePlayerFromRunningHeat(driver.getTPlayer().getUniqueId());
            removeDriver(driver);
        });
        ApiUtilities.msgConsole("CLEARED SCOREBOARDS");
        return true;
    }
}
