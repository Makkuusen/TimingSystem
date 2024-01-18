package me.makkuusen.timing.system.api.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
@Getter
@AllArgsConstructor
public class DriverResult {

    int position;
    int startPosition;
    String name;
    String uuid;

    List<LapResult> laps;

}
