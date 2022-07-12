package me.makkuusen.timing.system.event;

import lombok.Getter;
import me.makkuusen.timing.system.heat.FinalHeat;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.QualyHeat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class EventSchedule {

    private final List<Heat> practiceHeatList = new ArrayList<>();
    private final List<QualyHeat> qualyHeatList = new ArrayList<>();
    private final List<FinalHeat> finalHeatList = new ArrayList<>();

    public EventSchedule() {

    }

    public void createQuickSchedule(List<QualyHeat> qualyHeats, List<FinalHeat> finalHeats) {
        qualyHeatList.addAll(qualyHeats);
        finalHeatList.addAll(finalHeats);
    }

    public List<Heat> getHeats(){
        List<Heat> heats = new ArrayList<>();
        heats.addAll(getPracticeHeatList());
        heats.addAll(getQualyHeatList());
        heats.addAll(getFinalHeatList());
        return heats;
    }

    public List<String> listHeats() {
        List<String> message = new ArrayList<>();

        if (!getPracticeHeatList().isEmpty()) {
            message.add("§2Practice Heats:");
            getPracticeHeatList().stream().forEach(heat -> message.add("§a - " + heat.getName()));
        }

        if (!getQualyHeatList().isEmpty()) {
            message.add("§2Qualification Heats:");
            getQualyHeatList().stream().forEach(heat -> message.add("§a - " + heat.getName()));
        }

        if (!getFinalHeatList().isEmpty()) {
            message.add("§2Final Heats:");
            getFinalHeatList().stream().forEach(heat -> message.add("§a - " + heat.getName()));
        }
        return message;
    }

    public List<String> getRawHeats(){
        List<String> heats = new ArrayList<>();
        getPracticeHeatList().stream().forEach(heat -> heats.add(heat.getName()));
        getQualyHeatList().stream().forEach(heat -> heats.add(heat.getName()));
        getFinalHeatList().stream().forEach(heat -> heats.add(heat.getName()));
        return heats;
    }

    public Optional<Heat> getHeat(String heatName){
        var heats = getHeats();
        return heats.stream().filter(heat -> heatName.equalsIgnoreCase(heat.getName())).findFirst();
    }
}
