package me.makkuusen.timing.system.api.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RoundResult {

    private String name;
    private String type; // [FINAL] or [QUALIFICATION]
    private Integer index;
    private List<HeatResult> heatResults;
}
