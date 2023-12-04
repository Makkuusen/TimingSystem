package me.makkuusen.timing.system.round;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.EventDatabase;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.participant.Driver;

import java.util.List;

public class QualificationRound extends Round {

    public QualificationRound(DbRow data) {
        super(data);
    }

    public String getName() {
        return "R" + getRoundIndex() + "-Qualy";
    }

    public String getDisplayName() {
        return "Round " + getRoundIndex() + " - Qualification";
    }

    public void broadcastResults(Event event, List<Driver> drivers) {
        EventAnnouncements.broadcastQualificationResults(event, drivers);
    }

    public void createHeat(int heatNumber) {
        var maybeHeat = EventDatabase.heatNew(this, heatNumber);
        if (maybeHeat.isPresent()) {
            var nextHeat = maybeHeat.get();
            nextHeat.setTimeLimit(TimingSystem.configuration.getTimeLimit());
            nextHeat.setStartDelayInTicks(TimingSystem.configuration.getQualyStartDelayInMS());
        }
    }
}
