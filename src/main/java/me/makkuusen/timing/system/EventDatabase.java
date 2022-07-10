package me.makkuusen.timing.system;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class EventDatabase {

    public static TimingSystem plugin;
    private static final List<Event> events = new ArrayList<>();
}
