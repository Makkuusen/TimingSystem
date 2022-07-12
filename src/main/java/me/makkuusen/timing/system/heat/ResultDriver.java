package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.LapResult;

import java.util.List;

public class ResultDriver {

    private String eventId;
    private String trackId;
    private String heatName;
    String uuid;
    long startTime;
    long endTime;
    int position;
    boolean finished;
    List<LapResult> laps;
}
