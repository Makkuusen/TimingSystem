package me.makkuusen.timing.system.event;

import lombok.Getter;
import me.makkuusen.timing.system.heat.FinalHeat;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.heat.QualifyHeat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class EventSchedule {

    private final List<QualifyHeat> qualifyHeatList = new ArrayList<>();
    private final List<FinalHeat> finalHeatList = new ArrayList<>();

    public EventSchedule() {

    }

    public void createQuickSchedule(List<QualifyHeat> qualifyHeats, List<FinalHeat> finalHeats) {
        qualifyHeatList.addAll(qualifyHeats);
        finalHeatList.addAll(finalHeats);
    }

    public List<Heat> getHeats() {
        List<Heat> heats = new ArrayList<>();
        heats.addAll(getQualifyHeatList());
        heats.addAll(getFinalHeatList());
        return heats;
    }

    public boolean addHeat(Heat heat) {
        if (heat instanceof QualifyHeat qualifyHeat && !qualifyHeatList.contains(qualifyHeat)) {
            qualifyHeatList.add(qualifyHeat);
            return true;
        } else if (heat instanceof FinalHeat finalHeat && !finalHeatList.contains(finalHeat)) {
            finalHeatList.add(finalHeat);
        }

        return false;
    }

    public boolean removeHeat(Heat heat) {
        if (heat.getHeatState() != HeatState.FINISHED) {
            if (heat instanceof QualifyHeat qualifyHeat && qualifyHeatList.contains(qualifyHeat)) {
                qualifyHeatList.remove(qualifyHeat);
                for (Heat qheat : qualifyHeatList) {
                    if (heat.getHeatNumber() < qheat.getHeatNumber()){
                        qheat.setHeatNumber(qheat.getHeatNumber() - 1);
                    }
                }
                return true;
            } else if (heat instanceof FinalHeat finalHeat && finalHeatList.contains(finalHeat)) {
                finalHeatList.remove(finalHeat);
                for (Heat fheat : finalHeatList) {
                    if (heat.getHeatNumber() < fheat.getHeatNumber()){
                        fheat.setHeatNumber(fheat.getHeatNumber() - 1);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public List<String> listHeats() {
        List<String> message = new ArrayList<>();

        if (!getQualifyHeatList().isEmpty()) {
            message.add("§2Qualification Heats:");
            getQualifyHeatList().stream().forEach(heat -> message.add("§a - " + heat.getName()));
        }

        if (!getFinalHeatList().isEmpty()) {
            message.add("§2Final Heats:");
            getFinalHeatList().stream().forEach(heat -> message.add("§a - " + heat.getName()));
        }
        return message;
    }

    public List<String> getRawHeats() {
        List<String> heats = new ArrayList<>();
        getQualifyHeatList().stream().forEach(heat -> heats.add(heat.getName()));
        getFinalHeatList().stream().forEach(heat -> heats.add(heat.getName()));
        return heats;
    }

    public Optional<Heat> getHeat(String heatName) {
        var heats = getHeats();
        return heats.stream().filter(heat -> heatName.equalsIgnoreCase(heat.getName())).findFirst();
    }
}
