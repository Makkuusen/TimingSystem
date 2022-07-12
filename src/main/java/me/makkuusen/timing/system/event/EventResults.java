package me.makkuusen.timing.system.event;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.participant.Driver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventResults {


    List<Driver> qualyResults = new ArrayList<>();

    public EventResults(){

    }

    public void reportQualyResults(List<Driver> heatPos){
        qualyResults.addAll(heatPos);
    }

    public List<Driver> generateFinalPositions(){
        Collections.sort(qualyResults);
        ApiUtilities.msgConsole("Generated Final Positions:");
        for(Driver d : qualyResults){
            ApiUtilities.msgConsole(d.getPosition() + ":" + d.getTPlayer().getName());
        }

        return qualyResults;
    }
}
