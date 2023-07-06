package me.makkuusen.timing.system.event;

import lombok.Getter;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.round.QualificationRound;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
public class EventResults {


    public static List<Driver> generateHeatResults(Heat heat) {
        List<Driver> newList = new ArrayList<>(heat.getDrivers().values());
        newList.sort(Comparator.comparingInt(Driver::getPosition));
        return newList;
    }



    public static List<Driver> generateRoundResults(List<Heat> heats) {
        List<Driver> results = new ArrayList<>();
        for (Heat heat : heats) {
            List<Driver> newList = new ArrayList<>(heat.getDrivers().values());
            newList.sort(Comparator.comparingInt(Driver::getPosition));
            results.addAll(newList);
        }
        if (results.size() > 0) {
            if (results.get(0).getHeat().getRound() instanceof QualificationRound) {
                results.sort(Driver::compareTo);
            }
        }

        return results;
    }
}
