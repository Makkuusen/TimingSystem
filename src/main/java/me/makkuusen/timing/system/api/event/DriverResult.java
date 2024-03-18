package me.makkuusen.timing.system.api.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
@Getter
@AllArgsConstructor
public class DriverResult {

    Integer position;
    Integer startPosition;
    String name;
    String uuid;
    Long finishTimeInMs;

    List<LapResult> laps;

}
