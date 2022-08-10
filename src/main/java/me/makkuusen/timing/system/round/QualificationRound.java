package me.makkuusen.timing.system.round;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.event.EventResults;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.TrackRegion;

import java.util.List;

public class QualificationRound extends Round {

    public QualificationRound(DbRow data) {
        super(data);
    }

    public boolean finish(Event event, Round nextRound) {
        if (event.getState() != Event.EventState.RUNNING) {
            return false;
        }
        if (getHeats().stream().anyMatch(Heat::isActive)){
            return false;
        }

        List<Driver> drivers = EventResults.generateRoundResults(getHeats());
        if (getHeats().size() > 1) {
            EventAnnouncements.broadcastQualificationResults(event, drivers);
        }


        if (nextRound.getHeats().isEmpty()) {
            int maxDrivers = event.getTrack().getGridLocations().size();
            int finalHeats = drivers.size() / maxDrivers;
            if (drivers.size() % maxDrivers != 0) {
                finalHeats++;
            }
            for (int i = 0; i < finalHeats; i++) {
                int pits = 0;
                var maybePit = event.getTrack().getRegion(TrackRegion.RegionType.PIT);
                if (maybePit.isPresent() && maybePit.get().isDefined()) {
                    pits = TimingSystem.configuration.getPits();
                }
                EventDatabase.finalHeatNew(event, i + 1, TimingSystem.configuration.getLaps(), pits);
            }
        }

        int i = 0;
        for (Heat nextHeat : nextRound.getHeats()) {
            int startPos = 1;
            for (; i < drivers.size(); i++) {
                if (startPos > nextHeat.getMaxDrivers()){
                    break;
                }
                // Create next type of driver.
                //EventDatabase.finalDriverNew(drivers.get(i).getTPlayer().getUniqueId(), finalHeat, startPos++);
            }
        }
        event.eventSchedule.nextRound();
        return true;
    }
}
