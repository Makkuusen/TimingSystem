package me.makkuusen.timing.system.event;

import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.heat.FinalHeat;
import me.makkuusen.timing.system.participant.Driver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Getter
public class EventResults {

    List<Driver> qualyResults = new ArrayList<>();
    List<Driver> finalResults = new ArrayList<>();

    public EventResults() {

    }

    public void reportQualyResults(List<Driver> heatPos) {
        qualyResults.addAll(heatPos);
        Collections.sort(qualyResults);
    }

    public void reportFinalResults(List<FinalHeat> finalHeats) {
        for (FinalHeat finalHeat : finalHeats) {
            List<Driver> newList = new ArrayList<>();
            newList.addAll(finalHeat.getDrivers().values());
            Collections.sort(newList, Comparator.comparingInt(Driver::getPosition));
            finalResults.addAll(newList);
        }
    }

    public List<Driver> generateFinalStartingPositions() {
        Collections.sort(qualyResults);
        ApiUtilities.msgConsole("Generated Final Positions:");
        for (Driver d : qualyResults) {
            ApiUtilities.msgConsole(d.getPosition() + ":" + d.getTPlayer().getName());
        }

        return qualyResults;
    }
}
