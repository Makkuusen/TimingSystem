package me.makkuusen.timing.system.event;

import lombok.Getter;
import me.makkuusen.timing.system.heat.FinalHeat;
import me.makkuusen.timing.system.heat.QualifyHeat;
import me.makkuusen.timing.system.participant.Driver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Getter
public class EventResults {


    public static List<Driver> generateFinalHeatResults(FinalHeat heat) {
        List<Driver> newList = new ArrayList<>();
        newList.addAll(heat.getDrivers().values());
        Collections.sort(newList, Comparator.comparingInt(Driver::getPosition));
        return newList;
    }

    public static List<Driver> generateQualyHeatResults(QualifyHeat heat) {
        List<Driver> newList = new ArrayList<>();
        newList.addAll(heat.getDrivers().values());
        Collections.sort(newList);
        return newList;
    }

    public static List<Driver> generateFinalResults(List<FinalHeat> finalHeats) {
        List<Driver> finalResults = new ArrayList<>();
        for (FinalHeat finalHeat : finalHeats) {
            List<Driver> newList = new ArrayList<>();
            newList.addAll(finalHeat.getDrivers().values());
            Collections.sort(newList, Comparator.comparingInt(Driver::getPosition));
            finalResults.addAll(newList);
        }
        return finalResults;
    }

    public static List<Driver> generateQualificationResults(List<QualifyHeat> qualyHeats) {
        List<Driver> drivers = new ArrayList<>();
        for (QualifyHeat h : qualyHeats) {
            drivers.addAll(h.getDrivers().values());
        }
        Collections.sort(drivers);
        return drivers;
    }
}
