package me.makkuusen.timing.system;

import java.util.HashMap;
import java.util.UUID;

public class Event {

    public static TimingSystem plugin;

    public Event(){

        HashMap<UUID, RaceSpectator> raceSpectators = new HashMap<>();
        HashMap<UUID, RaceDriver> raceDrivers = new HashMap<>();
        Track track;


    }
}
