package me.makkuusen.timing.system.api.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LapResult {
    Long timeInMs;
    boolean pitstop;
    boolean isFastest;
}
