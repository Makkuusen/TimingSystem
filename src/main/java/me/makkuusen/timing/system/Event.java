package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.UUID;

@Getter
@Setter
public class Event {

    public static TimingSystem plugin;
    HashMap<UUID, RaceSpectator> raceSpectators = new HashMap<>();
    HashMap<UUID, RaceDriver> raceDrivers = new HashMap<>();
    Track track;

    public Event(){

    }
}
