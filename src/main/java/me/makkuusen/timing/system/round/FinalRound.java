package me.makkuusen.timing.system.round;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.TrackRegion;

import java.util.List;

public class FinalRound extends Round {

    public FinalRound(DbRow data) {
        super(data);
    }


    public String getName(){
        return "R" + getRoundIndex() + "-Final";
    }

    public String getDisplayName(){
        return "Round " + getRoundIndex() + " - Final";
    }

    public void broadcastResults(Event event, List<Driver> drivers){
        EventAnnouncements.broadcastFinalResults(event, drivers);
    }

    public void createHeat(int heatNumber){
        int pits = 0;
        var maybePit = getEvent().getTrack().getRegion(TrackRegion.RegionType.PIT);
        if (maybePit.isPresent() && maybePit.get().isDefined()) {
            pits = TimingSystem.configuration.getPits();
        }
        var maybeHeat = EventDatabase.heatNew(this, heatNumber);
        if (maybeHeat.isPresent()) {
            var nextHeat = maybeHeat.get();
            nextHeat.setTotalLaps(TimingSystem.configuration.getLaps());
            nextHeat.setTotalPits(pits);
        }
    }
}
