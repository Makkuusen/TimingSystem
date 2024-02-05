package me.makkuusen.timing.system.api.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class HeatResult {
    private String name;
    private Integer totalLaps;
    private Instant dateStarted;
    private Instant dateEnded;
    List<DriverResult> driverResultList;

}
