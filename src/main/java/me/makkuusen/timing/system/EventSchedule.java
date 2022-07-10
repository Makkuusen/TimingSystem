package me.makkuusen.timing.system;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class EventSchedule {

    private List<Heat> practiceHeatList = new ArrayList<>();
    private List<Heat> qualyHeatList = new ArrayList<>();
    private List<FinalHeat> finalHeatList = new ArrayList<>();

    public EventSchedule() {

    }

    public void createQuickSchedule(List<FinalHeat> finalHeat){
        finalHeatList.addAll(finalHeat);
    }

    public List<String> listHeats(){
        List<String> message = new ArrayList<>();
        if (!getPracticeHeatList().isEmpty()){
            message.add("§2Practice Heats:");
            getPracticeHeatList().stream().forEach(heat -> message.add(heat.getName()));
        }

        if (!getQualyHeatList().isEmpty()){
            message.add("§2Qualification Heats:");
            getQualyHeatList().stream().forEach(heat -> message.add(heat.getName()));
        }

        if (!getFinalHeatList().isEmpty()){
            message.add("§2Final Heats:");
            getFinalHeatList().stream().forEach(heat -> message.add(heat.getName()));
        }
        return message;
    }
}
